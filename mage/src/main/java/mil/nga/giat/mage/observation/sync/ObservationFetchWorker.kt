package mil.nga.giat.mage.observation.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import mil.nga.giat.mage.sdk.utils.UserUtility
import java.util.*
import java.util.concurrent.TimeUnit

class ObservationFetchWorker(var context: Context, params: WorkerParameters) : Worker(context, params) {

    companion object {
        private val LOG_NAME = ObservationFetchWorker::class.java.simpleName
        private val OBSERVATION_FETCH_WORK= "mil.nga.mage.OBSERVATION_FETCH_WORK"

        private fun workRequest(): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            return PeriodicWorkRequestBuilder<ObservationFetchWorker>(30, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()
        }

        fun beginWork(): UUID {
            val request = workRequest()
            WorkManager.getInstance().enqueueUniquePeriodicWork(OBSERVATION_FETCH_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
            return request.id
        }

        fun stopWork() {
            WorkManager.getInstance().cancelUniqueWork(OBSERVATION_FETCH_WORK);
        }
    }

    override fun doWork(): Result {
        // Check token
        if (UserUtility.getInstance(context).isTokenExpired()) {
            Log.d(LOG_NAME, "Token expired, turn off observation fetch worker.")
            return Result.FAILURE;
        }

        Log.d(LOG_NAME, "Fetching observations.")

        // Fetch observations
        // TODO would be nice to know if we got back a 401, in that case we should turn off
        ObservationServerFetch(context).fetch(notify = true)

        return Result.SUCCESS
    }
}