package mil.nga.giat.mage.observation.sync

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.work.*
import dagger.android.AndroidInjection
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import mil.nga.giat.mage.sdk.datastore.observation.AttachmentHelper
import mil.nga.giat.mage.sdk.event.IAttachmentEventListener
import java.util.*
import javax.inject.Inject

class AttachmentPushService : Service(), IAttachmentEventListener {

    @Inject
    lateinit var context: Context

    companion object {
        private val ATTACHMENT_SYNC_WORK = "mil.nga.mage.ATTACHMENT_SYNC_WORK"

        private fun workRequest(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            return OneTimeWorkRequestBuilder<AttachmentSyncWorker>()
                    .setConstraints(constraints)
                    .build()
        }

        fun beginWork(): UUID {
            val request = workRequest()
            WorkManager.getInstance().beginUniqueWork(ATTACHMENT_SYNC_WORK, ExistingWorkPolicy.APPEND, request).enqueue()
            return request.id
        }
    }

    override fun onCreate() {
        super.onCreate()

        AndroidInjection.inject(this)

        AttachmentHelper.getInstance(context).addListener(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Try and sync anything that may not have already been sync'ed
        // This could happen if attachments = were created when the user did not have a token
        if (shouldSync()) {
            beginWork()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        AttachmentHelper.getInstance(context).removeListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onAttachmentUploadable(attachment: Attachment) {
        Log.i("BILLY", "Schedule attachment work")
        beginWork()
    }

    override fun onAttachmentCreated(attachment: Attachment) {
    }

    override fun onAttachmentUpdated(attachment: Attachment) {
    }

    override fun onAttachmentDeleted(attachment: Attachment) {
    }

    override fun onError(error: Throwable?) {
    }

    private fun shouldSync(): Boolean {
        return  AttachmentHelper.getInstance(context).dirtyAttachments.isNotEmpty()
    }
}

