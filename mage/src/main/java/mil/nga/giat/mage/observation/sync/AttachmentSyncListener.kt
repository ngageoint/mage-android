package mil.nga.giat.mage.observation.sync

import android.content.Context
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import mil.nga.giat.mage.sdk.datastore.observation.AttachmentHelper
import mil.nga.giat.mage.sdk.event.IAttachmentEventListener

class AttachmentSyncListener(
   val context: Context,
   val sync : () -> Unit
): IAttachmentEventListener {

   init {
      AttachmentHelper.getInstance(context).addListener(this)
      sync()
   }

   override fun onAttachmentUploadable(attachment: Attachment?) {
      sync()
   }

   override fun onAttachmentCreated(attachment: Attachment?) {}
   override fun onAttachmentUpdated(attachment: Attachment?) {}
   override fun onAttachmentDeleted(attachment: Attachment?) {}
   override fun onError(error: Throwable?) {}
}