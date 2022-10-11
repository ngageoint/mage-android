package mil.nga.giat.mage.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mil.nga.giat.mage.MageApplication
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.location.LocationRepository
import mil.nga.giat.mage.login.LoginActivity
import javax.inject.Inject

@AndroidEntryPoint
open class LocationReportingService : LifecycleService(), Observer<Location>, SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject lateinit var locationProvider: LocationProvider
    @Inject lateinit var locationRepository: LocationRepository
    @Inject lateinit var locationAccess: LocationAccess
    @Inject lateinit var preferences: SharedPreferences

    private var shouldReportLocation: Boolean = false
    private var locationPushFrequency: Long = 0
    private var oldestLocationTime: Long = 0

    companion object {
        private val LOG_NAME = LocationReportingService::class.java.name

        const val NOTIFICATION_ID = 500
        const val NOTIFICATION_CHANNEL_ID = "mil.nga.mage.LOCATION_NOTIFICATION_CHANNEL"
    }

    override fun onCreate() {
        super.onCreate()

        // If the user disables the location permission from settings, MAGE will be restarted, including this service (Service.START_STICKY)
        // Check for location permission here as it may have been disabled, if so stop the service.
        if (locationAccess.isLocationDenied()) {
            stopForeground(true)
            return
        }

        preferences.registerOnSharedPreferenceChangeListener(this)

        locationPushFrequency = getLocationPushFrequency()
        shouldReportLocation = getShouldReportLocation()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "MAGE", NotificationManager.IMPORTANCE_MIN)
            channel.setShowBadge(true)
            notificationManager.createNotificationChannel(channel)
        }

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

        locationProvider.observe(this, this)

        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        locationProvider.removeObserver(this)

        preferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onChanged(location: Location?) {
        if (shouldReportLocation && location?.provider == LocationManager.GPS_PROVIDER) {
            Log.v(LOG_NAME, "GPS location changed")

            lifecycleScope.launch {
                locationRepository.saveLocation(location)

                if (oldestLocationTime == 0L) {
                    oldestLocationTime = location.time
                }

                if (!locationAccess.isPreciseLocationGranted() || (location.time - oldestLocationTime > locationPushFrequency)) {
                    val success = locationRepository.pushLocations()
                    if (success) {
                        oldestLocationTime = 0
                    }
                }
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key.equals(getString(R.string.reportLocationKey), ignoreCase = true)) {
            shouldReportLocation = getShouldReportLocation()
            Log.d(LOG_NAME, "Report location changed $shouldReportLocation")
        } else if (key.equals(getString(R.string.locationPushFrequencyKey), ignoreCase = true)) {
            locationPushFrequency = getLocationPushFrequency()
            Log.d(LOG_NAME, "Location push frequency changed $locationPushFrequency")
        }
    }

    private fun getLocationPushFrequency(): Long {
        return preferences.getInt(getString(R.string.locationPushFrequencyKey), resources.getInteger(R.integer.locationPushFrequencyDefaultValue)).toLong()
    }

    private fun getShouldReportLocation(): Boolean {
        return preferences.getBoolean(getString(R.string.reportLocationKey), resources.getBoolean(R.bool.reportLocationDefaultValue))
    }
}
