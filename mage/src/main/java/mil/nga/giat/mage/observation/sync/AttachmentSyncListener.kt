package mil.nga.giat.mage.observation.sync

import mil.nga.giat.mage.database.model.observation.Attachment
import mil.nga.giat.mage.data.datasource.observation.AttachmentLocalDataSource
import mil.nga.giat.mage.sdk.event.IAttachmentEventListener
import javax.inject.Inject

class AttachmentSyncListener(
   attachmentLocalDataSource: AttachmentLocalDataSource,
   val sync : () -> Unit
): IAttachmentEventListener {

   init {
      attachmentLocalDataSource.addListener(this)
      sync()
   }

   override fun onAttachmentUploadable(attachment: Attachment) {
      sync()
   }

   override fun onAttachmentCreated(attachment: Attachment?) {}
   override fun onAttachmentUpdated(attachment: Attachment?) {}
   override fun onAttachmentDeleted(attachment: Attachment?) {}
   override fun onError(error: Throwable?) {}
}