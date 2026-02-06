package mil.nga.giat.mage.data.repository.location

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
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
import mil.nga.giat.mage.location.LocationAccessPermissionsState
import mil.nga.giat.mage.network.location.LocationService
import mil.nga.giat.mage.sdk.Temporal
import mil.nga.giat.mage.database.model.location.Location
import mil.nga.giat.mage.data.datasource.location.LocationLocalDataSource
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.sdk.event.ILocationEventListener
import java.util.*
import javax.inject.Inject

class EventLocationsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: SharedPreferences,
    private val locationService: LocationService,
    private val userRepository: UserRepository,
    private val userLocalDataSource: UserLocalDataSource,
    private val eventLocalDataSource: EventLocalDataSource,
    private val locationLocalDataSource: LocationLocalDataSource
) {
    private var refreshTime: Long = 0
    private var refreshJob: Job? = null
    private var oldestLocation: Location? = null


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
        val locations = locationLocalDataSource.getEventLocationsExcludingCurrentUser(user, getTemporalFilter())
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

    suspend fun fetch() = withContext(Dispatchers.IO) {
        val currentEvent = eventLocalDataSource.currentEvent ?: return@withContext
        val currentUser = userLocalDataSource.readCurrentUser() ?: return@withContext

        try {
            val response = locationService.getEventLocations(currentEvent.remoteId)
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

    companion object {
        private val LOG_NAME = EventLocationsRepository::class.java.name
    }
}
