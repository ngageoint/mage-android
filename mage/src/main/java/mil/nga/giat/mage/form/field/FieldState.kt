package mil.nga.giat.mage.form.field

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import mil.nga.giat.mage.form.*
import mil.nga.giat.mage.observation.ObservationLocation
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import mil.nga.giat.mage.sdk.utils.GeometryUtility

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

  companion object {
    fun fromFormField(
      fieldDefinition: FormField<Any>,
      value: Any?,
      default: Any? = null
    ): FieldState<*, out FieldValue> {
      return when (fieldDefinition.type) {
        FieldType.ATTACHMENT -> {
          val fieldState = AttachmentFieldState(fieldDefinition as AttachmentFormField)
          val attachments = value as? List<Attachment>
          if (attachments != null) {
            fieldState.answer = FieldValue.Attachment(attachments)
          }
          fieldState
        }
        FieldType.CHECKBOX -> {
          val fieldState = BooleanFieldState(fieldDefinition as BooleanFormField)
          val boolean = default as? Boolean ?: value as? Boolean
          if (boolean != null) {
            fieldState.answer = FieldValue.Boolean(boolean)
          }
          fieldState
        }
        FieldType.DATE -> {
          val fieldState = DateFieldState(fieldDefinition as DateFormField)
          val date = FieldValue.Date.parseValue(default ?: value)
          if (date != null) {
            fieldState.answer = FieldValue.Date(date)
          }
          fieldState
        }
        FieldType.DROPDOWN -> {
          val fieldState = SelectFieldState(fieldDefinition as SingleChoiceFormField)
          val text = default as? String ?: value as? String
          if (text != null) {
            fieldState.answer = FieldValue.Text(text)
          }
          fieldState
        }
        FieldType.EMAIL -> {
          val fieldState = EmailFieldState(fieldDefinition as TextFormField)
          val email = default as? String ?: value as? String
          if (email != null) {
            fieldState.answer = FieldValue.Text(email)
          }
          fieldState
        }
        FieldType.GEOMETRY -> {
          val fieldState = GeometryFieldState(fieldDefinition as GeometryFormField)

          val location = if (default != null) {
            if (default is ByteArray) {
              ObservationLocation(GeometryUtility.toGeometry(default))
            } else {
              default as? ObservationLocation
            }
          } else {
            if (value is ByteArray) {
              ObservationLocation(GeometryUtility.toGeometry(value))
            } else {
              value as? ObservationLocation
            }
          }

          if (location != null) {
            fieldState.answer = FieldValue.Location(location)
          }
          fieldState
        }
        FieldType.MULTISELECTDROPDOWN -> {
          val fieldState = MultiSelectFieldState(fieldDefinition as MultiChoiceFormField)
          val choices = default as? Collection<*> ?: value as? Collection<*>
          if (choices != null) {
            fieldState.answer = FieldValue.Multi(choices.map { it.toString() })
          }
          fieldState
        }
        FieldType.NUMBERFIELD -> {
          val fieldState = NumberFieldState(fieldDefinition as NumberFormField)
          val number = default?.toString() ?: value?.toString()
          if (number != null) {
            fieldState.answer = FieldValue.Number(number)
          }
          fieldState
        }
        FieldType.RADIO -> {
          val fieldState = RadioFieldState(fieldDefinition as SingleChoiceFormField)
          val text = default as? String ?: value as? String
          if (text != null) {
            fieldState.answer = FieldValue.Text(text)
          }
          fieldState
        }
        FieldType.TEXTAREA, FieldType.TEXTFIELD, FieldType.PASSWORD -> {
          val fieldState = TextFieldState(fieldDefinition as TextFormField)
          val text = default as? String ?: value as? String
          if (text != null) {
            fieldState.answer = FieldValue.Text(text)
          }
          fieldState
        }
      }
    }
  }
}