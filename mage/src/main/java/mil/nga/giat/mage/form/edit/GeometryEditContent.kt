package mil.nga.giat.mage.form.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Place
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
  modifier: Modifier = Modifier,
  fieldState: GeometryFieldState,
  formState: FormState? = null,
  onClick: (() -> Unit)? = null
) {
  val geometry = fieldState.answer?.location
  val value = if (geometry != null) {
    CoordinateFormatter(LocalContext.current).format(geometry.centroidLatLng)
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
        MapViewContent(mapView, formState, location, MapState(fieldState.defaultMapCenter, fieldState.defaultMapZoom))
      }
    }

    fieldState.getError()?.let { error -> TextFieldError(textError = error) }
  }
}