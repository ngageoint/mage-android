package mil.nga.giat.mage.data.event

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.preference.PreferenceManager
import android.util.Log
import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mil.nga.geopackage.factory.GeoPackageFactory
import mil.nga.giat.mage.dagger.module.ApplicationContext
import mil.nga.giat.mage.data.feed.FeedDao
import mil.nga.giat.mage.glide.GlideApp
import mil.nga.giat.mage.glide.model.Avatar.Companion.forUser
import mil.nga.giat.mage.map.preference.MapLayerPreferences
import mil.nga.giat.mage.network.Resource
import mil.nga.giat.mage.network.api.FeedService
import mil.nga.giat.mage.sdk.R
import mil.nga.giat.mage.sdk.datastore.layer.Layer
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper
import mil.nga.giat.mage.sdk.datastore.user.*
import mil.nga.giat.mage.sdk.fetch.DownloadImageTask
import mil.nga.giat.mage.sdk.gson.deserializer.LayerDeserializer
import mil.nga.giat.mage.sdk.http.HttpClientManager
import mil.nga.giat.mage.sdk.http.resource.EventResource
import mil.nga.giat.mage.sdk.http.resource.LayerResource.LayerService
import mil.nga.giat.mage.sdk.http.resource.ObservationResource
import mil.nga.giat.mage.sdk.utils.ZipUtility
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
        @ApplicationContext private val context: Context,
        private val mapLayerPreferences: MapLayerPreferences,
        private val feedDao: FeedDao,
        private val feedService: FeedService
) {

    companion object {
        private val LOG_NAME = EventRepository::class.java.name
        private const val MAX_AVATAR_DIMENSION = 1024

        @JvmStatic
        val OBSERVATION_ICON_PATH = "/icons/observations"
    }

    private var iconFetch: DownloadImageTask? = null

    suspend fun syncEvent(event: Event): Resource<out Event> {
        return withContext(Dispatchers.IO) {
            try {
                syncTeams(event)
                syncIcons(event)
                syncLayers(event)
                syncFeeds(event)

                Resource.success(event)
            } catch (e: Exception) {
                Resource.error(e.localizedMessage, event)
            }
        }
    }

    @WorkerThread
    private fun syncTeams(event: Event) {
        val eventResource = EventResource(context)
        val teams = eventResource.getTeams(event.remoteId)
        Log.d(LOG_NAME, "Fetched " + teams.size + " teams")

        val userHelper = UserHelper.getInstance(context)
        userHelper.deleteUserTeams()

        val iconUsers = ArrayList<User>()

        val teamHelper = TeamHelper.getInstance(context)
        for (team in teams.keys) {
            val updatedTeam = teamHelper.createOrUpdate(team)
            val users = teams.get(updatedTeam)!!
            for (user in users) {
                user.fetchedDate = Date()
                var updatedUser = userHelper.createOrUpdate(user)
                if (updatedUser.avatarUrl != null) {
                    GlideApp.with(context)
                            .download(forUser(updatedUser))
                            .submit(MAX_AVATAR_DIMENSION, MAX_AVATAR_DIMENSION)
                }
                if (updatedUser.iconUrl != null) {
                    iconUsers.add(updatedUser)
                }
                if (userHelper.read(updatedUser.remoteId) == null) {
                    updatedUser = userHelper.createOrUpdate(updatedUser)
                }
                // populate the user/team join table
                userHelper.create(UserTeam(updatedUser, updatedTeam))
            }

            // populate the team/event join table
            val event = EventHelper.getInstance(context).read(event.id)
            teamHelper.create(TeamEvent(updatedTeam, event))
        }

        TeamHelper.getInstance(context).syncTeams(teams.keys)
        iconFetch = DownloadImageTask(context, iconUsers, DownloadImageTask.ImageType.ICON, true)
    }

    @WorkerThread
    private fun syncIcons(event: Event) {
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        val observationResource = ObservationResource(context)

        try {
            inputStream = observationResource.getObservationIcons(event.remoteId)
            val directory = File(context.filesDir.toString() + OBSERVATION_ICON_PATH)
            val zipFile = File(directory, "${event.remoteId}.zip")
            if (!zipFile.parentFile.exists()) {
                zipFile.parentFile.mkdirs()
            }
            if (zipFile.exists()) {
                zipFile.delete()
            }
            if (!zipFile.exists()) {
                zipFile.createNewFile()
            }
            val zipDirectory = File(directory, event.remoteId)
            if (!zipDirectory.exists()) {
                zipDirectory.mkdirs()
            }

            // copy stream to file
            outputStream = FileOutputStream(zipFile)
            inputStream.copyTo(outputStream)

            Log.d(LOG_NAME, "Unzipping " + zipFile.absolutePath + " to " + zipDirectory.absolutePath + ".")
            // unzip file
            ZipUtility.unzip(zipFile, zipDirectory)
            // delete the zip
            zipFile.delete()
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }

    @WorkerThread
    private fun syncLayers(event: Event) {
        var layers: Collection<Layer> = ArrayList()

        val baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue))
        val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(LayerDeserializer.getGsonBuilder(event)))
                .client(HttpClientManager.getInstance().httpClient())
                .build()

        val service = retrofit.create(LayerService::class.java)
        val response = service.getLayers(event.remoteId, "GeoPackage").execute()
        if (response.isSuccessful) {
            layers = response.body()!!
        } else {
            Log.e(LOG_NAME, "Bad request.")
            if (response.errorBody() != null) {
                Log.e(LOG_NAME, response.errorBody()!!.string())
            }
        }

        val layerHelper = LayerHelper.getInstance(context)
        layerHelper.deleteAll("GeoPackage")
        val manager = GeoPackageFactory.getManager(context)
        for (layer in layers) { // Check if geopackage has been downloaded as part of another event
            val relativePath = String.format("MAGE/geopackages/%s/%s", layer.remoteId, layer.fileName)
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), relativePath)
            if (file.exists() && manager.existsAtExternalFile(file)) {
                layer.isLoaded = true
                layer.relativePath = relativePath
            }
            layerHelper.create(layer)
        }
    }

    @WorkerThread
    private fun syncFeeds(event: Event) {
        val response = feedService.getFeeds(event.remoteId).execute()
        if (response.isSuccessful) {
            var enabledFeeds = mapLayerPreferences.getEnabledFeeds(event.remoteId).toMutableSet()
            val feeds = response.body()!!
            for (feed in feeds) {
                feed.eventRemoteId = event.remoteId
                val upserted = feedDao.upsert(feed)
                if (upserted && feed.itemsHaveSpatialDimension) {
                    enabledFeeds.add(feed.id)
                }
            }

            val feedIds = feeds.map { it.id }
            enabledFeeds = enabledFeeds.intersect(feedIds).toMutableSet()
            mapLayerPreferences.setEnabledFeeds(event.remoteId, enabledFeeds)
            feedDao.preserveFeeds(event.remoteId, feedIds)
        }
    }
}