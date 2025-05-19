package mil.nga.giat.mage.observation.sync

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mil.nga.giat.mage.MageApplication
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.repository.observation.ObservationRepository
import mil.nga.giat.mage.database.model.observation.Observation
import mil.nga.giat.mage.database.model.observation.ObservationFavorite
import mil.nga.giat.mage.data.datasource.observation.ObservationLocalDataSource
import mil.nga.giat.mage.database.model.observation.State
import java.util.concurrent.TimeUnit

@HiltWorker
class ObservationSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observationRepository: ObservationRepository,
    private val observationLocalDataSource: ObservationLocalDataSource
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        //Lock to ensure previous running work will complete when cancelled before new work is started.
        return mutex.withLock {
            try {
                //submit observations to the server that are marked as "dirty" and track the overall response flag of all transactions
                val overallResponseFlag = ResponseFlag.combineResponseFlags(
                    syncObservations(),
                    syncObservationImportant(),
                    syncObservationFavorites()
                )

                //if any of the transactions returned RetryFlag, then Result.retry() will be returned to retry the work request per back off policy
                //this should not result in the successful transactions being resubmitted, as they will no longer be marked as "dirty"
                when (overallResponseFlag) {
                    ResponseFlag.SuccessFlag -> Result.success()
                    ResponseFlag.FailureFlag -> Result.failure()
                    ResponseFlag.RetryFlag -> Result.retry()
                }
            } catch (e: Exception) {
                Log.e(LOG_NAME, "Error trying to sync observations with server", e)
                //any unhandled exception should result in a retry
                Result.retry()
            }
        }
    }

    private suspend fun syncObservations(): ResponseFlag {
        var overallResult: ResponseFlag = ResponseFlag.SuccessFlag

        for (observation in observationLocalDataSource.dirty) {
            val syncResult = syncObservation(observation)
            overallResult = ResponseFlag.combineResponseFlags(overallResult, syncResult)
        }

        return overallResult
    }

    private suspend fun syncObservation(observation: Observation): ResponseFlag {
        return if (observation.state == State.ARCHIVE) {
            archive(observation)
        } else {
            save(observation)
        }
    }

    private suspend fun syncObservationImportant(): ResponseFlag {
        var overallResult: ResponseFlag = ResponseFlag.SuccessFlag

        for (observation in observationLocalDataSource.dirtyImportant) {
            val updateResult = updateImportant(observation)
            overallResult = ResponseFlag.combineResponseFlags(overallResult, updateResult)
        }

        return overallResult
    }

    private suspend fun syncObservationFavorites(): ResponseFlag {
        var overallResult: ResponseFlag = ResponseFlag.SuccessFlag

        for (favorite in observationLocalDataSource.dirtyFavorites) {
            val updateResult = updateFavorite(favorite)
            overallResult = ResponseFlag.combineResponseFlags(overallResult, updateResult)
        }

        return overallResult
    }

    private suspend fun save(observation: Observation): ResponseFlag {
        return if (observation.remoteId.isNullOrEmpty()) {
            //observation doesn't exist on the server, so create it
            create(observation)
        } else {
            //observation should exist on the server, so update it
            update(observation)
        }
    }

    private suspend fun create(observation: Observation): ResponseFlag {
        val response = observationRepository.create(observation)
        return ResponseFlag.processResponse(response)
    }

    private suspend fun update(observation: Observation): ResponseFlag {
        val response = observationRepository.update(observation)
        return ResponseFlag.processResponse(response)
    }

    private suspend fun updateImportant(observation: Observation): ResponseFlag {
        val response = observationRepository.updateImportant(observation)
        return ResponseFlag.processResponse(response)
    }

    private suspend fun updateFavorite(favorite: ObservationFavorite): ResponseFlag {
        val response = observationRepository.updateFavorite(favorite)
        return ResponseFlag.processResponse(response)
    }

    private suspend fun archive(observation: Observation): ResponseFlag {
        val response = observationRepository.archive(observation)
        return ResponseFlag.processArchiveResponse(response)
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, MageApplication.MAGE_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sync_preference_24dp)
            .setContentTitle("Sync Observations")
            .setContentText("Pushing observation updates to MAGE.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()

        return ForegroundInfo(OBSERVATION_SYNC_NOTIFICATION_ID, notification)
    }

    companion object {
        private val LOG_NAME = ObservationSyncWorker::class.java.simpleName

        private const val OBSERVATION_SYNC_WORK = "mil.nga.mage.OBSERVATION_SYNC_WORK"
        private const val OBSERVATION_SYNC_NOTIFICATION_ID = 100

        private val mutex = Mutex()

        fun scheduleWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequest.Builder(ObservationSyncWorker::class.java)
                .setConstraints(constraints)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.SECONDS)
                .build()

            WorkManager
                .getInstance(context)
                .beginUniqueWork(OBSERVATION_SYNC_WORK, ExistingWorkPolicy.REPLACE, request)
                .enqueue()
        }
    }
}