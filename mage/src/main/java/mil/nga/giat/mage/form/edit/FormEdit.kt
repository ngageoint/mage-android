package mil.nga.giat.mage.form.edit

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*

import androidx.compose.material.*

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mil.nga.giat.mage.form.*

import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import mil.nga.giat.mage.form.field.*
import mil.nga.giat.mage.form.FormState
import mil.nga.giat.mage.form.view.FormHeaderContent
import mil.nga.giat.mage.utils.DateFormatFactory
import java.util.*

// TODO multi-form
// Check required fields for all types
// Auto scroll to new form on add form, TODO test with compose beta8
// Compat with server version 5.x

@Composable
fun FormEditContent(
  formState: FormState,
  onFormDelete: (() -> Unit)? = null,
  onFieldClick: ((FieldState<*, *>) -> Unit)? = null
) {
  Card(
    Modifier.fillMaxWidth()
  ) {
    Column(
      Modifier
        .animateContentSize()
        .padding(horizontal = 16.dp)
    ) {
      FormHeaderContent(formState) { formState.expanded.value = it }

      if (formState.expanded.value) {
        for (fieldState in formState.fields.sortedBy { it.definition.id }) {
          FieldEditContent(
            fieldState,
            onClick = {
              onFieldClick?.invoke(fieldState)
            }
          )
        }

        Divider()

        Row(
          horizontalArrangement = Arrangement.End,
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
        ) {
          TextButton(
            onClick = { onFormDelete?.invoke() }
          ) {
            Text("DELETE FORM", color = MaterialTheme.colors.error)
          }
        }
      }
    }
  }
}

@Composable
fun FieldEditContent(
  fieldState: FieldState<*, out FieldValue>,
  onClick: (() -> Unit)? = null
) {
  when (fieldState.definition.type) {
    FieldType.CHECKBOX -> {
      CheckboxEdit(fieldState as BooleanFieldState) {
        fieldState.answer = FieldValue.Boolean(it)
      }
    }
    FieldType.DATE -> {
      DateEdit(fieldState as DateFieldState, onClick)
    }
    FieldType.DROPDOWN -> {
      SelectEdit(fieldState as SelectFieldState, onClick)
    }
    FieldType.EMAIL -> {
      TextEdit(fieldState as EmailFieldState) {
        fieldState.answer = FieldValue.Text(it)
      }
    }
    FieldType.GEOMETRY -> {
      GeometryEdit(fieldState as GeometryFieldState, onClick = onClick)
    }
    FieldType.MULTISELECTDROPDOWN -> {
      MultiSelectEdit(fieldState as MultiSelectFieldState, onClick)
    }
    FieldType.NUMBERFIELD -> {
      NumberEdit(fieldState as NumberFieldState) {
        fieldState.answer = FieldValue.Number(it)
      }
    }
    FieldType.RADIO -> {
      RadioEdit(fieldState as RadioFieldState) {
        fieldState.answer = FieldValue.Text(it)
      }
    }
    FieldType.TEXTAREA, FieldType.TEXTFIELD -> {
      TextEdit(fieldState as TextFieldState) {
        fieldState.answer = FieldValue.Text(it)
      }
    }
  }
}

@Composable
fun CheckboxEdit(
  fieldState: BooleanFieldState,
  onAnswer: (Boolean) -> Unit,
) {
  val value = fieldState.answer?.boolean == true

  Row(
    Modifier
      .padding(bottom = 16.dp)
      .fillMaxWidth()) {
    Checkbox(
      checked = value,
      onCheckedChange = onAnswer,
      colors = CheckboxDefaults.colors(MaterialTheme.colors.primary),
      modifier = Modifier
        .padding(end = 8.dp)
    )

    Text(text = fieldState.definition.title)
  }

  fieldState.getError()?.let { error -> TextFieldError(textError = error) }
}


@Composable
fun DateEdit(
  fieldState: DateFieldState,
  onClick: (() -> Unit)? = null
) {
  val dateFormat = DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), LocalContext.current)
  val date = fieldState.answer?.date

  val focusManager = LocalFocusManager.current
  val labelColor = if (fieldState.showErrors()) MaterialTheme.colors.error else MaterialTheme.colors.onSurface.copy(ContentAlpha.medium)

  Column(Modifier.padding(bottom = 16.dp)) {
    TextField(
      value = if (date != null) dateFormat.format(date) else "",
      onValueChange = { },
      label = { Text("${fieldState.definition.title}${if (fieldState.definition.required) " *" else ""}") },
      enabled = false,
      isError = fieldState.showErrors(),
      colors = TextFieldDefaults.textFieldColors(
        disabledTextColor = LocalContentColor.current.copy(LocalContentAlpha.current),
        disabledLabelColor = labelColor
      ),
      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = {
          onClick?.invoke()
          focusManager.clearFocus()
          fieldState.onFocusChange(true)
          fieldState.enableShowErrors()
        })
    )

    fieldState.getError()?.let { error -> TextFieldError(textError = error) }
  }
}

