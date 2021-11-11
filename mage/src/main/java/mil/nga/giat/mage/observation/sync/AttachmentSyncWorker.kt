package mil.nga.giat.mage.observation.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import mil.nga.giat.mage.data.observation.AttachmentRepository
import mil.nga.giat.mage.sdk.datastore.observation.AttachmentHelper
import java.io.IOException
import java.net.HttpURLConnection

@HiltWorker
class AttachmentSyncWorker @AssistedInject constructor(
   @Assisted context: Context,
   @Assisted params: WorkerParameters,
   private val attachmentRepository: AttachmentRepository
) : CoroutineWorker(context, params) {

   private fun Result.withFlag(flag: Int): Int {
      return when (this) {
         is Result.Success -> RESULT_FAILURE_FLAG or flag
         is Result.Retry -> RESULT_RETRY_FLAG or flag
         else -> RESULT_SUCCESS_FLAG or flag
      }
   }

   private fun Int.containsFlag(flag: Int): Boolean {
      return (this or flag) == this
   }

   companion object {
      private val LOG_NAME = AttachmentSyncWorker::class.java.simpleName

      private const val RESULT_SUCCESS_FLAG = 0
      private const val RESULT_FAILURE_FLAG = 1
      private const val RESULT_RETRY_FLAG = 2
   }

   override suspend fun doWork(): Result {
      var result = RESULT_SUCCESS_FLAG

      try {
         result = syncAttachments()
      } catch (e: Exception) {
         Log.e(LOG_NAME, "Failed to sync attachments", e)
      }

      return if (result.containsFlag(RESULT_RETRY_FLAG)) {
         Result.retry()
      } else {
         Result.success()
      }
   }

   private suspend fun syncAttachments(): Int {
      var result = RESULT_SUCCESS_FLAG

      val attachmentHelper = AttachmentHelper.getInstance(applicationContext)
      for (attachment in attachmentHelper.dirtyAttachments.filter { !it.observation.remoteId.isNullOrEmpty() && it.url.isNullOrEmpty() }) {
         try {
            val response = attachmentRepository.syncAttachment(attachment)
            result = if (response.isSuccessful) {
               Result.success()
            } else {
               if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                  Result.failure()
               } else {
                  Result.retry()
               }
            }.withFlag(result)
         } catch (e: IOException) {
            Log.e(LOG_NAME, "Failed to sync attachment", e)
            Result.failure().withFlag(result)
         }
      }

      return result
   }
}