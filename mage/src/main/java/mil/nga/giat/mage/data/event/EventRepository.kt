package mil.nga.giat.mage.data.event

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.annotation.WorkerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mil.nga.geopackage.factory.GeoPackageFactory
import mil.nga.giat.mage.data.feed.FeedDao
import mil.nga.giat.mage.data.user.UserRepository
import mil.nga.giat.mage.glide.GlideApp
import mil.nga.giat.mage.glide.model.Avatar
import mil.nga.giat.mage.map.preference.MapLayerPreferences
import mil.nga.giat.mage.network.Resource
import mil.nga.giat.mage.network.api.FeedService
import mil.nga.giat.mage.network.api.LayerService
import mil.nga.giat.mage.network.api.ObservationService
import mil.nga.giat.mage.network.api.TeamService
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper
import mil.nga.giat.mage.sdk.datastore.user.*
import mil.nga.giat.mage.sdk.utils.ZipUtility
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
   @ApplicationContext private val context: Context,
   private val mapLayerPreferences: MapLayerPreferences,
   private val feedDao: FeedDao,
   private val feedService: FeedService,
   private val teamService: TeamService,
   private val layerService: LayerService,
   private val observationService: ObservationService,
   private val userRepository: UserRepository
) {
   suspend fun syncEvent(event: Event): Resource<out Event> {
      val resource = withContext(Dispatchers.IO) {
         try {
            syncTeams(event)
            syncObservationIcons(event)
            syncLayers(event)
            syncFeeds(event)

            Resource.success(event)
         } catch (e: Exception) {
            Log.e(LOG_NAME, "Error syncing event", e)
            Resource.error(e.localizedMessage ?: e.toString(), event)
         }
      }

      CoroutineScope(Dispatchers.IO).launch {
         userRepository.syncIcons(event)
      }

      return resource
   }

   private suspend fun syncTeams(event: Event): List<User> {
      val iconUsers = mutableListOf<User>()

      val response = teamService.getTeams(event.remoteId)
      if (response.isSuccessful) {
         val teams = response.body()!!
         Log.d(LOG_NAME, "Fetched " + teams.size + " teams")

         val userHelper = UserHelper.getInstance(context)
         userHelper.deleteUserTeams()


         val teamHelper = TeamHelper.getInstance(context)
         for (team in teams.keys) {
            val updatedTeam = teamHelper.createOrUpdate(team)
            val users = teams[updatedTeam]!!
            for (user in users) {
               user.fetchedDate = Date()
               var updatedUser = userHelper.createOrUpdate(user)
               if (updatedUser.avatarUrl != null) {
                  GlideApp.with(context)
                     .download(Avatar.forUser(updatedUser))
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
            teamHelper.create(TeamEvent(updatedTeam, event))
         }

         TeamHelper.getInstance(context).syncTeams(teams.keys)
      }

      return iconUsers
   }

   private suspend fun syncObservationIcons(event: Event) {
      val response = observationService.getObservationIcons(event.remoteId)
      if (response.isSuccessful) {
         val directory = File(context.filesDir.toString() + OBSERVATION_ICON_PATH)
         val destination = File(directory, "${event.remoteId}.zip")
         if (destination.parentFile?.exists() != true) {
            destination.parentFile?.mkdirs()
         }

         if (destination.exists()) {
            destination.delete()
         }

         if (!destination.exists()) {
            destination.createNewFile()
         }

         val zipDirectory = File(directory, event.remoteId)
         if (!zipDirectory.exists()) {
            zipDirectory.mkdirs()
         }

         response.body()!!.byteStream().use { input ->
            destination.outputStream().use {  output ->
               input.copyTo(output)
            }
         }

         ZipUtility.unzip(destination, zipDirectory)
         destination.delete()
      }
   }

   private suspend fun syncLayers(event: Event) {
      val response = layerService.getLayers(event.remoteId, "GeoPackage")
      if (response.isSuccessful) {
         val layers = response.body()!!

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
            layer.event = event
            layerHelper.create(layer)
         }
      }
   }

   @WorkerThread
   private suspend fun syncFeeds(event: Event) {
      val response = feedService.getFeeds(event.remoteId)
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

   companion object {
      private val LOG_NAME = EventRepository::class.java.name
      private const val MAX_AVATAR_DIMENSION = 1024

      @JvmStatic
      val OBSERVATION_ICON_PATH = "/icons/observations"
   }
}