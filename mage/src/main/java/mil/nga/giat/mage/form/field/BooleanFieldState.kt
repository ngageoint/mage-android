package mil.nga.giat.mage.form.field

import mil.nga.giat.mage.form.FormField

class BooleanFieldState(definition: FormField<Boolean>) :
  FieldState<Boolean, FieldValue.Boolean>(
    definition,
    validator = ::isValid,
    errorFor = ::errorMessage,
    hasValue = ::hasValue
  )

private fun errorMessage(definition: FormField<Boolean>, value: FieldValue.Boolean?): String {
  return "${definition.title} is required"
}

private fun isValid(definition: FormField<Boolean>, value: FieldValue.Boolean?): Boolean {
  return !definition.required || value?.boolean == true
}

private fun hasValue(value: FieldValue.Boolean?): Boolean {
  return true
}