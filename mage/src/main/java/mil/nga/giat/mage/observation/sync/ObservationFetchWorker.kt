package mil.nga.giat.mage.observation.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import androidx.work.PeriodicWorkRequest.Companion.MIN_PERIODIC_INTERVAL_MILLIS
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import mil.nga.giat.mage.data.repository.observation.ObservationRepository
import mil.nga.giat.mage.di.TokenProvider
import java.util.*
import java.util.concurrent.TimeUnit

@HiltWorker
class ObservationFetchWorker @AssistedInject constructor(
   @Assisted context: Context,
   @Assisted params: WorkerParameters,
   private val observationRepository: ObservationRepository,
   private val tokenProvider: TokenProvider
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (tokenProvider.isExpired()) {
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

            return PeriodicWorkRequestBuilder<ObservationFetchWorker>(MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
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