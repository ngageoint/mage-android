package mil.nga.giat.mage.form.field

import mil.nga.giat.mage.form.AttachmentFormField
import mil.nga.giat.mage.form.FormField
import mil.nga.giat.mage.database.model.observation.Attachment

class AttachmentFieldState(definition: FormField<Attachment>) :
  FieldState<Attachment, FieldValue.Attachment>(
    definition,
    validator = ::isValid,
    errorFor = ::errorMessage,
    hasValue = ::hasValue
  )

private fun errorMessage(definition: FormField<Attachment>, value: FieldValue.Attachment?): String {
  val size = value?.attachments?.filter { it.action != Media.ATTACHMENT_DELETE_ACTION }?.size ?: 0

  val fieldDefinition = definition as? AttachmentFormField
  return if (fieldDefinition?.min != null && size < fieldDefinition.min.toDouble()) {
    return "Must include at least  ${fieldDefinition.min} ${if (fieldDefinition.min.toInt() > 1) "attachments" else "attachment"}"
  } else if (fieldDefinition?.max != null && size > fieldDefinition.max.toInt()) {
    return "Cannot include more than ${fieldDefinition.max} ${if (fieldDefinition.max.toInt() > 1) "attachments" else "attachment"}"
  } else ""
}

private fun isValid(definition: FormField<Attachment>, value: FieldValue.Attachment?): Boolean {
  val size = value?.attachments?.filter { it.action != Media.ATTACHMENT_DELETE_ACTION }?.size ?: 0

  val fieldDefinition = definition as? AttachmentFormField

  return if (fieldDefinition?.min != null && size < fieldDefinition.min.toInt()) {
    false
  } else !(fieldDefinition?.max != null && size > fieldDefinition.max.toInt())
}

private fun hasValue(value: FieldValue.Attachment?): Boolean {
  return value?.attachments?.any { it.action != Media.ATTACHMENT_DELETE_ACTION } == true
}