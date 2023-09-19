package mil.nga.giat.mage.observation.sync

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.repository.observation.ObservationRepository
import javax.inject.Inject

@AndroidEntryPoint
class ObservationFetchService : LifecycleService(), SharedPreferences.OnSharedPreferenceChangeListener  {

    @Inject @ApplicationContext
    lateinit var context: Context

    @Inject
    lateinit var preferences: SharedPreferences

    @Inject
    lateinit var observationRepository: ObservationRepository

    private var observationFetchFrequency: Long = 0
    private var initialFetch = true
    private var pollJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        observationFetchFrequency = getObservationFetchFrequency()
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        preferences.unregisterOnSharedPreferenceChangeListener(this)
        stopPoll()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (pollJob?.isActive != true) {
            startPoll()
        }

        return START_NOT_STICKY
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key.equals(getString(R.string.observationFetchFrequencyKey), ignoreCase = true)) {
            observationFetchFrequency = getObservationFetchFrequency()
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
                try {
                    observationRepository.fetch(notify = !initialFetch)
                    initialFetch = false
                } catch (e: Exception) {
                    Log.i("Wha", "Who")
                }

                delay(timeMillis = getObservationFetchFrequency())
            }
        }
    }

    private fun getObservationFetchFrequency(): Long {
        return preferences.getInt(getString(R.string.observationFetchFrequencyKey), resources.getInteger(R.integer.observationFetchFrequencyDefaultValue)).toLong()
    }
}
