package mil.nga.giat.mage.data.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.j256.ormlite.stmt.Where
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import mil.nga.giat.mage.R
import mil.nga.giat.mage.filter.DateTimeFilter
import mil.nga.giat.mage.filter.Filter
import mil.nga.giat.mage.location.LocationAccess
import mil.nga.giat.mage.network.api.LocationService
import mil.nga.giat.mage.sdk.Temporal
import mil.nga.giat.mage.sdk.datastore.DaoStore
import mil.nga.giat.mage.sdk.datastore.location.Location
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper
import mil.nga.giat.mage.sdk.datastore.location.LocationProperty
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import mil.nga.giat.mage.sdk.datastore.user.Permission
import mil.nga.giat.mage.sdk.datastore.user.User
import mil.nga.giat.mage.sdk.datastore.user.UserHelper
import mil.nga.giat.mage.sdk.event.ILocationEventListener
import mil.nga.giat.mage.sdk.exceptions.LocationException
import mil.nga.giat.mage.sdk.exceptions.UserException
import mil.nga.giat.mage.sdk.fetch.UserServerFetch
import mil.nga.giat.mage.sdk.http.resource.LocationResource
import mil.nga.giat.mage.sdk.utils.UserUtility
import mil.nga.sf.Point
import java.sql.SQLException
import java.util.*
import javax.inject.Inject

