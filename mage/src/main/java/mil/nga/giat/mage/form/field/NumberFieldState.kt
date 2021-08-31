package mil.nga.giat.mage.form.field

import mil.nga.giat.mage.form.FormField
import mil.nga.giat.mage.form.NumberFormField

// TODO multi-form fix bug not allowing/validating decimal numbers
class NumberFieldState(definition: FormField<Number>) :
  FieldState<Number, FieldValue.Number>(
    definition,
    validator = ::isValid,
    errorFor = ::errorMessage,
    hasValue = ::hasValue
  )

private fun errorMessage(definition: FormField<Number>, value: FieldValue.Number?): String {
  return if (value?.number?.isEmpty() == true) {
    "Please enter a value"
  } else if (value?.number?.toDoubleOrNull() == null) {
    "Invalid number"
  } else {
    val number = value.number.toDouble()

    val numberDefinition = definition as? NumberFormField
    if (numberDefinition?.min != null && number < numberDefinition.min.toDouble()) {
      return "Must be greater than or equal to  ${numberDefinition.min}"
    } else if (numberDefinition?.max != null && number > numberDefinition.max.toDouble()) {
      return "Must be less than ${numberDefinition.max}"
    } else "Invalid number"
  }
}

private fun isValid(definition: FormField<Number>, value: FieldValue.Number?): Boolean {
  return if (!definition.required && !hasValue(value)) {
    true
  } else if (definition.required && !hasValue(value)) {
    false
  } else if (value?.number?.toDoubleOrNull() == null) {
    false
  } else {
    val number = value.number.toDouble()

    val numberDefinition = definition as? NumberFormField
    return if (numberDefinition?.min != null && number < numberDefinition.min.toDouble()) {
      false
    } else !(numberDefinition?.max != null && number > numberDefinition.max.toDouble())
  }
}

private fun hasValue(value: FieldValue.Number?): Boolean {
  return value?.number?.isNotEmpty() == true
}