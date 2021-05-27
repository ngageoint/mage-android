package mil.nga.giat.mage.form.field

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import mil.nga.giat.mage.form.FormField

open class FieldState<F, T : FieldValue> (
  val definition: FormField<F>,
  private val validator: (FormField<F>, T?) -> Boolean = { _: FormField<F>, _: T? -> true },
  private val errorFor: (FormField<F>, T?) -> String = { _: FormField<F>, _: T? -> "" },
  private val hasValue: (T?) -> Boolean = { _: T? -> false }
) {
  var answer by mutableStateOf<T?>(null)

  var isFocusedDirty: Boolean by mutableStateOf(false)
  var isFocused: Boolean by mutableStateOf(false)
  private var displayErrors: Boolean by mutableStateOf(false)

  open val isValid: Boolean
    get() = validator(definition, answer)

  fun onFocusChange(focused: Boolean) {
    isFocused = focused
    if (focused) isFocusedDirty = true
  }

  fun enableShowErrors() {
    // Only show errors if the text was focused at least once
    if (isFocusedDirty) {
      displayErrors = true
    }
  }

  fun showErrors(): Boolean {
    return !isValid && displayErrors
  }

  fun hasValue(): Boolean {
    return hasValue(answer)
  }

  open fun getError(): String? {
    return if (showErrors()) {
      errorFor(definition, answer)
    } else {
      null
    }
  }
}