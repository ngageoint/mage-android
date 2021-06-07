package mil.nga.giat.mage.form

import androidx.compose.runtime.mutableStateOf
import mil.nga.giat.mage.form.field.FieldState
import mil.nga.giat.mage.form.field.FieldValue

class FormState(
  val id: Long? = null,
  val eventId: String,
  val definition: Form,
  val fields: List<FieldState<*, *>>,
  expanded: Boolean = false,
) {
  val expanded = mutableStateOf(expanded)

  fun isValid(): Boolean {
    var valid = true
    for (fieldState in fields) {
      if (!fieldState.isValid) {
        fieldState.isFocusedDirty = true
        fieldState.enableShowErrors()
        valid = false
      }
    }

    return valid
  }

  companion object {
    fun fromForm(id: Long? = null, eventId: String, form: Form, defaultForm: Form? = null): FormState {
      val defaultFields = defaultForm?.fields?.associateTo(mutableMapOf()) {
        it.name to it.value
      }

      val fields = mutableListOf<FieldState<*, out FieldValue>>()
      for (field in form.fields) {
        val fieldState = FieldState.fromFormField(field, field.value, defaultFields?.get(field.name))
        fields.add(fieldState)
      }

      return FormState(id, eventId, form, fields)
    }
  }
}