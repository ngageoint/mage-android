package mil.nga.giat.mage.database.model.observation

import java.io.File

enum class AttachmentProcessingState {
   UPLOADING,
   PENDING,
   FAILED,
   READY
}

val Attachment.processingState: AttachmentProcessingState
   get() = when {
      isDirty && processingStatus == null -> AttachmentProcessingState.UPLOADING
      processingStatus == "pending" -> AttachmentProcessingState.PENDING
      processingStatus == "rejected" || processingStatus == "error" -> AttachmentProcessingState.FAILED
      else -> AttachmentProcessingState.READY
   }

val Attachment.hasLocalFile: Boolean
   get() = localPath?.let { File(it).exists() } == true
