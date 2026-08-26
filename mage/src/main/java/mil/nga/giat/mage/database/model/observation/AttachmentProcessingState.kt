package mil.nga.giat.mage.database.model.observation

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
