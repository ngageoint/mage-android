package mil.nga.giat.mage.form.field

import mil.nga.giat.mage.form.FormField
import java.util.*

class DateFieldState(definition: FormField<Date>) :
  FieldState<Date, FieldValue.Date>(
    definition,
    validator = ::isValid,
    errorFor = ::errorMessage,
    hasValue = ::hasValue
  )

private fun errorMessage(definition: FormField<Date>, value: FieldValue.Date?): String {
  return "Please enter a value"
}

private fun isValid(definition: FormField<Date>, value: FieldValue.Date?): Boolean {
  return !definition.required || value != null
}

private fun hasValue(value: FieldValue.Date?): Boolean {
  return value?.date != null
}