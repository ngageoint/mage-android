package mil.nga.giat.mage.location

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import dagger.android.AndroidInjection
import mil.nga.giat.mage.R
import mil.nga.giat.mage.dagger.module.ApplicationContext
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class LocationFetchService : Service(), SharedPreferences.OnSharedPreferenceChangeListener  {

    @Inject @field:ApplicationContext
    lateinit var context: Context

    @Inject
    lateinit var preferences: SharedPreferences

    private var locationFetchFrequency: Long = 0
    private var executor: ScheduledExecutorService
    private var future: ScheduledFuture<*>? = null

    init {
        executor = Executors.newScheduledThreadPool(1)
    }

    override fun onCreate() {
        super.onCreate()

        AndroidInjection.inject(this)

        locationFetchFrequency = getLocationFetchFrequency()
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        stopFetch()
        scheduleFetch()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        preferences.unregisterOnSharedPreferenceChangeListener(this)
        stopFetch()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key.equals(getString(R.string.userFetchFrequencyKey), ignoreCase = true)) {
            locationFetchFrequency = getLocationFetchFrequency()
            stopFetch()
            scheduleFetch()
        }
    }

    private fun scheduleFetch() {
        future = executor.scheduleAtFixedRate({
            LocationServerFetch(context).fetch()
        }, 0, getLocationFetchFrequency(), TimeUnit.MILLISECONDS)
    }

    private fun stopFetch() {
        future?.cancel(true)
    }

    private fun getLocationFetchFrequency(): Long {
        return preferences.getInt(getString(R.string.userFetchFrequencyKey), resources.getInteger(R.integer.userFetchFrequencyDefaultValue)).toLong()
    }
}
