package mil.nga.giat.mage.observation.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import mil.nga.giat.mage.data.observation.ObservationRepository
import mil.nga.giat.mage.sdk.utils.UserUtility
import java.util.*
import java.util.concurrent.TimeUnit

@HiltWorker
class ObservationFetchWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observationRepository: ObservationRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Check token
        if (UserUtility.getInstance(applicationContext).isTokenExpired) {
            Log.d(LOG_NAME, "Token expired, turn off observation fetch worker.")
            return Result.failure();
        }

        Log.d(LOG_NAME, "Fetching observations.")

        // Fetch observations
        // TODO would be nice to know if we got back a 401, in that case we should turn off
        observationRepository.fetch(notify = true)

        return Result.success()
    }

    companion object {
        private val LOG_NAME = ObservationFetchWorker::class.java.simpleName
        private const val OBSERVATION_FETCH_WORK= "mil.nga.mage.OBSERVATION_FETCH_WORK"

        private fun workRequest(): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            return PeriodicWorkRequestBuilder<ObservationFetchWorker>(1, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
        }

        fun beginWork(context: Context): UUID {
            val request = workRequest()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(OBSERVATION_FETCH_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
            return request.id
        }

        fun stopWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(OBSERVATION_FETCH_WORK);
        }
    }
}