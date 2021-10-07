package mil.nga.giat.mage.form.field

import android.util.Patterns
import mil.nga.giat.mage.form.FormField

class EmailFieldState(definition: FormField<String>) :
  FieldState<String, FieldValue.Text>(
    definition,
    validator = ::isValid,
    errorFor = ::errorMessage,
    hasValue = ::hasValue
  )

private fun errorMessage(definition: FormField<String>, value: FieldValue.Text?): String {
  return if (value?.text.isNullOrBlank()) {
    "Please enter a value"
  } else {
    "Invalid email address"
  }
}

private fun isValid(definition: FormField<String>, value: FieldValue.Text?): Boolean {
  return if (!definition.required && !hasValue(value)) {
    true
  } else if (definition.required && value?.text?.isBlank() == true) {
    false
  } else value != null && Patterns.EMAIL_ADDRESS.matcher(value.text).matches()
}

private fun hasValue(value: FieldValue.Text?): Boolean {
  return value?.text?.isNotEmpty() == true
}