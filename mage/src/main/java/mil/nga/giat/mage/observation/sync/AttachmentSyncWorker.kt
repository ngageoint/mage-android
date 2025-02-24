package mil.nga.giat.mage.observation.sync

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import androidx.work.ListenableWorker.Result.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mil.nga.giat.mage.MageApplication
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.repository.observation.AttachmentRepository
import mil.nga.giat.mage.data.datasource.observation.AttachmentLocalDataSource
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

@HiltWorker
class AttachmentSyncWorker @AssistedInject constructor(
   @Assisted context: Context,
   @Assisted params: WorkerParameters,
   private val attachmentLocalDataSource: AttachmentLocalDataSource,
   private val attachmentRepository: AttachmentRepository
) : CoroutineWorker(context, params) {

   override suspend fun doWork(): Result {
      // Lock to ensure previous running work will complete when cancelled before new work is started.
      return mutex.withLock {
         val result = try {
            syncAttachments()
         } catch (e: Exception) {
            Log.e(LOG_NAME, "Failed to sync attachments", e)
            RESULT_RETRY_FLAG
         }

         if (result.containsFlag(RESULT_RETRY_FLAG)) retry() else success()
      }
   }

   override suspend fun getForegroundInfo(): ForegroundInfo {
      val notification = NotificationCompat.Builder(applicationContext, MageApplication.MAGE_NOTIFICATION_CHANNEL_ID)
         .setSmallIcon(R.drawable.ic_sync_preference_24dp)
         .setContentTitle("Sync Attachments")
         .setContentText("Pushing attachments to MAGE.")
         .setPriority(NotificationCompat.PRIORITY_MAX)
         .setAutoCancel(true)
         .build()

      return ForegroundInfo(ATTACHMENT_SYNC_NOTIFICATION_ID, notification)
   }

   private suspend fun syncAttachments(): Int {
      var result = RESULT_SUCCESS_FLAG

      for (attachment in attachmentLocalDataSource.dirtyAttachments.filter { !it.observation.remoteId.isNullOrEmpty() && it.url.isNullOrEmpty() }) {
         val response = attachmentRepository.syncAttachment(attachment)
         result = if (response.isSuccessful) {
            success()
         } else {
            if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
               failure()
            } else {
               retry()
            }
         }.withFlag()
      }

      return result
   }

   private fun Result.withFlag(): Int {
      return when {
         this.toString().contains("Failure") -> RESULT_FAILURE_FLAG
         this.toString().contains("Retry") -> RESULT_RETRY_FLAG
         else -> RESULT_SUCCESS_FLAG
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

      private const val ATTACHMENT_SYNC_WORK = "mil.nga.mage.ATTACHMENT_SYNC_WORK"
      private const val ATTACHMENT_SYNC_NOTIFICATION_ID = 200

      private val mutex = Mutex()

      fun scheduleWork(context: Context) {
         val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

         val request = OneTimeWorkRequest.Builder(AttachmentSyncWorker::class.java)
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.SECONDS)
            .build()

         WorkManager
            .getInstance(context)
            .beginUniqueWork(ATTACHMENT_SYNC_WORK, ExistingWorkPolicy.REPLACE, request)
            .enqueue()
      }
   }
}