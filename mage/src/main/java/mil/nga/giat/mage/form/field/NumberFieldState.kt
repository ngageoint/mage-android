@file:OptIn(ExperimentalFoundationApi::class)

package mil.nga.giat.mage.form.field

import androidx.compose.foundation.ExperimentalFoundationApi
import mil.nga.giat.mage.form.FormField
import mil.nga.giat.mage.form.NumberFormField

import androidx.compose.foundation.text.input.TextFieldState as InputState

// Matches strings that are valid partial decimal input: "-", "1.", "-1.", etc.
private val PARTIAL_NUMBER_REGEX = Regex("^-?\\d*\\.?\\d*$")

class NumberFieldState(definition: FormField<Number>) :
  FieldState<Number, FieldValue.Number>(
    definition,
    validator = ::isValid,
    errorFor = ::errorMessage,
    hasValue = ::hasValue
  ) {
  val inputState: InputState by lazy { InputState(initialText = answer?.number ?: "") }
}

private fun errorMessage(definition: FormField<Number>, value: FieldValue.Number?): String {
  val text = value?.number ?: ""
  return if (text.isEmpty()) {
    "Please enter a value"
  } else if (PARTIAL_NUMBER_REGEX.matches(text) && text.toDoubleOrNull() == null) {
    // Still typing (e.g. "-" or "1.") — no error yet
    ""
  } else {
    val number = text.toDoubleOrNull() ?: return "Invalid number"
    val numberDefinition = definition as? NumberFormField
    if (numberDefinition?.min != null && number < numberDefinition.min.toDouble()) {
      "Must be greater than or equal to ${numberDefinition.min}"
    } else if (numberDefinition?.max != null && number > numberDefinition.max.toDouble()) {
      "Must be less than ${numberDefinition.max}"
    } else "Invalid number"
  }
}

private fun isValid(definition: FormField<Number>, value: FieldValue.Number?): Boolean {
  val text = value?.number ?: ""
  return if (!definition.required && !hasValue(value)) {
    true
  } else if (definition.required && !hasValue(value)) {
    false
  } else if (PARTIAL_NUMBER_REGEX.matches(text) && text.toDoubleOrNull() == null) {
    // Partial input ("-", "1.", "-1.") — allow it through while user is still typing
    true
  } else {
    val number = text.toDoubleOrNull() ?: return false
    val numberDefinition = definition as? NumberFormField
    if (numberDefinition?.min != null && number < numberDefinition.min.toDouble()) {
      false
    } else !(numberDefinition?.max != null && number > numberDefinition.max.toDouble())
  }
}

private fun hasValue(value: FieldValue.Number?): Boolean {
  return value?.number?.isNotEmpty() == true
}