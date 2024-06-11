package mil.nga.giat.mage.data.repository.location

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.repository.user.UserRepository
import mil.nga.giat.mage.di.TokenProvider
import mil.nga.giat.mage.filter.DateTimeFilter
import mil.nga.giat.mage.filter.Filter
import mil.nga.giat.mage.location.LocationAccess
import mil.nga.giat.mage.network.location.LocationService
import mil.nga.giat.mage.sdk.Temporal
import mil.nga.giat.mage.database.model.location.Location
import mil.nga.giat.mage.data.datasource.location.LocationLocalDataSource
import mil.nga.giat.mage.database.model.location.LocationProperty
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.sdk.event.ILocationEventListener
import mil.nga.giat.mage.sdk.exceptions.LocationException
import mil.nga.giat.mage.sdk.exceptions.UserException
import mil.nga.sf.Point
import java.sql.SQLException
import java.util.*
import javax.inject.Inject

class LocationRepository @Inject constructor(
   @ApplicationContext private val context: Context,
   private val preferences: SharedPreferences,
   private val locationAccess: LocationAccess,
   private val locationService: LocationService,
   private val userRepository: UserRepository,
   private val tokenProvider: TokenProvider,
   private val userLocalDataSource: UserLocalDataSource,
   private val eventLocalDataSource: EventLocalDataSource,
   private val locationLocalDataSource: LocationLocalDataSource
) {
   private var batteryStatus: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

   private var refreshTime: Long = 0
   private var refreshJob: Job? = null
   private var oldestLocation: Location? = null

   fun observeLocation(locationId: Long) = locationLocalDataSource.observeLocation(locationId)

   suspend fun saveLocation(gpsLocation: android.location.Location) = withContext(Dispatchers.IO) {
      Log.v(LOG_NAME, "Saving GPS location to database.")

      if (gpsLocation.time > 0) {
         val locationProperties = ArrayList<LocationProperty>()

         locationProperties.add(
            LocationProperty(
               "accuracy",
               gpsLocation.accuracy
            )
         )
         locationProperties.add(
            LocationProperty(
               "bearing",
               gpsLocation.bearing
            )
         )
         locationProperties.add(
            LocationProperty(
               "speed",
               gpsLocation.speed
            )
         )
         locationProperties.add(
            LocationProperty(
               "provider",
               gpsLocation.provider
            )
         )
         locationProperties.add(
            LocationProperty(
               "altitude",
               gpsLocation.altitude
            )
         )
         locationProperties.add(
            LocationProperty(
               "accuracy_type",
               if (locationAccess.isPreciseLocationGranted()) "PRECISE" else "COARSE"
            )
         )

         val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
         level?.let {
            locationProperties.add(
               LocationProperty(
                  "battery_level",
                  it
               )
            )
         }

         var user: User? = null
         try {
            user = userLocalDataSource.readCurrentUser()
         } catch (e: UserException) {
            Log.e(LOG_NAME, "Error reading current user from database", e)
         }

         if (user != null && user.currentEvent != null) {
            try {
               val location = Location(
                  "Feature",
                  user,
                  locationProperties,
                  Point(gpsLocation.longitude, gpsLocation.latitude),
                  Date(gpsLocation.time),
                  user.currentEvent
               )

               locationLocalDataSource.create(location)
            } catch (e: LocationException) {
               Log.e(LOG_NAME, "Error saving GPS location", e)
            }
         } else {
            Log.e(LOG_NAME, "Not saving location for user: $user in event: ${user?.currentEvent}")
         }
      }
   }

   suspend fun pushLocations(): Boolean = withContext(Dispatchers.IO) {
      if (tokenProvider.isExpired()) {
         return@withContext false
      }

      val currentUser = userLocalDataSource.readCurrentUser() ?: return@withContext false

      var success = true
      var locations = locationLocalDataSource.getCurrentUserLocations(currentUser, LOCATION_PUSH_BATCH_SIZE, false)
      // TODO: when locations can't be pushed, this condition never becomes false to terminate the loop
      while (locations.isNotEmpty()) {

         // Send locations for the current event
         val event = locations[0].event
         val localLocations = locations.filter { it.event == event }

         try {
            val response = locationService.pushLocations(event.remoteId, localLocations)
            if (response.isSuccessful) {
               val pushedLocations = response.body() ?: emptyList()
               // We've sync-ed locations to the server, lets remove the locations we synced from the database
               Log.d(LOG_NAME, "Pushed " + pushedLocations.size + " locations.")
               try {
                  localLocations.forEachIndexed { index, localLocation ->
                     val remoteId = pushedLocations.getOrNull(index)?.remoteId
                     if (remoteId == null) {
                        locationLocalDataSource.delete(listOf(localLocation))
                     } else {
                        localLocation.remoteId = remoteId
                        locationLocalDataSource.update(localLocation)
                     }
                  }

                  val syncedLocations = locationLocalDataSource.getSyncedLocations(currentUser, event)

                  if (syncedLocations.size > minNumberOfLocationsToKeep) {
                     val locationsToDelete = syncedLocations.subList(minNumberOfLocationsToKeep, syncedLocations.size)

                     try {
                        locationLocalDataSource.delete(locationsToDelete)
                     } catch (e: LocationException) {
                        Log.e(LOG_NAME, "Could not delete locations.", e)
                     }
                  }
               } catch (e: SQLException) {
                  Log.e(LOG_NAME, "Problem deleting locations.", e)
               }
            } else {
               Log.e(LOG_NAME, "Failed to push locations.")
               response.errorBody()?.string()?.let {
                  Log.e(LOG_NAME, "Failed to push locations with error $it")
               }
               success = false
            }

            locations = locationLocalDataSource.getCurrentUserLocations(currentUser, LOCATION_PUSH_BATCH_SIZE, false)
         } catch (e: Exception) {
            Log.e(LOG_NAME, "Failed to push user locations to the server", e)
         }
      }
      success
   }

   fun getLocations(): Flow<List<Location>> = callbackFlow {
      val locationListener = object: ILocationEventListener {
         override fun onLocationCreated(locations: Collection<Location>) {
            trySend(query(this@callbackFlow))
         }

         override fun onLocationUpdated(location: Location) {
            trySend(query(this@callbackFlow))
         }

         override fun onLocationDeleted(location: MutableCollection<Location>) {}
         override fun onError(error: Throwable?) {}
      }
      locationLocalDataSource.addListener(locationListener)

      val locationFilterKey = context.resources.getString(R.string.activeLocationTimeFilterKey)
      val preferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
         if (locationFilterKey == key) {
            trySend(query(this@callbackFlow))
         }
      }
      preferences.registerOnSharedPreferenceChangeListener(preferencesListener)

      trySend(query(this))

      awaitClose {
         locationLocalDataSource.removeListener(locationListener)
         preferences.unregisterOnSharedPreferenceChangeListener(preferencesListener)
      }

   }.flowOn(Dispatchers.IO)

   private fun query(scope: ProducerScope<List<Location>>): List<Location> {
      val user = userLocalDataSource.readCurrentUser()
      val locations = locationLocalDataSource.getAllUsersLocations(user, getTemporalFilter())
      locations.lastOrNull()?.let { location ->
         if (oldestLocation == null || oldestLocation?.timestamp?.after(location.timestamp) == true) {
            oldestLocation = location

            refreshJob?.cancel()
            refreshJob = scope.launch {
               delay(location.timestamp.time - refreshTime)
               oldestLocation = null
               scope.trySend(query(scope))
            }
         }
      }

      return locations
   }

   private fun getTemporalFilter(): Filter<Temporal>? {
      var filter: Filter<Temporal>? = null

      val date: Date? = when (getLocationTimeFilterId()) {
         context.resources.getInteger(R.integer.time_filter_last_month) -> {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, -1)
            calendar.time
         }
         context.resources.getInteger(R.integer.time_filter_last_week) -> {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, -7)
            calendar.time
         }
         context.resources.getInteger(R.integer.time_filter_last_24_hours) -> {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.HOUR, -24)
            calendar.time
         }
         context.resources.getInteger(R.integer.time_filter_today) -> {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.time
         }
         context.resources.getInteger(R.integer.time_filter_custom) -> {
            val calendar = Calendar.getInstance()
            val customFilterTimeUnit = getCustomTimeUnit()
            val customTimeNumber = getCustomTimeNumber()
            when (customFilterTimeUnit) {
               "Hours" -> calendar.add(Calendar.HOUR_OF_DAY, -1 * customTimeNumber)
               "Days" -> calendar.add(Calendar.DAY_OF_MONTH, -1 * customTimeNumber)
               "Months" -> calendar.add(Calendar.MONTH, -1 * customTimeNumber)
               else -> calendar.add(Calendar.MINUTE, -1 * customTimeNumber)
            }

            calendar.time
         } else -> null
      }

      if (date != null) {
         filter = DateTimeFilter(date, null, "timestamp")
      }

      return filter
   }

   private fun getCustomTimeUnit(): String? {
      return preferences.getString(context.resources.getString(R.string.customLocationTimeUnitFilterKey), context.resources.getStringArray(R.array.timeUnitEntries)[0])
   }

   private fun getCustomTimeNumber(): Int {
      return preferences.getInt(context.resources.getString(R.string.customLocationTimeNumberFilterKey), 0)
   }

   private fun getLocationTimeFilterId(): Int {
      return preferences.getInt(context.resources.getString(R.string.activeLocationTimeFilterKey), context.resources.getInteger(R.integer.time_filter_last_month))
   }

   suspend fun fetch() = withContext(Dispatchers.IO) {
      val currentEvent = eventLocalDataSource.currentEvent ?: return@withContext
      val currentUser = userLocalDataSource.readCurrentUser() ?: return@withContext

      try {
         val response = locationService.getLocations(currentEvent.remoteId)
         if (response.isSuccessful) {
            val locations = response.body()?.flatMap { (_, locations) ->
               locations.forEach { it.event = currentEvent }
               locations
            } ?: emptyList()

            locations.forEach { location ->
               // make sure that the user exists and is persisted in the local data-store
               var userId: String? = null
               val userIdProperty = location.propertiesMap["userId"]
               if (userIdProperty != null) {
                  userId = userIdProperty.value.toString()
               }
               if (userId != null) {
                  var user: User? = userLocalDataSource.read(userId)
                  if (user == null) {
                     // get any users that were not recognized or expired
                     Log.d(LOG_NAME, "User for location is null or stale, re-pulling")
                     userRepository.fetchUsers(listOf(userId))
                     user = userLocalDataSource.read(userId)
                  }
                  location.user = user

                  // if there is no existing location, create one
                  val existingLocation = locationLocalDataSource.read(location.remoteId)
                  if (existingLocation == null) {
                     // delete old location and create new one
                     if (user != null && user != currentUser) {
                        // don't pull your own locations
                        userId = user.id.toString()
                        val newLocation = locationLocalDataSource.create(location)
                        locationLocalDataSource.deleteUserLocations(userId, true, newLocation.event)
                     } else {
                        Log.w(LOG_NAME, "A location with no user was found and discarded.  User id: $userId")
                     }
                  }
               }
            }
         }
      } catch(e: Exception) {
         Log.e(LOG_NAME, "Failed to fetch user locations from server", e)
      }
   }

   suspend fun getLocations(
      minLatitude: Double,
      maxLatitude: Double,
      minLongitude: Double,
      maxLongitude: Double,
   ): List<Location> {
      val user = userLocalDataSource.readCurrentUser()
      val locations = locationLocalDataSource.getAllUsersLocations(
         user = user,
         filter = getTemporalFilter(),
         minLatitude = minLatitude,
         maxLatitude = maxLatitude,
         minLongitude = minLongitude,
         maxLongitude = maxLongitude
      )
      return locations
   }

   companion object {
      private val LOG_NAME = LocationRepository::class.java.name

      private const val LOCATION_PUSH_BATCH_SIZE: Long = 100
      @JvmStatic val minNumberOfLocationsToKeep = 40
   }
}
