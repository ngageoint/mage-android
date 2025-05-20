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
import mil.nga.giat.mage.data.repository.observation.AttachmentRepository
import mil.nga.giat.mage.data.datasource.observation.AttachmentLocalDataSource
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
         try {
            //submit observations to the server that are marked as "dirty" and track the overall response flag of all transactions
            val overallResponseFlag = syncAttachments()

            //if any of the transactions returned RetryFlag, then Result.retry() will be returned to retry the work request per back off policy
            //this should not result in the successful transactions being resubmitted, as they will no longer be marked as "dirty"
            when (overallResponseFlag) {
               ResponseFlag.SuccessFlag -> Result.success()
               ResponseFlag.FailureFlag -> Result.failure()
               ResponseFlag.RetryFlag -> Result.retry()
            }
         } catch (e: Exception) {
            Log.e(LOG_NAME, "Failed to sync attachments", e)
            //any unhandled exception should result in a retry
            Result.retry()
         }
      }
   }

   private suspend fun syncAttachments(): ResponseFlag {
      var overallResult: ResponseFlag = ResponseFlag.SuccessFlag

      for (attachment in attachmentLocalDataSource.dirtyAttachments.filter { !it.observation.remoteId.isNullOrEmpty() && it.url.isNullOrEmpty() }) {
         val response = ResponseFlag.processResponse(attachmentRepository.syncAttachment(attachment))
         overallResult = ResponseFlag.combineResponseFlags(overallResult, response)
      }

      return overallResult
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

   companion object {
      private val LOG_NAME = AttachmentSyncWorker::class.java.simpleName
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