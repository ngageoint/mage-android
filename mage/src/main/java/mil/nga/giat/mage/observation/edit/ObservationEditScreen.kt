package mil.nga.giat.mage.observation.edit

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.*
import androidx.compose.material.ButtonDefaults.textButtonColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mil.nga.giat.mage.form.FormState
import mil.nga.giat.mage.form.FormViewModel
import mil.nga.giat.mage.form.edit.DateEdit
import mil.nga.giat.mage.form.edit.FormEditContent
import mil.nga.giat.mage.form.edit.GeometryEdit
import mil.nga.giat.mage.observation.ObservationState
import mil.nga.giat.mage.form.field.DateFieldState
import mil.nga.giat.mage.form.field.FieldState
import mil.nga.giat.mage.form.field.GeometryFieldState
import mil.nga.giat.mage.form.view.AttachmentsViewContent
import mil.nga.giat.mage.observation.ObservationValidationResult
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
  onReorderForms: (() -> Unit)? = null,
  onFieldClick: ((FieldState<*, *>) -> Unit)? = null,
  onAttachmentClick: ((Attachment) -> Unit)? = null
) {
  val observationState by viewModel.observationState.observeAsState()
  val scope = rememberCoroutineScope()
  val scaffoldState = rememberScaffoldState()
  val listState = rememberLazyListState()

  MageTheme {
    Scaffold(
      scaffoldState = scaffoldState,
      topBar = {
        ObservationEditTopBar(
          isNewObservation = observationState?.id == null,
          eventName = observationState?.eventName,
          onSave = {
            observationState?.let { state ->
              when (val result = state.validate()) {
                is ObservationValidationResult.Invalid -> {
                  scope.launch {
                    scaffoldState.snackbarHostState.showSnackbar(result.error)
                  }
                }
                is ObservationValidationResult.Valid -> onSave?.invoke()
              }
            }
          },
          onCancel = { onCancel?.invoke() }
        )
      },
      content = {
        Column {
          ObservationMediaBar { onAction?.invoke(it) }

          ObservationEditContent(
            observationState,
            listState = listState,
            onFieldClick = onFieldClick,
            onAttachmentClick = onAttachmentClick,
            onReorderForms = onReorderForms,
            onDeleteForm = { index, formState ->
              scope.launch {
                val result = scaffoldState.snackbarHostState.showSnackbar("Form deleted", "UNDO")
                if (result == SnackbarResult.ActionPerformed) {
                  val forms = observationState?.forms?.value?.toMutableList() ?: mutableListOf()
                  forms.add(index, formState)
                  observationState?.forms?.value = forms
                }
              }
              onDeleteForm?.invoke(index)
            }
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
      TextButton(
        onClick = { onSave.invoke() },
        colors = textButtonColors(contentColor = Color.White)
      ) {
        Text("SAVE")
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
  listState: LazyListState,
  onFieldClick: ((FieldState<*, *>) -> Unit)? = null,
  onAttachmentClick: ((Attachment) -> Unit)? = null,
  onDeleteForm: ((Int, FormState) -> Unit)? = null,
  onReorderForms: (() -> Unit)? = null
) {

  if (observationState != null) {
    val forms by observationState.forms
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(forms.size) {
      if (initialized) {
        listState.animateScrollToItem(forms.size + 1)
      }

      initialized = true
    }

    LazyColumn(
      state = listState,
      contentPadding = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        top = 8.dp,
        bottom = 72.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .background(Color(0x19000000))
        .fillMaxHeight()
    ) {
      item {
        ObservationEditHeaderContent(
          timestamp = observationState.timestampFieldState,
          geometry = observationState.geometryFieldState,
          formState = forms.getOrNull(0),
          onTimestampClick = { onFieldClick?.invoke(observationState.timestampFieldState) },
          onLocationClick = { onFieldClick?.invoke(observationState.geometryFieldState) }
        )

        val attachments by observationState.attachments
        AttachmentsViewContent(attachments, onAttachmentClick)
      }

      if (forms.isNotEmpty()) {
        item {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
          ) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
              Text(
                text = "FORMS",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                  .weight(1f)
                  .padding(vertical = 16.dp)
              )
            }

            if (forms.size > 1) {
              IconButton(
                onClick = { onReorderForms?.invoke() },
              ) {
                Icon(
                  Icons.Default.SwapVert,
                  tint = MaterialTheme.colors.primary,
                  contentDescription = "Reorder Forms")
              }
            }
          }
        }
      }

      itemsIndexed(forms) { index, formState ->
        FormEditContent(
          formState = formState,
          onFormDelete = { onDeleteForm?.invoke(index, formState) },
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
    Modifier.fillMaxWidth()
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