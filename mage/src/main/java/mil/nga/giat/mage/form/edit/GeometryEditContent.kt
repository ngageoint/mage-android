package mil.nga.giat.mage.form.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import mil.nga.giat.mage.coordinate.CoordinateFormatter
import mil.nga.giat.mage.form.FormState
import mil.nga.giat.mage.form.field.GeometryFieldState
import mil.nga.giat.mage.form.view.MapState
import mil.nga.giat.mage.form.view.MapViewContent
import mil.nga.giat.mage.form.view.rememberMapViewWithLifecycle

@Composable
fun GeometryEdit(
  fieldState: GeometryFieldState,
  formState: FormState? = null,
  onClick: (() -> Unit)? = null
) {
  val geometry = fieldState.answer?.location
  val value = if (geometry != null) {
    CoordinateFormatter(LocalContext.current).format(geometry.centroidLatLng)
  } else ""

  val focusManager = LocalFocusManager.current

  Column(Modifier.padding(bottom = 16.dp)) {
    TextField(
      value = value,
      onValueChange = { },
      label = { Text("${fieldState.definition.title}${if (fieldState.definition.required) " *" else ""}") },
      enabled = false,
      isError = fieldState.showErrors(),
      colors = TextFieldDefaults.textFieldColors(
        disabledTextColor = LocalContentColor.current.copy(LocalContentAlpha.current),
        disabledLabelColor =  MaterialTheme.colors.onSurface.copy(ContentAlpha.medium)
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

    val location = fieldState.answer?.location
    if (location != null) {
      Box(
        Modifier
          .fillMaxWidth()
          .height(150.dp)
      ) {
        val mapView = rememberMapViewWithLifecycle()
        MapViewContent(mapView, formState, location, MapState(fieldState.defaultMapCenter, fieldState.defaultMapZoom))
      }
    }

    fieldState.getError()?.let { error -> TextFieldError(textError = error) }
  }
}