package mil.nga.giat.mage.observation.view

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mil.nga.giat.mage.coordinate.CoordinateFormatter
import mil.nga.giat.mage.form.FormState
import mil.nga.giat.mage.form.FormViewModel
import mil.nga.giat.mage.map.marker.ObservationBitmapFactory
import mil.nga.giat.mage.form.field.FieldValue
import mil.nga.giat.mage.form.view.*
import mil.nga.giat.mage.observation.ObservationPermission
import mil.nga.giat.mage.observation.ObservationState
import mil.nga.giat.mage.observation.ObservationStatusState
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import mil.nga.giat.mage.ui.theme.*
import mil.nga.giat.mage.utils.DateFormatFactory
import java.util.*

enum class ObservationAction {
  EDIT, FLAG, FAVORITE, DIRECTIONS, MORE
}

@Composable
fun ObservationViewScreen(
  viewModel: FormViewModel,
  onClose: (() -> Unit)? = null,
  onAction: ((ObservationAction) -> Unit)? = null,
  onLocationClick: ((String) -> Unit)? = null,
  onAttachmentClick: ((Attachment) -> Unit)? = null
) {
  val observationState by viewModel.observationState.observeAsState()

  MageTheme {
    Scaffold(
      topBar = {
        ObservationViewTopBar(
          observationState = observationState,
          onClose = { onClose?.invoke() }
        )
      },
      content = {
        Column {
          ObservationViewContent(
            observationState,
            onAction = onAction,
            onLocationClick = onLocationClick,
            onAttachmentClick = onAttachmentClick
          )
        }
      },
      floatingActionButton = {
        if (observationState?.permissions?.contains(ObservationPermission.EDIT) == true) {
          FloatingActionButton(
            onClick = { onAction?.invoke(ObservationAction.EDIT) }
          ) {
            Icon(
              Icons.Default.Edit,
              contentDescription = "Edit Observation",
              tint = Color.White
            )
          }
        }
      }
    )
  }
}

@Composable
fun ObservationViewTopBar(
  observationState: ObservationState?,
  onClose: () -> Unit
) {
  val formState = observationState?.forms?.value?.firstOrNull()
  val primary = formState?.fields?.find { it.definition.name == formState.definition.primaryMapField }?.answer as? FieldValue.Text
  val title = primary?.text ?: "Observation"

  TopAppBar(
    backgroundColor = MaterialTheme.colors.topAppBarBackground,
    contentColor = Color.White,
    title = {
      Column {
        Text(title)
        if (observationState?.eventName != null) {
          CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(observationState.eventName, fontSize = 14.sp)
          }
        }
      }
    },
    navigationIcon = {
      IconButton(onClick = { onClose.invoke() }) {
        Icon(Icons.Default.Close, "Cancel Edit")
      }
    }
  )
}

@Composable
fun ObservationViewContent(
  observationState: ObservationState?,
  onAction: ((ObservationAction) -> Unit)? = null,
  onLocationClick: ((String) -> Unit)? = null,
  onAttachmentClick: ((Attachment) -> Unit)? = null
) {
  if (observationState != null) {

    Surface(
      color = MaterialTheme.colors.surface,
      contentColor = contentColorFor(MaterialTheme.colors.surface),
      elevation = 2.dp
    ) {
      val status by observationState.status
      ObservationViewStatusContent(status)
    }

    Column(
      modifier = Modifier
        .background(Color(0x19000000))
        .fillMaxHeight()
        .verticalScroll(rememberScrollState())
        .padding(start = 8.dp, end = 8.dp, bottom = 80.dp)
    ) {
      val forms by observationState.forms

      ObservationViewHeaderContent(
        observationState = observationState,
        onAction = onAction,
        onLocationClick = { onLocationClick?.invoke(it) }
      )

      if (forms.isNotEmpty()) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 16.dp)
        ) {
          CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
              text = "FORMS",
              style = MaterialTheme.typography.caption,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
            )
          }
        }
      }

      for (formState in forms) {
        FormContent(formState, onAttachmentClick)
      }
    }
  }
}

