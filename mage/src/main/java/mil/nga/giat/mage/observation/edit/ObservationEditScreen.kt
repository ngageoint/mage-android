package mil.nga.giat.mage.observation.edit

import android.os.Parcelable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import mil.nga.giat.mage._server5.form.view.AttachmentsViewContent_server5
import mil.nga.giat.mage.form.Form
import mil.nga.giat.mage.form.FormField
import mil.nga.giat.mage.form.FormState
import mil.nga.giat.mage.form.FormViewModel
import mil.nga.giat.mage.form.edit.DateEdit
import mil.nga.giat.mage.form.edit.FormEditContent
import mil.nga.giat.mage.form.edit.GeometryEdit
import mil.nga.giat.mage.form.field.*
import mil.nga.giat.mage.observation.ObservationState
import mil.nga.giat.mage.observation.ObservationValidationResult
import mil.nga.giat.mage.sdk.Compatibility.Companion.isServerVersion5
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import mil.nga.giat.mage.ui.theme.MageTheme

enum class AttachmentAction {
  VIEW, DELETE
}

enum class MediaActionType {
  GALLERY, PHOTO, VIDEO, VOICE
}

@Parcelize
data class MediaAction (
  val type: MediaActionType,
  val formIndex: Int?,
  val fieldName: String?
): Parcelable

@Composable
fun ObservationEditScreen(
  viewModel: FormViewModel,
  onSave: (() -> Unit)? = null,
  onCancel: (() -> Unit)? = null,
  onAddForm: (() -> Unit)? = null,
  onDeleteForm: ((Int) -> Unit)? = null,
  onReorderForms: (() -> Unit)? = null,
  onFieldClick: ((FieldState<*, *>) -> Unit)? = null,
  onAttachmentAction: ((AttachmentAction, Attachment, FieldState<*, *>) -> Unit)? = null,
  onMediaAction: ((MediaAction) -> Unit)? = null
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
          if (isServerVersion5(LocalContext.current)) {
            ObservationMediaBar { onMediaAction?.invoke(MediaAction(it, null, null)) }
          }

          ObservationEditContent(
            observationState,
            listState = listState,
            onFieldClick = onFieldClick,
            onMediaAction = onMediaAction,
            onAttachmentAction = { action, media, fieldState ->
              when (action) {
                AttachmentAction.VIEW -> onAttachmentAction?.invoke(action, media, fieldState)
                AttachmentAction.DELETE -> {
                  val attachmentFieldState = fieldState as AttachmentFieldState
                  val attachments = attachmentFieldState.answer?.attachments?.toMutableList() ?: mutableListOf()
                  val index = attachments.indexOf(media)
                  val attachment = attachments[index]

                  scope.launch {
                    val result = scaffoldState.snackbarHostState.showSnackbar("Attachment removed.", "UNDO")
                    if (result == SnackbarResult.ActionPerformed) {
                      // TODO should I modify state here?
                      if (attachment.url?.isNotEmpty() == true) {
                        attachment.action = null
                      }
                      attachmentFieldState.answer = FieldValue.Attachment(attachments)
                    }
                  }
                  onAttachmentAction?.invoke(action, media, fieldState)
                }
              }
            },
            onReorderForms = onReorderForms,
            onDeleteForm = { index, formState ->
              scope.launch {
                val result = scaffoldState.snackbarHostState.showSnackbar("Form deleted", "UNDO")
                if (result == SnackbarResult.ActionPerformed) {
                  // TODO should I modify state here?
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
        val max = observationState?.definition?.maxObservationForms
        val totalForms = observationState?.forms?.value?.size ?: 0
        if (max == null || totalForms < max) {
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
      }
    )
  }
}

@Composable
fun ObservationEditTopBar(
  isNewObservation: Boolean,
  onSave: () -> Unit,
  onCancel: () -> Unit
) {
  val title = if (isNewObservation) "Create Observation" else "Observation Edit"
  TopAppBar(
    title = { Text(title) },
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
  onAction: (MediaActionType) -> Unit
) {
  Surface(
    elevation = 2.dp,
  ) {
    Row(
      horizontalArrangement = Arrangement.SpaceEvenly,
      modifier = Modifier.fillMaxWidth()
    ) {
      IconButton(onClick = { onAction.invoke(MediaActionType.GALLERY) }) {
        Icon(Icons.Default.Image, "Capture Gallery", tint = Color(0xFF66BB6A))
      }
      IconButton(onClick = { onAction.invoke(MediaActionType.PHOTO) }) {
        Icon(Icons.Default.PhotoCamera, "Capture Photo", tint = Color(0xFF42A5F5))
      }
      IconButton(onClick = { onAction.invoke(MediaActionType.VIDEO) }) {
        Icon(Icons.Default.Videocam, "Capture Video", tint = Color(0xFFEC407A))
      }
      IconButton(onClick = { onAction.invoke(MediaActionType.VOICE) }) {
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
  onMediaAction: ((MediaAction) -> Unit)? = null,
  onAttachmentAction: ((AttachmentAction, Attachment, FieldState<*, *>) -> Unit)? = null,
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
      }

      item {
        if (isServerVersion5(LocalContext.current)) {
          val attachments by observationState.attachments
          AttachmentsViewContent_server5(attachments)
        }
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
          onFieldClick = { onFieldClick?.invoke(it) },
          onMediaAction = { type, field ->
            onMediaAction?.invoke(MediaAction(type, index, field.name))
          },
          onAttachmentAction = onAttachmentAction
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
        modifier = Modifier.padding(bottom = 16.dp),
        fieldState = timestamp,
        onClick = onTimestampClick
      )

      GeometryEdit(
        modifier = Modifier.padding(bottom = 16.dp),
        fieldState = geometry,
        formState = formState,
        onClick = onLocationClick
      )
    }
  }
}