package mil.nga.giat.mage.form.view

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mil.nga.giat.mage.coordinate.CoordinateFormatter
import mil.nga.giat.mage.form.FieldType
import mil.nga.giat.mage.form.FormState
import mil.nga.giat.mage.form.field.*
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import mil.nga.giat.mage.utils.DateFormatFactory
import java.util.*

@Composable
fun FormContent(
  formState: FormState,
  onAttachmentClick: ((Attachment) -> Unit)? = null,
  onLocationClick: ((String) -> Unit)? = null
) {
  Card(
    Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp)
  ) {
    Column(Modifier.animateContentSize()) {
      FormHeaderContent(
        modifier = Modifier.padding(16.dp),
        formState = formState
      ) { formState.expanded.value = it }

      if (formState.expanded.value) {
        if (formState.fields.isNotEmpty()) {
          Divider(Modifier.padding(bottom = 16.dp))
        }

        for (fieldState in formState.fields.sortedBy { it.definition.id }) {
          FieldContent(
            modifier = Modifier
              .padding(bottom = 16.dp)
              .padding(horizontal = 16.dp),
            fieldState,
            onAttachmentClick,
            onLocationClick
          )
        }
      }
    }
  }
}

@Composable
fun FormHeaderContent(
  modifier: Modifier = Modifier,
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

  Row(modifier = modifier) {
    Column(Modifier.weight(1f)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .padding(bottom = 4.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Description,
          contentDescription = "Form",
          tint = Color(android.graphics.Color.parseColor(formState.definition.hexColor)),
          modifier = Modifier.padding(end = 8.dp)
        )

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
          Text(
            formState.definition.name.uppercase(Locale.ROOT),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.overline
          )
        }
      }

      FormHeaderContent(formState)
    }

    Column(Modifier.padding(top = 8.dp)) {
      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
        IconButton(onClick = { onToggleExpand(!expanded) }) {
          Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = "Expand",
            modifier = Modifier.rotate(angle)
          )
        }
      }
    }
  }
}

@Composable
fun FormHeaderContent(
  formState: FormState?,
) {
  val primaryState = formState?.fields?.find { it.definition.name == formState.definition.primaryFeedField }
  if (primaryState?.hasValue() == true) {
    Row {
      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
        Text(
          fieldText(primaryState, LocalContext.current),
          style = MaterialTheme.typography.h6
        )
      }
    }
  }

  val secondaryState = formState?.fields?.find { it.definition.name == formState.definition.secondaryFeedField }
  if (secondaryState?.hasValue() == true) {
    Row {
      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        Text(
          fieldText(secondaryState, LocalContext.current),
          style = MaterialTheme.typography.subtitle1
        )
      }
    }
  }
}

@Composable
fun FieldContent(
  modifier: Modifier = Modifier,
  fieldState: FieldState<*, out FieldValue>,
  onAttachmentClick: ((Attachment) -> Unit)? = null,
  onLocationClick: ((String) -> Unit)? = null
) {
  when (fieldState) {
    is BooleanFieldState -> {
      BooleanFieldContent(modifier, fieldState)
    }
    is DateFieldState -> {
      DateFieldContent(modifier, fieldState)
    }
    is GeometryFieldState -> {
      GeometryFieldContent(
        modifier,
        fieldState,
        onLocationClick
      )
    }
    is MultiSelectFieldState -> {
      MultiFieldContent(modifier, fieldState)
    }
    is AttachmentFieldState -> {
      AttachmentsFieldContent(
        modifier,
        fieldState,
        onAttachmentClick
      )
    }
    else -> {
      StringFieldContent(modifier, fieldState)
    }
  }
}

@Composable
fun AttachmentsFieldContent(
  modifier: Modifier = Modifier,
  fieldState: AttachmentFieldState,
  onAttachmentClick: ((Attachment) -> Unit)? = null
) {
  val attachments = fieldState.answer?.attachments ?: listOf()
  if (attachments.isNotEmpty()) {
    Column(modifier) {
      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        Text(
          fieldState.definition.title,
          fontSize = 14.sp,
          modifier = Modifier.padding(bottom = 4.dp)
        )
      }

      AttachmentsViewContent(attachments, onAttachmentAction = { _, attachment ->
        onAttachmentClick?.invoke(attachment)
      })
    }
  }
}

@Composable
fun StringFieldContent(
  modifier: Modifier = Modifier,
  fieldState: FieldState<*, out FieldValue>
) {
  val text = fieldText(fieldState, LocalContext.current)

  if (text.isNotEmpty()) {
    Column(modifier = modifier) {
      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        Text(fieldState.definition.title, fontSize = 14.sp)
      }

      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
        Text(
          text,
          fontSize = 16.sp,
          style = MaterialTheme.typography.body1
        )
      }
    }
  }
}

@Composable
fun BooleanFieldContent(
  modifier: Modifier = Modifier,
  fieldState: BooleanFieldState
) {
  val value = fieldState.answer?.boolean == true

  if (value) {
    Column(modifier) {
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
fun DateFieldContent(
  modifier: Modifier = Modifier,
  fieldState: DateFieldState
) {
  val dateFormat =
    DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), LocalContext.current)

  val date = fieldState.answer?.date
  val text = if (date != null) {
    dateFormat.format(date)
  } else ""

  if (text?.isNotEmpty() == true) {
    Column(modifier) {
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
fun MultiFieldContent(
  modifier: Modifier,
  fieldState: MultiSelectFieldState
) {
  val value: Collection<String>? = fieldState.answer?.choices
  if (value?.isNotEmpty() == true) {
    Column(modifier) {
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
fun GeometryFieldContent(
  modifier: Modifier = Modifier,
  fieldState: GeometryFieldState,
  onLocationClick: ((String) -> Unit)? = null
) {
  val location = fieldState.answer?.location
  if (location != null) {
    val coordinates = CoordinateFormatter(LocalContext.current).format(location.centroidLatLng)

    Column(modifier) {
      Row(
        Modifier
          .padding(bottom = 4.dp)
      ) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
          Text(fieldState.definition.title, fontSize = 14.sp)
        }
      }

      Column(Modifier
        .height(150.dp)
        .fillMaxWidth()
      ) {
        val mapView = rememberMapViewWithLifecycle()
        MapViewContent(mapView, location)
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .padding(top = 4.dp)
          .clickable { onLocationClick?.invoke(coordinates) }
          .padding(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.MyLocation,
          tint = MaterialTheme.colors.primary,
          contentDescription = "Location",
          modifier = Modifier.padding(end = 4.dp).width(16.dp).height(16.dp)
        )
        Text(
          text  = coordinates,fontSize = 14.sp,
          color = MaterialTheme.colors.primary
        )
      }
    }
  }
}

fun fieldText(
  fieldState: FieldState<*, out FieldValue>,
  context: Context
): String {
  val fieldValue = fieldState.answer

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
      if (fieldState.definition.type == FieldType.PASSWORD) {
        "*".repeat((6..12).random())
      } else fieldValue.text
    } else -> ""
  }
}