package mil.nga.giat.mage.observation.form

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Description
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import mil.nga.giat.mage.coordinate.CoordinateFormatter
import mil.nga.giat.mage.form.*
import mil.nga.giat.mage.observation.ObservationLocation
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory
import mil.nga.giat.mage.ui.theme.MageTheme
import mil.nga.giat.mage.utils.DateFormatFactory
import java.lang.Exception
import java.util.*
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty

// Used to set content on a view from a Java Activity/Fragment
object FormView {
  fun setContent(view: ComposeView, viewModel: FormViewModel) {
    view.setContent {
      FormsContent(viewModel)
    }
  }
}

@Composable
fun FormsContent(viewModel: FormViewModel = viewModel()) {
  val forms by viewModel.getForms().observeAsState(emptyList())

  MageTheme {
    Column {
      for ((index, form) in forms.withIndex()) {
//        FormContent(formState, index == 0)
      }
    }
  }
}

@Composable
fun FormContent(
  formState: FormState,
  expanded: Boolean
) {
  var expanded by rememberSaveable { mutableStateOf(expanded) }

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

//      if (expanded) {
//        for (fieldModel in form.model.properties) {
//          val fieldDefinition = form.definition.fields.find { it.name == fieldModel.key }
//          if (fieldDefinition != null) {
//            FieldContent(fieldDefinition, fieldModel)
//          }
//        }
//      }
    }
  }
}

@Composable
fun FormHeaderContent(
  formDefinition: Form,
  expanded: Boolean,
  onToggleExpand: () -> Unit
) {
  val angle: Float by animateFloatAsState(
    targetValue = if (expanded) 180F else 0F,
    animationSpec = tween(
      durationMillis = 250,
      easing = FastOutSlowInEasing
    )
  )

  Row(
   modifier = Modifier
      .padding(vertical = 16.dp)
  ) {
    Column(
      Modifier
        .weight(1f)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .padding(bottom = 4.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Description,
          contentDescription = "Form",
          tint = Color(android.graphics.Color.parseColor(formDefinition.hexColor)),
          modifier = Modifier
            .padding(end = 8.dp)
        )

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
          Text(
            AnnotatedString(formDefinition.name).toUpperCase(),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.overline
          )
        }
      }

//      val primaryModel = form.model.properties.find { it.key == form.definition.primaryFeedField }
//      if (primaryModel?.isEmpty == false) {
//        Row {
//          CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
//            Text(
//              fieldText(primaryModel, LocalContext.current),
//              style = MaterialTheme.typography.h6,
//              color = MaterialTheme.colors.primary)
//          }
//        }
//      }
//
//      val secondaryModel = form.model.properties.find { it.key == form.definition.secondaryFeedField }
//      if (secondaryModel?.isEmpty == false) {
//        Row {
//          CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
//            Text(
//              fieldText(secondaryModel, LocalContext.current),
//              style = MaterialTheme.typography.subtitle1
//            )
//          }
//        }
//      }
    }
    Column {
      IconButton(onClick = { onToggleExpand() }) {
        Icon(
          imageVector = Icons.Default.ExpandMore,
          contentDescription = "Expand",
          tint = MaterialTheme.colors.primary,
          modifier = Modifier.rotate(angle)
        )
      }
    }
  }
}

@Composable
fun FieldContent(fieldDefinition: FormField<Any>, fieldModel: ObservationProperty) {
  if (!fieldModel.isEmpty) {
    when (fieldDefinition.type) {
      FieldType.GEOMETRY -> {
        GeometryFieldContent(fieldDefinition as FormField<ObservationLocation>)
      }
      FieldType.DATE -> {
        DateFieldContent(fieldDefinition)
      }
      FieldType.MULTISELECTDROPDOWN -> {
        MultiFieldContent(fieldDefinition as FormField<MultiChoiceFormField>)
      }
      FieldType.CHECKBOX -> {
        BooleanFieldContent(fieldDefinition as FormField<Boolean>)
      }
      else -> {
        StringFieldContent(fieldDefinition, fieldModel)
      }
    }
  }
}

@Composable
fun StringFieldContent(fieldDefinition: FormField<Any>, fieldModel: ObservationProperty) {
  Column(
    Modifier
      .padding(bottom = 16.dp)
  ) {
    Row(
      Modifier
        .padding(bottom = 4.dp)
    ) {

      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        Text(
          fieldDefinition.title, fontSize = 14.sp
        )
      }
    }

    Row {
      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
        Text(
          fieldModel.value.toString(),
          fontSize = 16.sp,
          style = MaterialTheme.typography.body1
        )
      }
    }
  }
}

@Composable
fun BooleanFieldContent(field: FormField<Boolean>) {
  Column(
    Modifier
      .padding(bottom = 16.dp)
  ) {
    Row(
      Modifier
        .padding(bottom = 4.dp)
    ) {
      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        Text(field.title, fontSize = 14.sp)
      }
    }

    Checkbox(
      checked = field.value == true,
      enabled = false,
      onCheckedChange = null
    )
  }
}

@Composable
fun DateFieldContent(field: FormField<Any>) {
  val dateFormat =
    DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), LocalContext.current)

  val text = when (val value = field.value) {
    is String -> {
      try {
        val date = ISO8601DateFormatFactory.ISO8601().parse(value)
        dateFormat.format(date!!)
      } catch (e: Exception) {
        null
      }
    }
    is Date -> {
      dateFormat.format(value)
    }
    else -> null
  }

  if (text?.isNotEmpty() == true) {
    Column(
      Modifier
        .padding(bottom = 16.dp)
    ) {
      Row(
        Modifier
          .padding(bottom = 4.dp)
      ) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
          Text(field.title, fontSize = 14.sp)
        }
      }

      Row {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
          Text(text, fontSize = 16.sp)
        }
      }
    }
  }
}

@Composable
fun MultiFieldContent(field: FormField<MultiChoiceFormField>) {
  val value: List<String>? =
    field.value as? List<String> // TODO why do I have to force cast as List<String>
  if (value != null && field.hasValue()) {
    Column(
      Modifier
        .padding(bottom = 16.dp)
    ) {
      Row(
        Modifier
          .padding(bottom = 4.dp)
      ) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
          Text(field.title, fontSize = 14.sp)
        }
      }

      Row {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
          Text(value.joinToString(", "), fontSize = 16.sp)
        }
      }
    }
  }
}

@Composable
fun GeometryFieldContent(field: FormField<ObservationLocation>) {
  if (field.value != null) {
    val value = CoordinateFormatter(LocalContext.current).format(field.value?.centroidLatLng)

    Column(
      Modifier
        .padding(bottom = 16.dp)
    ) {
      Row(
        Modifier
          .padding(bottom = 4.dp)
      ) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
          Text(field.title, fontSize = 14.sp)
        }
      }

      Row {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
          Text(value, fontSize = 16.sp)
        }
      }
    }
  }
}

fun fieldText(
  fieldModel: ObservationProperty,
  context: Context
): String {
  val dateFormat =
    DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), context)

  return when (val value = fieldModel.value) {
    is ObservationLocation -> {
      CoordinateFormatter(context).format(value.centroidLatLng)
    }
    is Date -> {
      dateFormat.format(value)
    }
    is List<*> -> {
      value.joinToString(", ")
    }
    else -> {
      value.toString()
    }
  }
}