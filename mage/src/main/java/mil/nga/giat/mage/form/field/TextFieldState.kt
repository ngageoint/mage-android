@file:OptIn(ExperimentalFoundationApi::class)

package mil.nga.giat.mage.form.field

import androidx.compose.foundation.ExperimentalFoundationApi
import mil.nga.giat.mage.form.FormField

// state of undo/redo
import androidx.compose.foundation.text.input.TextFieldState as InputState

class TextFieldState(definition: FormField<String>) :
  FieldState<String, FieldValue.Text>(
    definition,
    validator = ::isValid,
    errorFor = ::errorMessage,
    hasValue = ::hasValue
  ) {

  val inputState: InputState by lazy { InputState(initialText = answer?.text ?: "") }
}

private fun errorMessage(definition: FormField<String>, value: FieldValue.Text?): String {
  return "Please enter a value"
}

private fun isValid(definition: FormField<String>, value: FieldValue.Text?): Boolean {
  return !definition.required || value?.text?.isNotEmpty() == true
}

private fun hasValue(value: FieldValue.Text?): Boolean {
  return value?.text?.isNotEmpty() == true
}