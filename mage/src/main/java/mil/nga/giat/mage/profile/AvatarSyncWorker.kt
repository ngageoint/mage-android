package mil.nga.giat.mage.profile

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import mil.nga.giat.mage.data.repository.user.UserRepository
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import java.util.concurrent.TimeUnit

@HiltWorker
class AvatarSyncWorker @AssistedInject constructor(
   @Assisted context: Context,
   @Assisted params: WorkerParameters,
   private val userRepository: UserRepository,
   private val userLocalDataSource: UserLocalDataSource
) : CoroutineWorker(context, params) {

   override suspend fun doWork(): Result {
      return try {
         userLocalDataSource.readCurrentUser()?.let { user->
            userRepository.syncAvatar(user.avatarPath)
         }
         Result.success()
      } catch (e: Exception) {
         Log.e(LOG_NAME, "Failed to sync user avatar", e)
         Result.retry()
      }
   }

   companion object {
      private val LOG_NAME = AvatarSyncWorker::class.java.simpleName

      private const val AVATAR_SYNC_WORK = "mil.nga.mage.AVATAR_SYNC_WORK"

      fun scheduleWork(context: Context) {
         val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

         val request = OneTimeWorkRequest.Builder(AvatarSyncWorker::class.java)
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.SECONDS)
            .build()

         WorkManager
            .getInstance(context)
            .beginUniqueWork(AVATAR_SYNC_WORK, ExistingWorkPolicy.KEEP, request)
            .enqueue()
      }
   }
}