@Composable
fun ObservationViewStatusContent(
  status: ObservationStatusState
) {
  if (status.dirty) {
    if (status.error != null) {
      ObservationErrorStatus(error = status.error)
    } else {
      ObservationLocalStatus()
    }
  } else if (status.lastModified != null) {
    ObservationSyncStatus(syncDate = status.lastModified)
  }
}

@Composable
fun ObservationSyncStatus(
  syncDate: Date
) {
  val dateFormat =
    DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), LocalContext.current)

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp)
  ) {
    Icon(
      imageVector = Icons.Default.Check,
      contentDescription = "Sync",
      tint = Color(0xFF66BB6A),
      modifier = Modifier
        .width(24.dp)
        .height(24.dp)
        .padding(end = 8.dp)
    )

    Text(
      text = "Pushed on ${dateFormat.format(syncDate)}",
      style = MaterialTheme.typography.body2,
      color = Color(0xFF66BB6A)
    )
  }
}

@Composable
fun ObservationLocalStatus() {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp)
  ) {
    Icon(
      imageVector = Icons.Default.Sync,
      contentDescription = "Local",
      tint = Color(0xFFFFA726),
      modifier = Modifier
        .width(24.dp)
        .height(24.dp)
        .padding(end = 8.dp)
    )

    Text(
      text = "Saved locally, will be submitted",
      style = MaterialTheme.typography.body2,
      color = Color(0xFFFFA726)
    )
  }
}

@Composable
fun ObservationErrorStatus(
  error: String
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 8.dp)
    ) {
      Icon(
        imageVector = Icons.Default.ErrorOutline,
        contentDescription = "Local",
        tint = Color(0xFFF44336),
        modifier = Modifier
          .width(24.dp)
          .height(24.dp)
          .padding(end = 8.dp)
      )

      Text(
        text = "Observation changes not pushed",
        style = MaterialTheme.typography.body2,
        color = Color(0xFFF44336)
      )
    }

    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
      Text(
        text = error,
        style = MaterialTheme.typography.body1
      )
    }
  }
}

@Composable
fun ObservationViewHeaderContent(
  observationState: ObservationState? = null,
  onLocationClick: ((String) -> Unit)? = null,
  onAction: ((ObservationAction) -> Unit)? = null
) {
  val dateFormat =
    DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), LocalContext.current)

  val formState = observationState?.forms?.value?.firstOrNull()
  Card(
    Modifier
      .fillMaxWidth()
      .padding(top = 8.dp)
  ) {
    Column {
      val important = observationState?.important?.value
      if (important != null) {
        Row(
          Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.importantBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Flag,
            contentDescription = "Important Flag",
            modifier = Modifier
              .height(40.dp)
              .width(40.dp)
              .padding(end = 8.dp)
          )

          Column() {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
              Text(
                text = "Flagged by ${important.user}".toUpperCase(Locale.ROOT),
                style = MaterialTheme.typography.overline,
                fontWeight = FontWeight.SemiBold,
              )
            }

            if (important.description != null) {
              Text(
                text = important.description
              )
            }
          }
        }
      }

      Row(modifier = Modifier
        .padding(top = 16.dp, start = 16.dp, bottom = 16.dp)
      ) {
        Column(Modifier.weight(1f)) {
          Row(
            modifier = Modifier
              .padding(bottom = 16.dp)
          ) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
              observationState?.userDisplayName?.let {
                Text(
                  text = it.toUpperCase(Locale.ROOT),
                  fontWeight = FontWeight.SemiBold,
                  style = MaterialTheme.typography.overline
                )
                Text(
                  text = "\u2022",
                  fontWeight = FontWeight.SemiBold,
                  style = MaterialTheme.typography.overline,
                  modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                )
              }

              observationState?.timestampFieldState?.answer?.date?.let {
                Text(
                  text = dateFormat.format(it).toUpperCase(Locale.ROOT),
                  fontWeight = FontWeight.SemiBold,
                  style = MaterialTheme.typography.overline
                )
              }
            }
          }

          FormHeaderContent(formState)
        }

        ObservationIcon(formState)
      }

      observationState?.geometryFieldState?.answer?.location?.let { location ->
        Box(
          Modifier
            .fillMaxWidth()
            .height(150.dp)
        ) {
          val mapView = rememberMapViewWithLifecycle()
          val mapState = MapState(observationState.geometryFieldState.defaultMapCenter, observationState.geometryFieldState.defaultMapZoom)
          MapViewContent(mapView, formState, location, mapState)
        }

        val locationText = CoordinateFormatter(LocalContext.current).format(location.centroidLatLng)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onLocationClick?.invoke(locationText) }
            .padding(16.dp)
        ) {
          Icon(
            imageVector = Icons.Default.GpsFixed,
            contentDescription = "Location",
            tint = MaterialTheme.colors.primary,
            modifier = Modifier
              .height(24.dp)
              .width(24.dp)
              .padding(end = 4.dp)
          )

          CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
            Text(
              text = locationText,
              color = MaterialTheme.colors.primary,
              style = MaterialTheme.typography.body2,
              modifier = Modifier.padding(end = 8.dp)
            )
          }

          if (location.provider != null) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
              Text(
                text = location.provider.toUpperCase(Locale.ROOT),
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(end = 2.dp)
              )
            }
          }

          if (location.accuracy != null) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
              Text(
                text = " \u00B1 ${location.accuracy}",
                style = MaterialTheme.typography.caption,
              )
            }
          }
        }
      }

      Divider(Modifier.fillMaxWidth())

      ObservationActions(observationState) { onAction?.invoke(it) }
    }
  }
}

