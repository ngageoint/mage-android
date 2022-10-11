package mil.nga.giat.mage.form.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import mil.nga.giat.mage.coordinate.CoordinateFormatter
import mil.nga.giat.mage.form.FormState
import mil.nga.giat.mage.form.field.GeometryFieldState
import mil.nga.giat.mage.form.view.MapState
import mil.nga.giat.mage.form.view.MapViewContent
import mil.nga.giat.mage.form.view.rememberMapViewWithLifecycle
import mil.nga.giat.mage.ui.theme.warning

@Composable
fun GeometryEdit(
  modifier: Modifier = Modifier,
  fieldState: GeometryFieldState,
  formState: FormState? = null,
  onClick: (() -> Unit)? = null
) {
  val geometry = fieldState.answer?.location
  val value = if (geometry != null) {
    val accuracy = geometry.accuracy?.let { "± ${it}m" } ?: ""
    "${CoordinateFormatter(LocalContext.current).format(geometry.centroidLatLng)} $accuracy"
  } else ""

  val focusManager = LocalFocusManager.current

  Column(modifier) {
    TextField(
      value = value,
      onValueChange = { },
      label = { Text("${fieldState.definition.title}${if (fieldState.definition.required) " *" else ""}") },
      enabled = false,
      isError = fieldState.showErrors(),
      colors = TextFieldDefaults.textFieldColors(
        disabledTrailingIconColor = MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.IconOpacity),
        disabledTextColor = LocalContentColor.current.copy(LocalContentAlpha.current),
        disabledLabelColor =  MaterialTheme.colors.onSurface.copy(ContentAlpha.medium)
      ),
      trailingIcon = {
        Icon(
          imageVector = Icons.Outlined.Place,
          contentDescription = "Map Marker",
        )
      },
      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = {
          onClick?.invoke()
          focusManager.clearFocus()
          fieldState.onFocusChange(true)
          fieldState.enableShowErrors()
        })
    )

    val location = fieldState.answer?.location
    if (location != null) {
      Box(
        Modifier
          .fillMaxWidth()
          .height(150.dp)
      ) {
        val mapView = rememberMapViewWithLifecycle()
        MapViewContent(mapView, MapState(fieldState.defaultMapCenter, fieldState.defaultMapZoom), formState, location)
      }
    }

    val error = fieldState.getError()
    val accuracy = fieldState.answer?.location?.accuracy ?: 0f
    if (error != null) {
      TextFieldError(textError = error)
    } else if (accuracy > 500) {
      AccuracyWarning(accuracy)
    }
  }
}

@Composable
private fun AccuracyWarning(accuracy: Float) {
  Row(modifier = Modifier.fillMaxWidth()) {
    Spacer(modifier = Modifier.width(16.dp))

    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
      Text(
        text = "Please check observation location, accuracy is $accuracy.",
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.warning)
      )
    }
  }
}