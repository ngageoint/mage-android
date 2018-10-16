package mil.nga.giat.mage.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.util.Log
import dagger.android.AndroidInjection
import mil.nga.giat.mage.MageApplication
import mil.nga.giat.mage.R
import java.util.concurrent.*
import javax.inject.Inject

open class LocationReportingService : LifecycleService(), Observer<Location>, LocationSaveTask.LocationDatabaseListener, LocationPushTask.LocationSyncListener, SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    lateinit var locationProvider: LocationProvider

    @Inject
    lateinit var preferences: SharedPreferences

    private var shouldReportLocation: Boolean = false
    private var locationPushFrequency: Long = 0
    private var oldestLocationTime: Long = 0

    companion object {
        private val LOG_NAME = LocationReportingService::class.java.name

        const val NOTIFICATION_ID = 500
        const val NOTIFICATION_CHANNEL_ID = "mil.nga.mage.LOCATION_NOTIFICATION_CHANNEL"

        val SAVE_EXECUTOR: Executor = Executors.newSingleThreadExecutor()
        val PUSH_EXECUTOR: Executor = ThreadPoolExecutor(1, 1, 1L, TimeUnit.SECONDS, SynchronousQueue(), ThreadPoolExecutor.DiscardPolicy())
    }

    override fun onCreate() {
        super.onCreate()

        AndroidInjection.inject(this)

        preferences.registerOnSharedPreferenceChangeListener(this)

        locationPushFrequency = getLocationPushFrequency()
        shouldReportLocation = getShouldReportLocation()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "MAGE", NotificationManager.IMPORTANCE_MIN)
            channel.setShowBadge(true)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("MAGE Location Service")
                .setContentText("MAGE is currently reporting your location.")
                .setSmallIcon(R.drawable.ic_place_white_24dp)
                .setGroup(MageApplication.MAGE_NOTIFICATION_GROUP)
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
        if (shouldReportLocation && location!!.provider == LocationManager.GPS_PROVIDER) {
            Log.v(LOG_NAME, "GPS location changed")
            LocationSaveTask(applicationContext, this).executeOnExecutor(SAVE_EXECUTOR, location)
        }
    }

    override fun onSaveComplete(location: Location?) {
        location?.let {
            if (it.time - oldestLocationTime > locationPushFrequency) {
                LocationPushTask(applicationContext, this).executeOnExecutor(PUSH_EXECUTOR)
            }

            if (oldestLocationTime == 0L) {
                oldestLocationTime = it.time
            }
        }
    }

    override fun onSyncComplete(status: Boolean) {
        if (status) {
            oldestLocationTime = 0
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
