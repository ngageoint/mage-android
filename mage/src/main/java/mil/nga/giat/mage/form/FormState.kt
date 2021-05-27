package mil.nga.giat.mage.form

import androidx.compose.runtime.mutableStateOf
import mil.nga.giat.mage.form.field.FieldState

class FormState(val eventId: String, val definition: Form, val fields: List<FieldState<*, *>>) {
  val expanded = mutableStateOf(false)
}