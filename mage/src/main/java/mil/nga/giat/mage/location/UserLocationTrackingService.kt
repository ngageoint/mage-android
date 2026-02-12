package mil.nga.giat.mage.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mil.nga.giat.mage.MageApplication
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.data.datasource.location.LocationLocalDataSource
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.database.model.location.LocationProperty
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.di.TokenProvider
import mil.nga.giat.mage.login.LoginActivity
import mil.nga.giat.mage.network.location.LocationService
import mil.nga.giat.mage.sdk.exceptions.LocationException
import mil.nga.giat.mage.sdk.exceptions.UserException
import mil.nga.sf.Point
import java.sql.SQLException
import java.util.ArrayList
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class UserLocationTrackingService : LifecycleService(), SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject lateinit var locationProvider: UserLocationProvider
    @Inject lateinit var locationAccess: LocationAccessPermissionsState
    @Inject lateinit var userLocalDataSource: UserLocalDataSource
    @Inject lateinit var locationLocalDataSource: LocationLocalDataSource
    @Inject lateinit var tokenProvider: TokenProvider
    @Inject lateinit var eventLocalDataSource: EventLocalDataSource
    @Inject lateinit var locationService: LocationService
    @Inject lateinit var preferences: SharedPreferences
    @Inject lateinit var mageApp: MageApplication

    private var shouldReportLocation: Boolean = false
    private var locationPushFrequency: Long = 0
    private var locationTimeForLastPush: Long = 0
    private var userLocationUpdatesJob: Job? = null
    private var isFirstLocationInSession = true
    private val isPushing = AtomicBoolean(false)
    private var batteryStatus: Intent? = null

    companion object {
        private val LOG_NAME = UserLocationTrackingService::class.java.name

        private const val NOTIFICATION_ID = 500
        private const val NOTIFICATION_CHANNEL_ID = "mil.nga.mage.LOCATION_NOTIFICATION_CHANNEL"
        private const val LOCATION_PUSH_BATCH_SIZE: Long = 100
        private const val MIN_NUMBER_OF_LOCATIONS_TO_KEEP = 40
    }

    override fun onCreate() {
        super.onCreate()

        // If the user disables the location permission from settings, MAGE will be restarted, including this service (Service.START_STICKY)
        // Check for location permission here as it may have been disabled, if so stop the service.
        if (locationAccess.isLocationDenied()) {
            stopForeground(true)
            return
        }

        batteryStatus = mageApp.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        preferences.registerOnSharedPreferenceChangeListener(this)

        locationPushFrequency = getLocationPushFrequency()
        shouldReportLocation = mageApp.shouldReportLocation()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "MAGE", NotificationManager.IMPORTANCE_MIN)
        notificationChannel.setShowBadge(true)
        notificationManager.createNotificationChannel(notificationChannel)

        val intent = Intent(applicationContext, LoginActivity::class.java)
        intent.putExtra("LOGOUT", true)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(applicationContext, 1, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("MAGE Location Service")
            .setContentText("MAGE is currently reporting your location.")
            .setSmallIcon(R.drawable.ic_place_white_24dp)
            .setGroup(MageApplication.MAGE_NOTIFICATION_GROUP)
            .addAction(R.drawable.ic_power_settings_new_white_24dp, "Logout", pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (userLocationUpdatesJob == null) {
            userLocationUpdatesJob = lifecycleScope.launch {
                locationProvider.locationUpdates.filterNotNull().collect { location ->
                    if (shouldReportLocation && location.provider == LocationManager.GPS_PROVIDER) {
                        Log.v(LOG_NAME, "GPS location changed")

                        launch {
                            saveLocation(location)
                            checkForPushEligibility(location)
                        }
                    }
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            userLocationUpdatesJob?.cancel()
            preferences.unregisterOnSharedPreferenceChangeListener(this)
        } catch (e: Exception) {
            Log.d(LOG_NAME, "Error shutting down service: " + e.message)
        }
    }

    private suspend fun checkForPushEligibility(location: Location) {
        if (isFirstLocationInSession ||
            (location.time - locationTimeForLastPush > locationPushFrequency) ||
            !locationAccess.isPreciseLocationGranted()) {
            if (isPushing.compareAndSet(false, true)) {
                try {
                    val success = pushLocations()
                    if (success) {
                        isFirstLocationInSession = false
                        locationTimeForLastPush = location.time
                    }
                } finally {
                    isPushing.set(false)
                }
            }
        }
    }

    private suspend fun pushLocations(): Boolean = withContext(Dispatchers.IO) {
        if (tokenProvider.isExpired()) {
            return@withContext false
        }

        ensureActive()

        val currentUser = userLocalDataSource.readCurrentUser() ?: return@withContext false
        var success = true

        do {
            val locationsToPush = locationLocalDataSource.getCurrentUserLocations(currentUser, LOCATION_PUSH_BATCH_SIZE, false)

            val currentEvent = eventLocalDataSource.currentEvent
            if (locationsToPush.isEmpty() || currentEvent == null) {
                break
            }

            val currentLocations = locationsToPush.filter { it.event == currentEvent }
            if (currentLocations.isEmpty()) {
                break
            }

            try {
                val response = locationService.pushUserLocation(currentEvent.remoteId, currentLocations)
                if (response.isSuccessful) {
                    val pushedLocations = response.body() ?: emptyList()
                    Log.d(LOG_NAME, "Pushed " + pushedLocations.size + " locations.")

                    try {
                        //update database locations with remote_ids, or delete them if they failed to push individually
                        currentLocations.forEachIndexed { index, location ->
                            val remoteId = pushedLocations.getOrNull(index)?.remoteId
                            if (remoteId == null) {
                                locationLocalDataSource.delete(listOf(location))
                            } else {
                                location.remoteId = remoteId
                                locationLocalDataSource.update(location)
                            }
                        }

                        //prune synced locations to keep the table size in check
                        val syncedLocations = locationLocalDataSource.getSyncedLocations(currentUser, currentEvent)
                        if (syncedLocations.size > MIN_NUMBER_OF_LOCATIONS_TO_KEEP) {
                            val locationsToDelete = syncedLocations.subList(
                                MIN_NUMBER_OF_LOCATIONS_TO_KEEP, syncedLocations.size)
                            try {
                                locationLocalDataSource.delete(locationsToDelete)
                            } catch (e: Exception) {
                                Log.e(LOG_NAME, "Could not delete locations.", e)
                            }
                        }
                    } catch (e: SQLException) {
                        Log.e(LOG_NAME, "Problem cleaning up pushed locations.", e)
                    }
                } else {
                    Log.e(LOG_NAME, "Failed to push locations.")
                    response.errorBody()?.string()?.let {
                        Log.e(LOG_NAME, "Failed to push locations with error $it")
                    }
                    success = false

                    break
                }
            } catch (e: Exception) {
                Log.e(LOG_NAME, "Failed to push user locations to the server", e)
                success = false
                break

            }
        } while (locationsToPush.size == LOCATION_PUSH_BATCH_SIZE.toInt())

        return@withContext success
    }

    private suspend fun saveLocation(gpsLocation: android.location.Location) = withContext(Dispatchers.IO) {
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
                    val location = mil.nga.giat.mage.database.model.location.Location(
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

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key.equals(getString(R.string.reportLocationKey), ignoreCase = true)) {
            shouldReportLocation = mageApp.shouldReportLocation()
            Log.d(LOG_NAME, "Report location changed $shouldReportLocation")
        } else if (key.equals(getString(R.string.locationPushFrequencyKey), ignoreCase = true)) {
            locationPushFrequency = getLocationPushFrequency()
            Log.d(LOG_NAME, "Location push frequency changed $locationPushFrequency")
        }
    }

    private fun getLocationPushFrequency(): Long {
        return preferences.getInt(getString(R.string.locationPushFrequencyKey), resources.getInteger(R.integer.locationPushFrequencyDefaultValue)).toLong()
    }
}
