package mil.nga.giat.mage.form.view

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mil.nga.giat.mage.coordinate.CoordinateFormatter
import mil.nga.giat.mage.form.field.*
import mil.nga.giat.mage.form.FormState
import mil.nga.giat.mage.utils.DateFormatFactory
import java.util.*

@Composable
fun FormContent(
  formState: FormState
) {
  Card(
    Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp)
  ) {
    Column(
      Modifier
        .padding(horizontal = 16.dp)
        .animateContentSize()
    ) {
      FormHeaderContent(formState) { formState.expanded.value = it }

      if (formState.expanded.value) {
        for (fieldState in formState.fields.sortedBy { it.definition.id }) {
          FieldContent(fieldState)
        }
      }
    }
  }
}

@Composable
fun FormHeaderContent(
  formState: FormState,
  onToggleExpand: (Boolean) -> Unit
) {
  val expanded by formState.expanded

  val angle: Float by animateFloatAsState(
    targetValue = if (expanded) 180F else 0F,
    animationSpec = tween(
      durationMillis = 250,
      easing = FastOutSlowInEasing
    )
  )

  Row {
    Column(
      Modifier
        .weight(1f)
        .padding(vertical = 16.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .padding(bottom = 4.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Description,
          contentDescription = "Form",
          tint = Color(android.graphics.Color.parseColor(formState.definition.hexColor)),
          modifier = Modifier
            .padding(end = 8.dp)
        )

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
          Text(
            formState.definition.name.toUpperCase(Locale.ROOT),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.overline
          )
        }
      }

      FormHeaderContent(formState)
    }
    Column(Modifier.padding(top = 8.dp)) {
      IconButton(onClick = { onToggleExpand(!expanded) }) {
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
fun FormHeaderContent(
  formState: FormState?
) {
  val primaryState = formState?.fields?.find { it.definition.name == formState.definition.primaryFeedField }
  if (primaryState?.hasValue() == true) {
    Row {
      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
        Text(
          fieldText(primaryState.answer, LocalContext.current),
          style = MaterialTheme.typography.h6,
          color = MaterialTheme.colors.primary
        )
      }
    }
  }

  val secondaryState = formState?.fields?.find { it.definition.name == formState.definition.secondaryFeedField }
  if (secondaryState?.hasValue() == true) {
    Row {
      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        Text(
          fieldText(secondaryState.answer, LocalContext.current),
          style = MaterialTheme.typography.subtitle1
        )
      }
    }
  }
}

@Composable
fun FieldContent(fieldState: FieldState<*, out FieldValue>) {
  when (fieldState) {
    is BooleanFieldState -> {
      BooleanFieldContent(fieldState)
    }
    is DateFieldState -> {
      DateFieldContent(fieldState)
    }
    is GeometryFieldState -> {
      GeometryFieldContent(fieldState)
    }
    is MultiSelectFieldState -> {
      MultiFieldContent(fieldState)
    }
    else -> {
      StringFieldContent(fieldState)
    }
  }
}

@Composable
fun StringFieldContent(fieldState: FieldState<*, out FieldValue>) {
  val text = fieldText(fieldState.answer, LocalContext.current)

  if (text.isNotEmpty()) {
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
      Text(fieldState.definition.title, fontSize = 14.sp)
    }

    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
      Text(
        text,
        fontSize = 16.sp,
        style = MaterialTheme.typography.body1,
        modifier = Modifier.padding(bottom = 16.dp)
      )
    }
  }
}

@Composable
fun BooleanFieldContent(fieldState: BooleanFieldState) {
  val value = fieldState.answer?.boolean == true

  if (value) {
    Column(
      Modifier
        .padding(bottom = 16.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .padding(bottom = 4.dp)
      ) {
        Checkbox(
          checked = value,
          enabled = false,
          onCheckedChange = null,
          modifier = Modifier
            .padding(end = 8.dp)
        )

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
          Text(fieldState.definition.title, fontSize = 14.sp)
        }
      }
    }
  }
}

@Composable
fun DateFieldContent(fieldState: DateFieldState) {
  val dateFormat =
    DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), LocalContext.current)

  val date = fieldState.answer?.date
  val text = if (date != null) {
    dateFormat.format(date)
  } else ""

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
          Text(fieldState.definition.title, fontSize = 14.sp)
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
fun MultiFieldContent(fieldState: MultiSelectFieldState) {
  val value: Collection<String>? = fieldState.answer?.choices
  if (value?.isNotEmpty() == true) {
    Column(
      Modifier
        .padding(bottom = 16.dp)
    ) {
      Row(
        Modifier
          .padding(bottom = 4.dp)
      ) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
          Text(fieldState.definition.title, fontSize = 14.sp)
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
fun GeometryFieldContent(fieldState: GeometryFieldState) {
  val value = fieldState.answer?.location
  if (value != null) {
    val coordinates = CoordinateFormatter(LocalContext.current).format(value.centroidLatLng)

    Column(
      Modifier
        .padding(bottom = 16.dp)
    ) {
      Row(
        Modifier
          .padding(bottom = 4.dp)
      ) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
          Text(fieldState.definition.title, fontSize = 14.sp)
        }
      }

      Row {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
          Text(coordinates, fontSize = 16.sp)
        }
      }
    }
  }
}

fun fieldText(
  fieldValue: FieldValue?,
  context: Context
): String {
  return when (fieldValue) {
    is FieldValue.Boolean -> {
      fieldValue.boolean.toString()
    }
    is FieldValue.Date -> {
      val dateFormat = DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), context)
      dateFormat.format(fieldValue)
    }
    is FieldValue.Location -> {
      CoordinateFormatter(context).format(fieldValue.location.centroidLatLng)
    }
    is FieldValue.Number -> {
      fieldValue.number
    }
    is FieldValue.Multi -> {
      fieldValue.choices.joinToString(", ")
    }
    is FieldValue.Text -> {
      fieldValue.text
    } else -> ""
  }
}