@Composable
fun TextEdit(
  fieldState: FieldState<String, out FieldValue.Text>,
  onAnswer: (String) -> Unit,
) {
  val keyboardType = if (fieldState.definition.type == FieldType.EMAIL) {
    KeyboardType.Email
  } else {
    KeyboardType.Text
  }

  Column(Modifier.padding(bottom = 16.dp)) {
    TextField(
      value = fieldState.answer?.text ?: "",
      onValueChange = onAnswer,
      label = { Text("${fieldState.definition.title}${if (fieldState.definition.required) " *" else ""}") },
      singleLine = fieldState.definition.type != FieldType.TEXTAREA,
      isError = fieldState.showErrors(),
      modifier = Modifier
        .fillMaxWidth()
        .onFocusChanged { focusState ->
          val focused = focusState.isFocused
          fieldState.onFocusChange(focused)
          if (!focused) {
            fieldState.enableShowErrors()
          }
        },
      keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )

    fieldState.getError()?.let { error -> TextFieldError(textError = error) }
  }
}

@Composable
fun NumberEdit(
  fieldState: FieldState<Number, FieldValue.Number>,
  onAnswer: (String) -> Unit,
) {
  Column(Modifier.padding(bottom = 16.dp)) {
    TextField(
      value = fieldState.answer?.number?.toString() ?: "",
      onValueChange = { onAnswer(it) },
      label = { Text("${fieldState.definition.title}${if (fieldState.definition.required) " *" else ""}") },
      singleLine = fieldState.definition.type != FieldType.TEXTAREA,
      isError = fieldState.showErrors(),
      modifier = Modifier
        .fillMaxWidth()
        .onFocusChanged { focusState ->
          val focused = focusState.isFocused
          fieldState.onFocusChange(focused)
          if (!focused) {
            fieldState.enableShowErrors()
          }
        },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )

    fieldState.getError()?.let { error -> TextFieldError(textError = error) }
  }
}

@Composable
fun MultiSelectEdit(
  fieldState: MultiSelectFieldState,
  onClick: (() -> Unit)? = null
) {
  val focusManager = LocalFocusManager.current
  val labelColor = if (fieldState.showErrors()) MaterialTheme.colors.error else MaterialTheme.colors.onSurface.copy(ContentAlpha.medium)

  Column(Modifier.padding(bottom = 16.dp)) {
    TextField(
      value = fieldState.answer?.choices?.joinToString(", ") ?: "",
      onValueChange = { },
      label = { Text("${fieldState.definition.title}${if (fieldState.definition.required) " *" else ""}") },
      enabled = false,
      isError = fieldState.showErrors(),
      colors = TextFieldDefaults.textFieldColors(
        disabledTextColor = LocalContentColor.current.copy(LocalContentAlpha.current),
        disabledLabelColor =  labelColor
      ),

      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = {
          onClick?.invoke()
          focusManager.clearFocus()
          fieldState.onFocusChange(true)
          fieldState.enableShowErrors()
        })
    )

    fieldState.getError()?.let { error -> TextFieldError(textError = error) }
  }
}

@Composable
fun SelectEdit(
  fieldState: SelectFieldState,
  onClick: (() -> Unit)? = null
) {
  val focusManager = LocalFocusManager.current
  val labelColor = if (fieldState.showErrors()) MaterialTheme.colors.error else MaterialTheme.colors.onSurface.copy(ContentAlpha.medium)

  Column(Modifier.padding(bottom = 16.dp)) {
    TextField(
      value = fieldState.answer?.text ?: "",
      onValueChange = { },
      label = { Text("${fieldState.definition.title}${if (fieldState.definition.required) " *" else ""}") },
      enabled = false,
      isError = fieldState.showErrors(),
      colors = TextFieldDefaults.textFieldColors(
        disabledTextColor = LocalContentColor.current.copy(LocalContentAlpha.current),
        disabledLabelColor =  labelColor//MaterialTheme.colors.onSurface.copy(ContentAlpha.medium)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = {
          onClick?.invoke()
          focusManager.clearFocus()
          fieldState.onFocusChange(true)
          fieldState.enableShowErrors()
        })
    )

    fieldState.getError()?.let { error -> TextFieldError(textError = error) }
  }
}

@Composable
fun RadioEdit(
  fieldState: RadioFieldState,
  onAnswer: (String) -> Unit,
) {
  val definition = fieldState.definition as SingleChoiceFormField

  Column(Modifier.padding(bottom = 16.dp)) {
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
      Text(
        text = "${fieldState.definition.title}${if (fieldState.definition.required) " *" else ""}",
        modifier = Modifier
          .padding(bottom = 16.dp)
      )
    }

    definition.choices.forEach { choice ->
      Row(
        Modifier
          .padding(bottom = 8.dp)
          .fillMaxWidth()) {

        RadioButton(
          selected = (fieldState.answer?.text == choice.title),
          onClick = { onAnswer.invoke(choice.title) },
          colors = RadioButtonDefaults.colors(MaterialTheme.colors.primary),
          modifier = Modifier
            .padding(end = 8.dp)
        )

        Text(text = choice.title)
      }
    }
  }
}

/**
 * To be removed when [TextField]s support error
 */
@Composable
fun TextFieldError(textError: String) {
  Row(modifier = Modifier.fillMaxWidth()) {
    Spacer(modifier = Modifier.width(16.dp))

    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
      Text(
        text = textError,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.error)
      )
    }
  }
}