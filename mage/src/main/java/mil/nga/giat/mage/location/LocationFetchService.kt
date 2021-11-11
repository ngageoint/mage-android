package mil.nga.giat.mage.location

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.location.LocationRepository
import javax.inject.Inject

@AndroidEntryPoint
class LocationFetchService : LifecycleService(), SharedPreferences.OnSharedPreferenceChangeListener  {

    @Inject @ApplicationContext
    lateinit var context: Context

    @Inject
    lateinit var preferences: SharedPreferences

    @Inject
    lateinit var locationRepository: LocationRepository

    private var locationFetchFrequency: Long = 0
    private var pollJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        locationFetchFrequency = getLocationFetchFrequency()
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (pollJob?.isActive != true) {
            startPoll()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        preferences.unregisterOnSharedPreferenceChangeListener(this)
        stopPoll()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key.equals(getString(R.string.userFetchFrequencyKey), ignoreCase = true)) {
            locationFetchFrequency = getLocationFetchFrequency()
            stopPoll()
            startPoll()
        }
    }

    private fun startPoll() {
        pollJob = poll()
    }

    private fun stopPoll() {
        pollJob?.cancel()
    }

    private fun poll(): Job {
        return lifecycleScope.launch {
            while (isActive) {
                locationRepository.fetch()
                delay(timeMillis = getLocationFetchFrequency())
            }
        }
    }

    private fun getLocationFetchFrequency(): Long {
        return preferences.getInt(getString(R.string.userFetchFrequencyKey), resources.getInteger(R.integer.userFetchFrequencyDefaultValue)).toLong()
    }
}
