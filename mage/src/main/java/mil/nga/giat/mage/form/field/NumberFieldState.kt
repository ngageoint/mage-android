package mil.nga.giat.mage.form.field

import mil.nga.giat.mage.form.FormField
import mil.nga.giat.mage.form.NumberFormField

class NumberFieldState(definition: FormField<Number>) :
  FieldState<Number, FieldValue.Number>(
    definition,
    validator = ::isValid,
    errorFor = ::errorMessage,
    hasValue = ::hasValue
  )

private fun errorMessage(definition: FormField<Number>, value: FieldValue.Number?): String {
  return if (value?.number == null) {
    "Please enter a value"
  } else if (value?.number == null) {
    "Invalid number"
  } else {
    val numberDefinition = definition as? NumberFormField
    if (numberDefinition?.min != null && value.number.toDouble() < numberDefinition.min.toDouble()) {
      return "Must be greater than or equal to  ${numberDefinition.min}"
    } else if (numberDefinition?.max != null && value.number.toDouble() > numberDefinition.max.toDouble()) {
      return "Must be less than ${numberDefinition.max}"
    } else "Invalid number"
  }
}

private fun isValid(definition: FormField<Number>, value: FieldValue.Number?): Boolean {
  return if (definition.required && value?.number == null) {
    false
  } else if (value?.number == null) {
    false
  } else {
    val numberDefinition = definition as? NumberFormField

    return if (numberDefinition?.min != null && value.number.toDouble() < numberDefinition.min.toDouble()) {
      false
    } else !(numberDefinition?.max != null && value.number.toDouble() > numberDefinition.max.toDouble())
  }
}

private fun hasValue(value: FieldValue.Number?): Boolean {
  return value?.number != null
}