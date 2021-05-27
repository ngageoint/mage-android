package mil.nga.giat.mage.form.field

import mil.nga.giat.mage.form.FormField

class TextFieldState(definition: FormField<String>) :
  FieldState<String, FieldValue.Text>(
    definition,
    validator = ::isValid,
    errorFor = ::errorMessage,
    hasValue = ::hasValue
  )

private fun errorMessage(definition: FormField<String>, value: FieldValue.Text?): String {
  return "Please enter a value"
}

private fun isValid(definition: FormField<String>, value: FieldValue.Text?): Boolean {
  return !definition.required || value?.text?.isNotEmpty() == true
}

private fun hasValue(value: FieldValue.Text?): Boolean {
  return value?.text?.isNotEmpty() == true
}