package mil.nga.giat.mage.form.field

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import mil.nga.giat.mage.form.FormField

class TextFieldState(definition: FormField<String>) :
  FieldState<String, FieldValue.Text>(
    definition,
    validator = ::isValid,
    errorFor = ::errorMessage,
    hasValue = ::hasValue
  ) {

  private val undoStack = ArrayDeque<String>()
  private val redoStack = ArrayDeque<String>()

  var canUndo by mutableStateOf(false)
    private set
  var canRedo by mutableStateOf(false)
    private set

  var isTypingActive by mutableStateOf(false)

  fun pushHistory(previous: String) {
    undoStack.addLast(previous)
    redoStack.clear()
    canUndo = true
    canRedo = false
  }

  fun undo() {
    if (undoStack.isEmpty()) return
    val previous = undoStack.removeLast()
    redoStack.addLast(answer?.text ?: "")
    answer = FieldValue.Text(previous)
    canUndo = undoStack.isNotEmpty()
    canRedo = true
  }

  fun redo() {
    if (redoStack.isEmpty()) return
    val next = redoStack.removeLast()
    undoStack.addLast(answer?.text ?: "")
    answer = FieldValue.Text(next)
    canUndo = true
    canRedo = redoStack.isNotEmpty()
  }
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