class LocationRepository @Inject constructor(
   @ApplicationContext private val context: Context,
   private val preferences: SharedPreferences,
   private val locationAccess: LocationAccess,
   private val locationService: LocationService
) {
   private val userFetch: UserServerFetch = UserServerFetch(context)
   private val userHelper: UserHelper = UserHelper.getInstance(context)
   private val locationHelper: LocationHelper = LocationHelper.getInstance(context)
   private val locationDao = DaoStore.getInstance(context).locationDao
   private var batteryStatus: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

   private var refreshTime: Long = 0
   private var refreshJob: Job? = null
   private var oldestLocation: Location? = null

   suspend fun saveLocation(gpsLocation: android.location.Location) = withContext(Dispatchers.IO) {
      Log.v(LOG_NAME, "Saving GPS location to database.")

      if (gpsLocation.time > 0) {
         val locationProperties = ArrayList<LocationProperty>()

         val locationHelper = LocationHelper.getInstance(context)

         locationProperties.add(LocationProperty("accuracy", gpsLocation.accuracy))
         locationProperties.add(LocationProperty("bearing", gpsLocation.bearing))
         locationProperties.add(LocationProperty("speed", gpsLocation.speed))
         locationProperties.add(LocationProperty("provider", gpsLocation.provider))
         locationProperties.add(LocationProperty("altitude", gpsLocation.altitude))
         locationProperties.add(LocationProperty("accuracy_type", if (locationAccess.isPreciseLocationGranted()) "PRECISE" else "COARSE"))

         val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
         level?.let {
            locationProperties.add(LocationProperty("battery_level", it))
         }

         var user: User? = null
         try {
            user = UserHelper.getInstance(context).readCurrentUser()
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
                  user.currentEvent)

               locationHelper.create(location)
            } catch (e: LocationException) {
               Log.e(LOG_NAME, "Error saving GPS location", e)
            }
         } else {
            Log.e(LOG_NAME, "Not saving location for user: $user in event: ${user?.currentEvent}")
         }
      }
   }

   suspend fun pushLocations(): Boolean = withContext(Dispatchers.IO) {
      if (UserUtility.getInstance(context).isTokenExpired) {
         return@withContext false
      }
      val locationResource = LocationResource(context)
      val locationHelper = LocationHelper.getInstance(context)

      var currentUser: User? = null
      try {
         currentUser = UserHelper.getInstance(context).readCurrentUser()
      } catch (e: UserException) {
         Log.e(LOG_NAME, "error reading current user", e)
      }

      var success = true
      var locations = locationHelper.getCurrentUserLocations(LOCATION_PUSH_BATCH_SIZE, false)
      // TODO: when locations can't be pushed, this condition never becomes false to terminate the loop
      while (locations.isNotEmpty()) {

         // Send locations for the current event
         val event = locations[0].event

         val eventLocations = ArrayList<Location>()
         for (l in locations) {
            if (event == l.event) {
               eventLocations.add(l)
            }
         }

         try {
            if (locationResource.createLocations(event, eventLocations)) {
               // We've sync-ed locations to the server, lets remove the locations we synced from the database
               Log.d(LOG_NAME, "Pushed " + eventLocations.size + " locations.")

               // Delete location where:
               // * user is current user
               // * remote id is set. (have been sent to server)
               // * past the lower n amount
               try {
                  if (currentUser != null) {
                     val locationDao = DaoStore.getInstance(context).locationDao
                     val queryBuilder = locationDao.queryBuilder()
                     val where = queryBuilder.where().eq("user_id", currentUser.id)
                     where.and().isNotNull("remote_id").and().eq("event_id", event.id)
                     queryBuilder.orderBy("timestamp", false)
                     val pushedLocations = queryBuilder.query()

                     if (pushedLocations.size > minNumberOfLocationsToKeep) {
                        val locationsToDelete = pushedLocations.subList(
                           minNumberOfLocationsToKeep, pushedLocations.size)

                        try {
                           LocationHelper.getInstance(context).delete(locationsToDelete)
                        } catch (e: LocationException) {
                           Log.e(LOG_NAME, "Could not delete locations.", e)
                        }

                     }
                  }
               } catch (e: SQLException) {
                  Log.e(LOG_NAME, "Problem deleting locations.", e)
               }
            } else {
               Log.e(LOG_NAME, "Failed to push locations.")
               success = false
            }

            locations = locationHelper.getCurrentUserLocations(LOCATION_PUSH_BATCH_SIZE, false)
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
      locationHelper.addListener(locationListener)

      val locationFilterKey = context.resources.getString(R.string.activeLocationTimeFilterKey)
      val preferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
         if (locationFilterKey == key) {
            trySend(query(this@callbackFlow))
         }
      }
      preferences.registerOnSharedPreferenceChangeListener(preferencesListener)

      trySend(query(this))

      awaitClose {
         locationHelper.removeListener(locationListener)
         preferences.unregisterOnSharedPreferenceChangeListener(preferencesListener)
      }

   }.flowOn(Dispatchers.IO)

   private fun query(scope: ProducerScope<List<Location>>): List<Location> {
      val dao = DaoStore.getInstance(context).locationDao
      val query = dao.queryBuilder()
      val where: Where<out Temporal?, Long> = query.where()

      val currentUser: User? = try {
         userHelper.readCurrentUser()
      } catch (ignore: UserException) { null }

      if (currentUser != null) {
         where
            .ne("user_id", currentUser.id)
            .and()
            .eq("event_id", currentUser.userLocal.currentEvent.id)
      }

      getTemporalFilter()?.let { filter ->
         filter.query()?.let { query.join(it) }
         filter.and(where)
      }

      query.orderBy("timestamp", false)

      val iterator = locationDao.iterator(query.prepare())

      val locations = mutableListOf<Location>()
      while(iterator.hasNext()) {
         locations.add(iterator.current())
      }

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
      var currentUser: User? = null
      try {
         currentUser = userHelper.readCurrentUser()
      } catch (e: UserException) {
         Log.e(LOG_NAME, "Error reading current user.", e)
      }

      val event = EventHelper.getInstance(context).currentEvent
      try {
         val response = locationService.getLocations(event.remoteId)
         if (response.isSuccessful) {
            val foo = response.body()!!
            val locations = foo.map {
               it.event = event
               it
            }

            for (location in locations) {
               // make sure that the user exists and is persisted in the local data-store
               var userId: String? = null
               val userIdProperty = location.propertiesMap["userId"]
               if (userIdProperty != null) {
                  userId = userIdProperty.value.toString()
               }
               if (userId != null) {
                  var user: User? = userHelper.read(userId)
                  // TODO : test the timer to make sure users are updated as needed!
                  val sixHoursInMilliseconds = (6 * 60 * 60 * 1000).toLong()
                  if (user == null || Date().after(Date(user.fetchedDate.time + sixHoursInMilliseconds))) {
                     // get any users that were not recognized or expired
                     Log.d(LOG_NAME, "User for location is null or stale, re-pulling")
                     userFetch.fetch(userId)
                     user = userHelper.read(userId)
                  }
                  location.user = user

                  // if there is no existing location, create one
                  val l = locationHelper.read(location.remoteId)
                  if (l == null) {
                     // delete old location and create new one
                     if (user != null) {
                        // don't pull your own locations
                        if (user != currentUser) {
                           userId = user.id.toString()
                           val newLocation = locationHelper.create(location)
                           locationHelper.deleteUserLocations(userId, true, newLocation.event)
                        }
                     } else {
                        Log.w(LOG_NAME, "A location with no user was found and discarded.  User id: $userId")
                     }
                  }
               }
            }
         }

         response
      } catch(e: Exception) {
         Log.e(LOG_NAME, "Failed to fetch user locations from server", e)
      }
   }

   companion object {
      private val LOG_NAME = LocationRepository::class.java.name

      private const val LOCATION_PUSH_BATCH_SIZE: Long = 100
      @JvmStatic val minNumberOfLocationsToKeep = 40
   }
}
