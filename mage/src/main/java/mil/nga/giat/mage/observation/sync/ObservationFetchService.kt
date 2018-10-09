package mil.nga.giat.mage.observation.sync

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import dagger.android.AndroidInjection
import mil.nga.giat.mage.R
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ObservationFetchService : Service(), SharedPreferences.OnSharedPreferenceChangeListener  {

    @Inject
    lateinit var context: Context

    @Inject
    lateinit var preferences: SharedPreferences

    private var observationFetchFrequency: Long = 0;
    private var executor: ScheduledExecutorService
    private var future: ScheduledFuture<*>? = null
    private var intialFetch = true;

    init {
        executor = Executors.newScheduledThreadPool(1);
    }

    override fun onCreate() {
        super.onCreate()

        AndroidInjection.inject(this)

        observationFetchFrequency = getObservationFetchFrequency()
        preferences.registerOnSharedPreferenceChangeListener(this);

        scheduleFetch()
    }

    override fun onDestroy() {
        super.onDestroy()

        preferences.unregisterOnSharedPreferenceChangeListener(this);
        stopFetch()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key.equals(getString(R.string.observationFetchFrequencyKey), ignoreCase = true)) {
            observationFetchFrequency = getObservationFetchFrequency();
            stopFetch()
            scheduleFetch()
        }
    }

    private fun scheduleFetch() {
        future = executor.scheduleAtFixedRate({
            ObservationServerFetch(context).fetch(notify = !intialFetch)
            intialFetch = false;
        }, 0, getObservationFetchFrequency(), TimeUnit.MILLISECONDS)
    }

    private fun stopFetch() {
        future?.cancel(true)
    }

    private fun getObservationFetchFrequency(): Long {
        return preferences.getInt(getString(R.string.observationFetchFrequencyKey), resources.getInteger(R.integer.observationFetchFrequencyDefaultValue)).toLong()
    }
}
