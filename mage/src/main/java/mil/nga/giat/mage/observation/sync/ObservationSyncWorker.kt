package mil.nga.giat.mage.observation.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import mil.nga.giat.mage.data.observation.ObservationRepository
import mil.nga.giat.mage.sdk.datastore.observation.Observation
import mil.nga.giat.mage.sdk.datastore.observation.ObservationFavorite
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper
import mil.nga.giat.mage.sdk.datastore.observation.State
import java.io.IOException
import java.net.HttpURLConnection

@HiltWorker
class ObservationSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observationRepository: ObservationRepository
) : CoroutineWorker(context, params) {

    private val observationHelper = ObservationHelper.getInstance(applicationContext)

    private fun Result.withFlag(flag: Int): Int {
        return when(this) {
            is Result.Failure -> RESULT_FAILURE_FLAG or flag
            is Result.Retry -> RESULT_RETRY_FLAG or flag
            else -> RESULT_SUCCESS_FLAG or flag
        }
    }

    private fun Int.containsFlag(flag: Int): Boolean {
        return (this or flag) == this
    }

    private fun Int.withFlag(flag: Int): Int {
        return this or flag
    }

    companion object {
        private val LOG_NAME = ObservationSyncWorker::class.java.simpleName

        private const val RESULT_SUCCESS_FLAG = 0
        private const val RESULT_FAILURE_FLAG = 1
        private const val RESULT_RETRY_FLAG = 2
    }

    override suspend fun doWork(): Result {
        var result = RESULT_SUCCESS_FLAG

        try {
            result = syncObservations().withFlag(result)
            result = syncObservationImportant().withFlag(result)
            result = syncObservationFavorites().withFlag(result)
        } catch (e: Exception) {
            Log.e(LOG_NAME, "Error trying to sync observations with server", e)
        }

        return if (result.containsFlag(RESULT_RETRY_FLAG)) Result.retry() else Result.success()
    }

    private suspend fun syncObservations(): Int {
        var result = RESULT_SUCCESS_FLAG

        for (observation in observationHelper.dirty) {
            result = syncObservation(observation).withFlag(result)
        }

        return result
    }

    private suspend fun syncObservation(observation: Observation): Int {
        var result = RESULT_SUCCESS_FLAG

        try {
            result = if (observation.state == State.ARCHIVE) {
                archive(observation).withFlag(result)
            } else {
                save(observation).withFlag(result)
            }
        } catch(e: IOException) {
            Log.e(LOG_NAME, "Failed to sync observation with server", e)
            result = RESULT_FAILURE_FLAG
        }

        return result
    }

    private suspend fun syncObservationImportant(): Int {
        var result = RESULT_SUCCESS_FLAG

        for (observation in observationHelper.dirtyImportant) {
            result = updateImportant(observation).withFlag(result)
        }

        return result
    }

    private suspend fun syncObservationFavorites(): Int {
        var result = RESULT_SUCCESS_FLAG

        for (favorite in observationHelper.dirtyFavorites) {
            result = updateFavorite(favorite).withFlag(result)
        }

        return result
    }

    private suspend fun save(observation: Observation): Result {
        return if (observation.remoteId.isNullOrEmpty()) {
            create(observation)
        } else {
            update(observation)
        }
    }

    private suspend fun create(observation: Observation): Result {
        val response = observationRepository.create(observation)
        return if (response.isSuccessful) {
            Result.success()
        } else {
            if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) Result.failure() else Result.retry()
        }
    }

    private suspend fun update(observation: Observation): Result {
        val response = observationRepository.update(observation)
        return if (response.isSuccessful) {
            Result.success()
        } else {
            if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) Result.failure() else Result.retry()
        }
    }

    private suspend fun archive(observation: Observation): Result {
        val response = observationRepository.archive(observation)
        return when {
            response.isSuccessful -> Result.success()
            response.code() == HttpURLConnection.HTTP_NOT_FOUND -> Result.success()
            response.code() == HttpURLConnection.HTTP_UNAUTHORIZED -> Result.failure()
            else -> Result.retry()
        }
    }

    private suspend fun updateImportant(observation: Observation): Result {
        val response = observationRepository.updateImportant(observation)

        return if (response.isSuccessful) {
            Result.success()
        } else {
            if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) Result.failure() else Result.retry()
        }
    }

    private suspend fun updateFavorite(favorite: ObservationFavorite): Result {
        val response = observationRepository.updateFavorite(favorite)
        return if (response.isSuccessful) {
            Result.success()
        } else {
            if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) Result.failure() else Result.retry()
        }
    }
}