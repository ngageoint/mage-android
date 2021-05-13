package mil.nga.giat.mage.observation.form

import android.content.Context
import android.util.AttributeSet
import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.*

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import mil.nga.giat.mage.form.*
import mil.nga.giat.mage.ui.theme.MageTheme
import mil.nga.giat.mage.sdk.datastore.observation.Observation
import androidx.compose.runtime.getValue

import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.AbstractComposeView

// TODO multi-form
// Handle required fields
// Validation of fields, don't allow save and show error
// Auto scroll to new form on ad form
// Update primary/secondary for form header if those fields change
// Remove form from observation
// TODO Web is not ordering fields correctly
// Make sure all defaults are applied to new form fields
// need to pass form index in OnClick so we can grab the right field on the right form
// full screen select dialog
// dismiss single select dialog on first selection

sealed class FormFieldEvent {
  data class OnClick(val formId: Long, val fieldName: String) : FormFieldEvent()
}

class FormsComposeView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : AbstractComposeView(context, attrs) {

  var formsState = mutableStateOf(FormsState());

  var viewModel: FormViewModel? = null
    set(value) {
      field = value
      disposeComposition()
    }

  @Composable
  override fun Content() {
    val formDefinitions = viewModel?.getFormDefinitions()!!
    val state = rememberSaveable {
      formsState.value
    }

    FormsEditContent(state)
  }

  fun setObservation(observation: Observation) {
    val formDefinitions = viewModel?.getFormDefinitions()!!
    formsState.value = FormsState.fromObservation(observation, formDefinitions)
  }
}

// Used to set content on a view from a Java Activity/Fragment
object FormEdit {
  private var formsState = mutableStateOf(FormsState())

  fun setContent(
    view: ComposeView,
    viewModel: FormViewModel,
    observation: Observation,
    onClickHandler: ((FormFieldEvent.OnClick) -> Unit)? = null
  ) {
    val formDefinitions = viewModel.getFormDefinitions()
    formsState.value = FormsState.fromNew(formDefinitions)
//    val formsState = FormState.fromObservation(observation, formDefinitions)

    view.setContent {
      val state: FormsState by rememberSaveable { formsState }

      FormsEditContent(
        state,
        onClickHandler = onClickHandler
      )
    }
  }

  fun addForm(
    observation: Observation,
    viewModel: FormViewModel
  ) {
    val formDefinitions = viewModel.getFormDefinitions()
    formsState.value = FormsState.fromObservation(observation, formDefinitions)
  }
}

@Composable
fun FormsEditContent(
//  forms: List<FormState> = rememberSaveable { emptyList<FormState>() },
  formsState: FormsState,
  onClickHandler: ((FormFieldEvent.OnClick) -> Unit)? = null
) {
  MageTheme {
    Column {
      for ((index, formState) in formsState.forms().withIndex()) {
        FormEditContent(
          formState,
          index == 0,
          onEventHandler = onClickHandler
        )
      }
    }
  }
}

@Composable
fun FormEditContent(
  formState: FormState,
  expanded: Boolean,
  onEventHandler: ((FormFieldEvent.OnClick) -> Unit)? = null
) {
  var expanded by remember { mutableStateOf(expanded) }

  Card(
    Modifier
      .fillMaxWidth()
      .padding(8.dp)
  ) {
    Column(
      Modifier
        .padding(horizontal = 16.dp)
        .animateContentSize()
    ) {
      FormHeaderContent(formState.definition, expanded) { expanded = !expanded }

      if (expanded) {
        for (fieldState in formState.fields) {
          val fieldDefinition = formState.definition.fields.find { it.name == fieldState.definition.name }
          if (fieldDefinition != null) {
            FieldEditContent(
              fieldDefinition,
              fieldState,
              onClick = {
                onEventHandler?.invoke(FormFieldEvent.OnClick(formState.definition.id, fieldDefinition.name))
              }
            )
          }
        }
      }
    }
  }
}

@Composable
fun FieldEditContent(
  fieldDefinition: FormField<Any>,
  fieldState: FieldState,
  onClick: (() -> Unit)? = null
) {
  if (fieldDefinition.type == FieldType.TEXTFIELD ||
    fieldDefinition.type == FieldType.TEXTAREA ) {
    TextEdit(fieldState, fieldDefinition)
  } else if (fieldDefinition.type == FieldType.DROPDOWN) {
//    SelectEditContent(fieldDefinition, fieldState, onClick)
  }
}

