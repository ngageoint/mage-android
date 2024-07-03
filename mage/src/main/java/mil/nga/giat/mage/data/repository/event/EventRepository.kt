package mil.nga.giat.mage.data.repository.event

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.annotation.WorkerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mil.nga.geopackage.GeoPackageFactory
import mil.nga.giat.mage.data.datasource.team.TeamLocalDataSource
import mil.nga.giat.mage.data.repository.user.UserRepository
import mil.nga.giat.mage.database.dao.feed.FeedDao
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.data.datasource.layer.LayerLocalDataSource
import mil.nga.giat.mage.data.datasource.permission.RoleLocalDataSource
import mil.nga.giat.mage.database.model.team.TeamEvent
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.database.model.event.Form
import mil.nga.giat.mage.database.model.user.UserTeam
import mil.nga.giat.mage.glide.GlideApp
import mil.nga.giat.mage.glide.model.Avatar
import mil.nga.giat.mage.map.preference.MapLayerPreferences
import mil.nga.giat.mage.network.Resource
import mil.nga.giat.mage.network.event.EventService
import mil.nga.giat.mage.network.feed.FeedService
import mil.nga.giat.mage.network.layer.LayerService
import mil.nga.giat.mage.network.observation.ObservationService
import mil.nga.giat.mage.network.role.RoleService
import mil.nga.giat.mage.network.team.TeamService
import mil.nga.giat.mage.sdk.utils.ZipUtility
import java.io.File
import java.util.Date
import javax.inject.Inject

class EventRepository @Inject constructor(
   @ApplicationContext private val context: Context,
   private val mapLayerPreferences: MapLayerPreferences,
   private val teamLocalDataSource: TeamLocalDataSource,
   private val feedDao: FeedDao,
   private val roleService: RoleService,
   private val feedService: FeedService,
   private val teamService: TeamService,
   private val layerService: LayerService,
   private val eventService: EventService,
   private val observationService: ObservationService,
   private val userRepository: UserRepository,
   private val roleLocalDataSource: RoleLocalDataSource,
   private val userLocalDataSource: UserLocalDataSource,
   private val layerLocalDataSource: LayerLocalDataSource,
   private val eventLocalDataSource: EventLocalDataSource
) {

   suspend fun getCurrentEvent(): Event? {
      return withContext(Dispatchers.IO) {
         eventLocalDataSource.currentEvent
      }
   }

   suspend fun getEvents(forceUpdate: Boolean): List<Event> {
      return withContext(Dispatchers.IO) {
         if (forceUpdate) {
            try {
               syncRoles()
               fetchEvents()
            } catch(e: Exception) {
               Log.e(LOG_NAME, "Error fetching events", e)
            }
         }

         eventLocalDataSource.readAll()
      }
   }

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

   private suspend fun syncRoles() {
      val response = roleService.getRoles()
      if (response.isSuccessful) {
         val roles = response.body() ?: emptyList()
         roles.forEach { roleLocalDataSource.createOrUpdate(it) }
      }
   }

   private suspend fun fetchEvents(): List<Event> {
      teamLocalDataSource.deleteTeamEvents()

      val response = eventService.getEvents()
      return if (response.isSuccessful) {
         val events = response.body() ?: emptyList()
         events.forEach { event ->
            try {
               eventLocalDataSource.createOrUpdate(event)
            } catch (e: Exception) {
               Log.e(LOG_NAME, "Error saving event to database", e)
            }
         }

         eventLocalDataSource.syncEvents(events)

         events
      } else emptyList()
   }

   private suspend fun syncTeams(event: Event): List<User> {
      val iconUsers = mutableListOf<User>()

      val response = teamService.getTeams(event.remoteId)
      if (response.isSuccessful) {
         val teams = response.body() ?: emptyMap()
         Log.d(LOG_NAME, "Fetched " + teams.size + " teams")

         userLocalDataSource.deleteUserTeams()

         teams.keys.forEach { team ->
            val updatedTeam = teamLocalDataSource.createOrUpdate(team)
            teams[updatedTeam]?.forEach { (user, roleId) ->
               try {
                  user.fetchedDate = Date()
                  user.role = roleLocalDataSource.read(roleId)
                  userLocalDataSource.read(user.remoteId)?.let { localUser ->
                     user.id = localUser.id
                  }

                  val updatedUser = userLocalDataSource.createOrUpdate(user)
                  if (updatedUser.avatarUrl != null) {
                     GlideApp.with(context)
                        .download(Avatar.forUser(updatedUser))
                        .submit(MAX_AVATAR_DIMENSION, MAX_AVATAR_DIMENSION)
                  }

                  if (updatedUser.iconUrl != null) {
                     iconUsers.add(updatedUser)
                  }

                  val teamUser = if (userLocalDataSource.read(updatedUser.remoteId) == null) {
                     userLocalDataSource.createOrUpdate(updatedUser)
                  } else updatedUser

                  // populate the user/team join table
                  userLocalDataSource.create(UserTeam(teamUser, updatedTeam))
               } catch (e: Exception) {
                  Log.e(LOG_NAME, "Error sync'ing user", e)
               }
            }

            // populate the team/event join table
            teamLocalDataSource.create(
               TeamEvent(
                  updatedTeam,
                  event
               )
            )
         }

         teamLocalDataSource.syncTeams(teams.keys)
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
         val layers = response.body() ?: emptyList()

         layerLocalDataSource.deleteAll("GeoPackage")
         val manager = GeoPackageFactory.getManager(context)
         layers.forEach { layer -> // Check if geopackage has been downloaded as part of another event
            val relativePath = String.format("MAGE/geopackages/%s/%s", layer.remoteId, layer.fileName)
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), relativePath)
            if (file.exists() && manager.existsAtExternalFile(file)) {
               layer.isLoaded = true
               layer.relativePath = relativePath
            }
            layer.event = event
            layerLocalDataSource.create(layer)
         }
      }
   }

   @WorkerThread
   private suspend fun syncFeeds(event: Event) {
      val response = feedService.getFeeds(event.remoteId)
      if (response.isSuccessful) {
         var enabledFeeds = mapLayerPreferences.getEnabledFeeds(event.id).toMutableSet()
         val feeds = response.body() ?: emptyList()
         feeds.forEach { feed ->
            feed.eventRemoteId = event.remoteId
            val upserted = feedDao.upsert(feed)
            if (upserted && feed.itemsHaveSpatialDimension) {
               enabledFeeds.add(feed.id)
            }
         }

         val feedIds = feeds.map { it.id }
         enabledFeeds = enabledFeeds.intersect(feedIds.toSet()).toMutableSet()
         mapLayerPreferences.setEnabledFeeds(event.id, enabledFeeds)
         feedDao.preserveFeeds(event.remoteId, feedIds)
      }
   }

   fun getForm(formId: Long): Form? = eventLocalDataSource.getForm(formId)

   companion object {
      private val LOG_NAME = EventRepository::class.java.name
      private const val MAX_AVATAR_DIMENSION = 1024

      @JvmStatic
      val OBSERVATION_ICON_PATH = "/icons/observations"
   }
}