@Composable
fun ObservationActions(
  observationState: ObservationState?,
  onAction: ((ObservationAction) -> Unit)? = null
) {
  Row(
    Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.End
  ) {


    if (observationState?.permissions?.contains(ObservationPermission.FLAG) == true) {
      val isFlagged = observationState.important.value != null
      val flagTint = if (isFlagged) {
        Color(0XFFFF9100)
      } else {
        MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
      }

      IconButton(
        modifier = Modifier.padding(end = 8.dp),
        onClick = { onAction?.invoke(ObservationAction.FLAG) }
      ) {
        Icon(
          imageVector = if (isFlagged) Icons.Default.Flag else Icons.Outlined.Flag,
          tint = flagTint,
          contentDescription = "Flag",
        )
      }
    }

    val isFavorite = observationState?.favorite?.value == true
    val favoriteTint = if (isFavorite) {
      Color(0XFF7ED31F)
    } else {
      MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
    }

    IconButton(
      modifier = Modifier.padding(end = 8.dp),
      onClick = { onAction?.invoke(ObservationAction.FAVORITE) }
    ) {
      Icon(
        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
        tint = favoriteTint,
        contentDescription = "Favorite",
      )
    }

    IconButton(
      modifier = Modifier.padding(end = 8.dp),
      onClick = { onAction?.invoke(ObservationAction.DIRECTIONS) }
    ) {
      Icon(
        imageVector = Icons.Outlined.Directions,
        contentDescription = "Directions",
        tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
      )
    }

    if (observationState?.permissions?.contains(ObservationPermission.DELETE) == true) {
      IconButton(
        modifier = Modifier.padding(end = 8.dp),
        onClick = { onAction?.invoke(ObservationAction.MORE) }
      ) {
        Icon(
          imageVector = Icons.Default.MoreVert,
          contentDescription = "MoreVert",
          tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
        )
      }
    }
  }
}

@Composable
fun ObservationIcon(
  formState: FormState?
) {
  val primary = formState?.fields?.find { it.definition.name == formState.definition.primaryMapField }?.answer as? FieldValue.Text
  val secondary = formState?.fields?.find { it.definition.name == formState.definition.secondaryMapField }?.answer as? FieldValue.Text
  val bitmap = ObservationBitmapFactory.bitmap(LocalContext.current, formState?.eventId, formState?.definition?.id, primary, secondary)

  Box(modifier = Modifier
    .padding(start = 16.dp, end = 16.dp)
  ) {
    Image(
      bitmap = bitmap.asImageBitmap(),
      contentDescription = "Observation Icon",
      modifier = Modifier
        .width(40.dp)
        .height(40.dp)
    )
  }
}