@Composable
fun TextEdit(
  fieldState: FieldState,
  fieldDefinition: FormField<Any>
) {
  Column(Modifier.padding(bottom = 32.dp)) {
    TextField(
      value = fieldState.value as? String ?: "",
      onValueChange = { fieldState?.value = it },
      label = { Text("${fieldDefinition.title}${if (fieldDefinition.required) " *" else ""}") },
      singleLine = fieldDefinition.type == FieldType.TEXTFIELD,
      modifier = Modifier.fillMaxWidth()
    )
  }
}

//@Composable
//fun SelectEditContent(
//  fieldDefinition: FormField<Any>,
//  fieldState: FieldState,
//  onClick: (() -> Unit)? = null
//) {
//  val model by fieldModel.liveData.observeAsState()
//  val value: String = if (fieldDefinition.type == FieldType.DROPDOWN) {
//    model as? String ?: ""
//  } else {
//    (model as? List<*>)?.joinToString(", ") ?: ""
//  }
//
//  val focusManager = LocalFocusManager.current
//
//  Column(Modifier.padding(bottom = 32.dp)) {
//    TextField(
//      value = value,
//      onValueChange = { },
//      label = { Text("${fieldDefinition.title}${if (fieldDefinition.required) " *" else ""}") },
//      enabled = false,
//      colors = TextFieldDefaults.textFieldColors(
//        disabledTextColor = LocalContentColor.current.copy(LocalContentAlpha.current),
//        disabledLabelColor =  MaterialTheme.colors.onSurface.copy(ContentAlpha.medium)
//      ),
//      modifier = Modifier
//        .fillMaxWidth()
//        .clickable(onClick = {
//          onClick?.invoke()
//          focusManager.clearFocus()
//        })
//    )
//  }
//}

//
//@Composable
//fun BooleanFieldContent(field: FormField<Boolean>) {
//  Column(
//    Modifier
//      .padding(bottom = 16.dp)
//  ) {
//    Row(
//      Modifier
//        .padding(bottom = 4.dp)
//    ) {
//      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
//        Text(field.title, fontSize = 14.sp)
//      }
//    }
//
//    Checkbox(
//      checked = field.value == true,
//      enabled = false,
//      onCheckedChange = null
//    )
//  }
//}
//
//@Composable
//fun DateFieldContent(field: FormField<Any>) {
//  val dateFormat =
//    DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), LocalContext.current)
//
//  val text = when (val value = field.value) {
//    is String -> {
//      try {
//        val date = ISO8601DateFormatFactory.ISO8601().parse(value)
//        dateFormat.format(date!!)
//      } catch (e: Exception) {
//        null
//      }
//    }
//    is Date -> {
//      dateFormat.format(value)
//    }
//    else -> null
//  }
//
//  if (text?.isNotEmpty() == true) {
//    Column(
//      Modifier
//        .padding(bottom = 16.dp)
//    ) {
//      Row(
//        Modifier
//          .padding(bottom = 4.dp)
//      ) {
//        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
//          Text(field.title, fontSize = 14.sp)
//        }
//      }
//
//      Row {
//        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
//          Text(text, fontSize = 16.sp)
//        }
//      }
//    }
//  }
//}
//
//@Composable
//fun MultiFieldContent(field: FormField<MultiChoiceFormField>) {
//  val value: List<String>? =
//    field.value as? List<String> // TODO why do I have to force cast as List<String>
//  if (value != null && field.hasValue()) {
//    Column(
//      Modifier
//        .padding(bottom = 16.dp)
//    ) {
//      Row(
//        Modifier
//          .padding(bottom = 4.dp)
//      ) {
//        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
//          Text(field.title, fontSize = 14.sp)
//        }
//      }
//
//      Row {
//        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
//          Text(value.joinToString(", "), fontSize = 16.sp)
//        }
//      }
//    }
//  }
//}
//
//@Composable
//fun GeometryFieldContent(field: FormField<ObservationLocation>) {
//  if (field.value != null) {
//    val value = CoordinateFormatter(LocalContext.current).format(field.value?.centroidLatLng)
//
//    Column(
//      Modifier
//        .padding(bottom = 16.dp)
//    ) {
//      Row(
//        Modifier
//          .padding(bottom = 4.dp)
//      ) {
//        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
//          Text(field.title, fontSize = 14.sp)
//        }
//      }
//
//      Row {
//        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
//          Text(value, fontSize = 16.sp)
//        }
//      }
//    }
//  }
//}
//
//fun fieldText(
//  fieldModel: ObservationProperty,
//  context: Context
//): String {
//  val dateFormat =
//    DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), context)
//
//  return when (val value = fieldModel.value) {
//    is ObservationLocation -> {
//      CoordinateFormatter(context).format(value.centroidLatLng)
//    }
//    is Date -> {
//      dateFormat.format(value)
//    }
//    is List<*> -> {
//      value.joinToString(", ")
//    }
//    else -> {
//      value.toString()
//    }
//  }
//}