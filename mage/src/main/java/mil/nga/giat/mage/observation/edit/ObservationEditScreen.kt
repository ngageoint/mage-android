package mil.nga.giat.mage.observation.edit

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mil.nga.giat.mage.form.FormState
import mil.nga.giat.mage.form.edit.DateEdit
import mil.nga.giat.mage.form.edit.FormEditContent
import mil.nga.giat.mage.form.edit.GeometryEdit
import mil.nga.giat.mage.observation.form.ObservationState
import mil.nga.giat.mage.observation.form.*
import mil.nga.giat.mage.form.field.DateFieldState
import mil.nga.giat.mage.form.field.FieldState
import mil.nga.giat.mage.form.field.GeometryFieldState
import mil.nga.giat.mage.form.view.AttachmentsViewContent
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import mil.nga.giat.mage.ui.theme.MageTheme

enum class ObservationMediaAction {
  GALLERY, PHOTO, VIDEO, VOICE
}

@Composable
fun ObservationEditScreen(
  viewModel: FormViewModel,
  onSave: (() -> Unit)? = null,
  onCancel: (() -> Unit)? = null,
  onAction: ((ObservationMediaAction) -> Unit)? = null,
  onAddForm: (() -> Unit)? = null,
  onDeleteForm: ((Int) -> Unit)? = null,
  onFieldClick: ((FieldState<*, *>) -> Unit)? = null,
  onAttachmentClick: ((Attachment) -> Unit)? = null
) {
  val observationState by viewModel.observationState.observeAsState()

  MageTheme {
    Scaffold(
      topBar = {
        ObservationEditTopBar(
          isNewObservation = observationState?.id == null,
          eventName = observationState?.eventName,
          onSave = { onSave?.invoke() },
          onCancel = { onCancel?.invoke() }
        )
      },
      content = {
        Column {
          ObservationMediaBar { onAction?.invoke(it) }

          ObservationEditContent(
            observationState,
            onFieldClick = onFieldClick,
            onAttachmentClick = onAttachmentClick,
            onDeleteForm = onDeleteForm
          )
        }
      },
      floatingActionButton = {
        ExtendedFloatingActionButton(
          icon = {
            Icon(
              Icons.Default.NoteAdd,
              contentDescription = "Add Form",
              tint = Color.White
            )
           },
          text = { Text("ADD FORM", color = Color.White) },
          onClick = { onAddForm?.invoke() }
        )
      }
    )
  }
}

@Composable
fun ObservationEditTopBar(
  isNewObservation: Boolean,
  eventName: String?,
  onSave: () -> Unit,
  onCancel: () -> Unit
) {
  val title = if (isNewObservation) "Create Observation" else "Observation Edit"
  TopAppBar(
    title = {
      Column {
        Text(title)
        if (eventName != null) {
          CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(eventName, fontSize = 14.sp)
          }
        }
      }
    },
    navigationIcon = {
      IconButton(onClick = { onCancel.invoke() }) {
        Icon(Icons.Default.Close, "Cancel Edit")
      }
    },
    actions = {
      TextButton(onClick = { onSave.invoke() }) {
        Text("SAVE", color = Color.White)
      }
    }
  )
}

@Composable
fun ObservationMediaBar(
  onAction: (ObservationMediaAction) -> Unit
) {
  Surface(
    elevation = 2.dp,
  ) {
    Row(
      horizontalArrangement = Arrangement.SpaceEvenly,
      modifier = Modifier.fillMaxWidth()
    ) {
      IconButton(onClick = { onAction.invoke(ObservationMediaAction.GALLERY) }) {
        Icon(Icons.Default.Image, "Capture Gallery", tint = Color(0xFF66BB6A))
      }
      IconButton(onClick = { onAction.invoke(ObservationMediaAction.PHOTO) }) {
        Icon(Icons.Default.PhotoCamera, "Capture Photo", tint = Color(0xFF42A5F5))
      }
      IconButton(onClick = { onAction.invoke(ObservationMediaAction.VIDEO) }) {
        Icon(Icons.Default.Videocam, "Capture Video", tint = Color(0xFFEC407A))
      }
      IconButton(onClick = { onAction.invoke(ObservationMediaAction.VOICE) }) {
        Icon(Icons.Default.Mic, "Capture Audio", tint = Color(0xFFAB47BC))
      }
    }
  }
}

@Composable
fun ObservationEditContent(
  observationState: ObservationState?,
  onFieldClick: ((FieldState<*, *>) -> Unit)? = null,
  onAttachmentClick: ((Attachment) -> Unit)? = null,
  onDeleteForm: ((Int) -> Unit)? = null,
  ) {
  if (observationState != null) {
    Column(
      modifier = Modifier
        .background(Color(0x19000000))
        .fillMaxHeight()
        .verticalScroll(rememberScrollState())
        .padding(bottom = 72.dp)
    ) {
      val forms by observationState.forms

      ObservationEditHeaderContent(
        timestamp = observationState.timestampFieldState,
        geometry = observationState.geometryFieldState,
        formState = forms.getOrNull(0),
        onTimestampClick = { onFieldClick?.invoke(observationState.timestampFieldState) },
        onLocationClick = { onFieldClick?.invoke(observationState.geometryFieldState) }
      )

      val attachments by observationState.attachments
      AttachmentsViewContent(attachments, onAttachmentClick)

      for ((index, formState) in forms.withIndex()) {
        FormEditContent(
          formState = formState,
          onFormDelete = { onDeleteForm?.invoke(index) },
          onFieldClick = { onFieldClick?.invoke(it) }
        )
      }
    }
  }
}

@Composable
fun ObservationEditHeaderContent(
  timestamp: DateFieldState,
  geometry: GeometryFieldState,
  formState: FormState? = null,
  onTimestampClick: (() -> Unit)? = null,
  onLocationClick: (() -> Unit)? = null
) {
  Card(
    Modifier
      .fillMaxWidth()
      .padding(8.dp)
  ) {
    Column(Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
      DateEdit(
        timestamp,
        onClick = onTimestampClick
      )

      GeometryEdit(
        geometry,
        formState,
        onClick = onLocationClick
      )
    }
  }
}