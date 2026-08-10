@file:OptIn(ExperimentalFoundationApi::class)

package mil.nga.giat.mage.observation.edit

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.*
import androidx.compose.material.ButtonDefaults.textButtonColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mil.nga.giat.mage.compat.server5.form.view.AttachmentsViewContentServer5
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.form.FormState
import mil.nga.giat.mage.form.FormViewModel
import mil.nga.giat.mage.form.edit.DateEdit
import mil.nga.giat.mage.form.edit.FormEditContent
import mil.nga.giat.mage.form.edit.GeometryEdit
import mil.nga.giat.mage.form.field.*
import mil.nga.giat.mage.observation.ObservationState
import mil.nga.giat.mage.observation.ObservationValidationResult
import mil.nga.giat.mage.sdk.Compatibility.Companion.isServerVersion5
import mil.nga.giat.mage.database.model.observation.Attachment
import mil.nga.giat.mage.ui.theme.MageTheme
import mil.nga.giat.mage.ui.theme.topAppBarBackground

enum class AttachmentAction {
  VIEW, DELETE
}

enum class MediaActionType {
  GALLERY, PHOTO, VIDEO, VOICE, FILE
}

@Parcelize
data class MediaAction (
  val type: MediaActionType,
  val formIndex: Int?,
  val fieldName: String?
): Parcelable

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ObservationEditScreen(
   viewModel: FormViewModel,
   onSave: (() -> Unit)? = null,
   onCancel: (() -> Unit)? = null,
   onAddForm: (() -> Unit)? = null,
   onDeleteForm: ((Int) -> Unit)? = null,
   onReorderForms: (() -> Unit)? = null,
   onFieldClick: ((FieldState<*, *>) -> Unit)? = null,
   onAttachmentAction: ((AttachmentAction, Attachment, FieldState<*, *>?) -> Unit)? = null,
   onMediaAction: ((MediaAction) -> Unit)? = null
) {
  val observationState by viewModel.observationState.observeAsState()
  val scope = rememberCoroutineScope()
  val scaffoldState = rememberScaffoldState()
  val listState = rememberLazyListState()
  val focusManager = LocalFocusManager.current

  val focusedUndoField by remember {
    derivedStateOf {
      observationState?.forms?.value
        ?.flatMap { it.fields }
        ?.firstOrNull { field ->
          field.isFocused && when (field) {
            is TextFieldState -> field.hasValue() || field.inputState.undoState.canUndo || field.inputState.undoState.canRedo
            is NumberFieldState -> field.hasValue() || field.inputState.undoState.canUndo || field.inputState.undoState.canRedo
            else -> false
          }
        }
    }
  }

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
        var lastFocusedField by remember { mutableStateOf<FieldState<*, *>?>(null) }
        focusedUndoField?.let { lastFocusedField = it }

        var undoBarHeightPx by remember { mutableStateOf(0) }
        val density = LocalDensity.current

        // Scroll the list up by the bar height when it first appears so the focused
        // field is not hidden behind it.
        LaunchedEffect(Unit) {
          snapshotFlow { (focusedUndoField != null) to undoBarHeightPx }
            .distinctUntilChanged()
            .collect { (focused, height) ->
              if (focused && height > 0) {
                listState.animateScrollBy(height.toFloat())
              }
            }
        }

        Column(
          modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .pointerInput(Unit) {
              detectTapGestures(onTap = { focusManager.clearFocus() })
            }
        ) {
          if (isServerVersion5(LocalContext.current)) {
            ObservationMediaBar { onMediaAction?.invoke(MediaAction(it, null, null)) }
          }

          ObservationEditContent(
            modifier = Modifier.weight(1f),
            event = viewModel.event,
            observationState = observationState,
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

          AnimatedVisibility(
            visible = focusedUndoField != null,
            enter = slideInVertically { it } + fadeIn(animationSpec = tween(200)),
            exit = slideOutVertically { it } + fadeOut(animationSpec = tween(200)),
          ) {
            UndoRedoBar(
              focusedField = lastFocusedField,
              modifier = Modifier.onSizeChanged { undoBarHeightPx = it.height }
            )
          }
        }
      },
      floatingActionButton = {
        val max = observationState?.definition?.maxObservationForms
        val totalForms = observationState?.forms?.value?.size ?: 0
        if (max == null || totalForms < max) {
          ExtendedFloatingActionButton(
            modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
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
    modifier = Modifier
      .background(color = MaterialTheme.colors.topAppBarBackground)
      .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)),
    backgroundColor = MaterialTheme.colors.topAppBarBackground,
    elevation = 0.dp,
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
  modifier: Modifier = Modifier,
  event: Event?,
  observationState: ObservationState?,
  listState: LazyListState,
  onFieldClick: ((FieldState<*, *>) -> Unit)? = null,
  onMediaAction: ((MediaAction) -> Unit)? = null,
  onAttachmentAction: ((AttachmentAction, Attachment, FieldState<*, *>?) -> Unit)? = null,
  onDeleteForm: ((Int, FormState) -> Unit)? = null,
  onReorderForms: (() -> Unit)? = null
) {
  val context = LocalContext.current

  Box(modifier = modifier) {
  if (observationState != null) {
    val forms by observationState.forms
    var previousForms by remember { mutableStateOf<List<FormState>>(listOf()) }

    // TODO scroll to added element, not last
    LaunchedEffect(forms.size) {
      if (previousForms.isNotEmpty() && forms.size > previousForms.size) {
        // find new form that was added, diff between forms and previous forms
        val addedForm = forms.filterNot { previousForms.contains(it) }.first()
        val scrollTo = forms.indexOf(addedForm) + 2 // account for 2 "header" items in list
        listState.animateScrollToItem(scrollTo)
      }

      previousForms = forms
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
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))

    ) {
      item {
        ObservationEditHeaderContent(
          event = event,
          timestamp = observationState.timestampFieldState,
          geometry = observationState.geometryFieldState,
          formState = forms.getOrNull(0),
          onTimestampClick = { onFieldClick?.invoke(observationState.timestampFieldState) },
          onLocationClick = { onFieldClick?.invoke(observationState.geometryFieldState) }
        )
      }

      if (isServerVersion5(context)) {
        item {
          val attachments by observationState.attachments
          AttachmentsViewContentServer5(attachments) {
            onAttachmentAction?.invoke(AttachmentAction.VIEW, it, null)
          }
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
          event = event,
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
  } // Box
}

@Composable
fun ObservationEditHeaderContent(
  event: Event?,
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
        event = event,
        fieldState = geometry,
        formState = formState,
        onClick = onLocationClick,
        modifier = Modifier.padding(bottom = 16.dp)
      )
    }
  }
}

