package mil.nga.giat.mage.form.field

import mil.nga.giat.mage.form.FormField

class MultiSelectFieldState(definition: FormField<List<String>>) :
  FieldState<List<String>, FieldValue.Multi>(
    definition,
    validator = ::isValid,
    errorFor = ::errorMessage,
    hasValue = ::hasValue
  )

private fun errorMessage(definition: FormField<List<String>>, value: FieldValue.Multi?): String {
  return "Please enter a value"
}

private fun isValid(definition: FormField<List<String>>, value: FieldValue.Multi?): Boolean {
  return !definition.required || value?.choices?.isNotEmpty() == true
}

private fun hasValue(value: FieldValue.Multi?): Boolean {
  return value?.choices?.isNotEmpty() == true
}