@Composable
fun UndoRedoBar(
  modifier: Modifier = Modifier,
  focusedField: FieldState<*, *>?
) {
  val canUndo = when (focusedField) {
    is TextFieldState -> focusedField.inputState.undoState.canUndo
    is NumberFieldState -> focusedField.inputState.undoState.canUndo
    else -> false
  }
  val canRedo = when (focusedField) {
    is TextFieldState -> focusedField.inputState.undoState.canRedo
    is NumberFieldState -> focusedField.inputState.undoState.canRedo
    else -> false
  }

  Surface(
    modifier = modifier.fillMaxWidth(),
    elevation = 8.dp,
    color = MaterialTheme.colors.surface
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = {
        when (focusedField) {
          is TextFieldState -> focusedField.inputState.undoState.undo()
          is NumberFieldState -> focusedField.inputState.undoState.undo()
          else -> {}
        }
      }, enabled = canUndo) {
        Icon(
          imageVector = Icons.Outlined.Undo,
          contentDescription = "Undo"
        )
      }
      IconButton(onClick = {
        when (focusedField) {
          is TextFieldState -> focusedField.inputState.undoState.redo()
          is NumberFieldState -> focusedField.inputState.undoState.redo()
          else -> {}
        }
      }, enabled = canRedo) {
        Icon(
          imageVector = Icons.Outlined.Redo,
          contentDescription = "Redo"
        )
      }
    }
  }
}