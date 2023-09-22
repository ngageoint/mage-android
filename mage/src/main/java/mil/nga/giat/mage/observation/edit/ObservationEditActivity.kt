package mil.nga.giat.mage.observation.edit

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.R
import mil.nga.giat.mage.compat.server5.observation.edit.FormViewModel_server5
import mil.nga.giat.mage.form.AttachmentFormField
import mil.nga.giat.mage.form.AttachmentType
import mil.nga.giat.mage.form.ChoiceFormField
import mil.nga.giat.mage.form.Form
import mil.nga.giat.mage.form.FormState
import mil.nga.giat.mage.form.FormViewModel
import mil.nga.giat.mage.form.edit.dialog.DateFieldDialog
import mil.nga.giat.mage.form.edit.dialog.FormReorderDialog
import mil.nga.giat.mage.form.edit.dialog.GeometryFieldDialog
import mil.nga.giat.mage.form.edit.dialog.SelectFieldDialog
import mil.nga.giat.mage.form.edit.dialog.SelectFieldDialog.Companion.newInstance
import mil.nga.giat.mage.form.field.DateFieldState
import mil.nga.giat.mage.form.field.FieldState
import mil.nga.giat.mage.form.field.FieldValue
import mil.nga.giat.mage.form.field.GeometryFieldState
import mil.nga.giat.mage.form.field.Media
import mil.nga.giat.mage.form.field.MultiSelectFieldState
import mil.nga.giat.mage.form.field.SelectFieldState
import mil.nga.giat.mage.network.observation.ObservationTypeAdapter
import mil.nga.giat.mage.observation.ObservationLocation
import mil.nga.giat.mage.observation.attachment.AttachmentViewActivity
import mil.nga.giat.mage.observation.edit.FormPickerBottomSheetFragment.OnFormClickListener
import mil.nga.giat.mage.sdk.Compatibility.Companion.isServerVersion5
import mil.nga.giat.mage.database.model.observation.Attachment
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.sdk.utils.MediaUtility
import mil.nga.sf.Point
import java.io.File
import java.io.IOException
import java.util.Date
import java.util.UUID
import javax.inject.Inject

sealed class PermissionRequest(
  val permission: String,
  val deniedTitleResourceId: Int,
  val deniedMessageResourceId: Int) {
  class Image: PermissionRequest(
    Manifest.permission.CAMERA,
    R.string.camera_access_title,
    R.string.camera_access_message
  )
  class Video: PermissionRequest(
    Manifest.permission.CAMERA,
    R.string.camera_access_title,
    R.string.camera_access_message
  )
  class Audio: PermissionRequest(
    Manifest.permission.RECORD_AUDIO,
    R.string.audio_access_title,
    R.string.audio_access_message
  )
}

@AndroidEntryPoint
open class ObservationEditActivity : AppCompatActivity() {
  protected lateinit var viewModel: FormViewModel

  private var currentMediaPath: String? = null
  private var attachmentMediaAction: MediaAction? = null

  private var defaultMapLatLng = LatLng(0.0, 0.0)
  private var defaultMapZoom: Float = 0f

  @Inject lateinit var eventLocalDataSource: EventLocalDataSource

  private val requestImagePermission = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { result ->
    this.onPermission(PermissionRequest.Image(), result)
  }

  private val requestVideoPermission = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { result ->
    this.onPermission(PermissionRequest.Video(), result)
  }

  private val requestAudioPermission = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { result ->
    this.onPermission(PermissionRequest.Audio(), result)
  }

  private val getAudio = registerForActivityResult(
    CaptureAudio()
  ) { uri: Uri? ->
    val uris = if (uri != null) listOf(uri) else emptyList()
    onUris(uris)
  }

  private val getImage = registerForActivityResult(
    ActivityResultContracts.TakePicture()
  ) { status ->
    onMediaResult(status)
  }

  private val getVideo = registerForActivityResult(
    ActivityResultContracts.CaptureVideo()
  ) { status ->
    onMediaResult(status)
  }

  private val getDocument = registerForActivityResult(
    ActivityResultContracts.OpenMultipleDocuments()
  ) { uris ->
    onUris(uris)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    viewModel = if (isServerVersion5(applicationContext)) {
      ViewModelProvider(this).get(FormViewModel_server5::class.java)
    } else {
      ViewModelProvider(this).get(FormViewModel::class.java)
    }

    defaultMapLatLng = intent.getParcelableExtra(INITIAL_LOCATION) ?: LatLng(0.0, 0.0)
    defaultMapZoom = intent.getFloatExtra(INITIAL_ZOOM, 0.0f)

    val draftObservation = savedInstanceState?.getString(DRAFT_OBSERVATION_JSON)
    if (draftObservation != null) {
      restoreDraft(savedInstanceState)
    } else {
      val observationId = intent.getLongExtra(OBSERVATION_ID, NEW_OBSERVATION)
      if (observationId == NEW_OBSERVATION) {
        val location: ObservationLocation = intent.getParcelableExtra(LOCATION)!!
        val showFormPicker = viewModel.createObservation(Date(), location, defaultMapZoom, defaultMapLatLng)
        if (showFormPicker) { pickForm() }
      } else {
        viewModel.setObservation(observationId)
      }
    }

    setContent {
      ObservationEditScreen(
        viewModel = viewModel,
        onSave = { save() },
        onCancel = { cancel() },
        onAddForm = { pickForm() },
        onDeleteForm = { deleteForm(it) },
        onReorderForms = { reorderForms() },
        onFieldClick = { fieldState ->  onFieldClick(fieldState = fieldState) },
        onMediaAction = { action -> onMediaAction(action) },
        onAttachmentAction = { action, attachment, fieldState -> onAttachmentAction(action, attachment, fieldState) }
      )
    }
  }

  override fun onBackPressed() {
    cancel()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)

    (viewModel as? FormViewModel_server5)?.let {
      val attachments = arrayListOf<Attachment>().apply {
        addAll(it.attachments)
      }
      outState.putParcelableArrayList("attachmentsToCreate", attachments)
    }

    val observation = viewModel.draftObservation()
    val json = ObservationTypeAdapter().toJson(observation)
    outState.putString(DRAFT_OBSERVATION_JSON, json)
    if (observation.id != null) {
      outState.putLong(DRAFT_OBSERVATION_ID, observation.id)
    }
    outState.putParcelable(ATTACHMENT_MEDIA_ACTION, attachmentMediaAction)
    outState.putString(CURRENT_MEDIA_PATH, currentMediaPath)
  }

  private fun restoreDraft(savedInstanceState: Bundle) {
    (viewModel as? FormViewModel_server5)?.let { viewModel ->
      savedInstanceState.getParcelableArrayList<Attachment>("attachmentsToCreate")?.let {
        for (attachment in it) {
          viewModel.addAttachment(attachment, null)
        }
      }
    }

    val draftObservation = savedInstanceState.getString(DRAFT_OBSERVATION_JSON)!!
    val observation = ObservationTypeAdapter().fromJson(draftObservation)
    observation.event = eventLocalDataSource.currentEvent
    if (savedInstanceState.containsKey(DRAFT_OBSERVATION_ID)) {
      observation.id = savedInstanceState.getLong(DRAFT_OBSERVATION_ID)
    }

    viewModel.setObservation(observation)
    attachmentMediaAction = savedInstanceState.getParcelable(ATTACHMENT_MEDIA_ACTION)
    currentMediaPath = savedInstanceState.getString(CURRENT_MEDIA_PATH)
  }

  private fun showDisabledPermissionsDialog(title: String, message: String) {
    AlertDialog.Builder(this)
      .setTitle(title)
      .setMessage(message)
      .setPositiveButton(R.string.settings) { _, _ ->
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", applicationContext.packageName, null)
        startActivity(intent)
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun save() {
    if (viewModel.saveObservation()) {
      finish()
    }
  }

  private fun cancel() {
    AlertDialog.Builder(this)
      .setTitle("Discard Changes")
      .setMessage(R.string.cancel_edit)
      .setPositiveButton(R.string.discard_changes) { _, _ -> finish() }
      .setNegativeButton(R.string.no, null)
      .show()
  }

  private fun onPermission(request: PermissionRequest, result: Boolean) {
    if (result) {
      when (request) {
        is PermissionRequest.Image -> launchCameraIntent()
        is PermissionRequest.Video -> launchVideoIntent()
        is PermissionRequest.Audio -> launchAudioIntent()
      }
    } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, request.permission)) {
      showDisabledPermissionsDialog(
        resources.getString(request.deniedTitleResourceId),
        resources.getString(request.deniedMessageResourceId))
    }
  }

  private fun onUris(uris: List<Uri>) {
    val mediaAction = attachmentMediaAction

    uris.forEach { uri ->
      try {
        val file = MediaUtility.copyMediaFromUri(applicationContext, uri)
        val attachment =
          Attachment()
        attachment.action = Media.ATTACHMENT_ADD_ACTION
        attachment.localPath = file.absolutePath
        attachment.name = file.name
        attachment.contentType = contentResolver.getType(uri)
        attachment.size = file.length()
        viewModel.addAttachment(attachment, mediaAction)
      } catch (e: IOException) {
        Log.e(LOG_NAME, "Error copying document to local storage", e)

        val fileName = MediaUtility.getDisplayName(applicationContext, uri)
        val displayName = if (fileName.length > 12) "${fileName.substring(0, 12)}..." else fileName
        val message = String.format(resources.getString(R.string.observation_edit_invalid_attachment), displayName)
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
      }
    }
  }

  private fun onMediaAction(mediaAction: MediaAction) {
    attachmentMediaAction = mediaAction

    when (mediaAction.type) {
      MediaActionType.PHOTO -> requestImagePermission.launch(Manifest.permission.CAMERA)
      MediaActionType.VIDEO -> requestVideoPermission.launch(Manifest.permission.CAMERA)
      MediaActionType.VOICE -> requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
      MediaActionType.GALLERY -> launchGalleryIntent(mediaAction)
      MediaActionType.FILE -> launchFileIntent()
    }
  }

  private fun launchCameraIntent() {
    val file = File(
      getExternalFilesDir(Environment.DIRECTORY_PICTURES),
      "${UUID.randomUUID()}.jpg",
    )

    val uri = FileProvider.getUriForFile(applicationContext, applicationContext.packageName + ".fileprovider", file)

    currentMediaPath = file.absolutePath
    getImage.launch(uri)
  }

  private fun onMediaResult(result: Boolean) {
    if (result) {
      currentMediaPath?.let { path ->
        val uri = Uri.fromFile(File(path))
        val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
        MediaUtility.addImageToGallery(applicationContext, uri)

        val file = MediaUtility.copyMediaFromUri(applicationContext, uri)
        val attachment =
          Attachment()
        attachment.action = Media.ATTACHMENT_ADD_ACTION
        attachment.localPath = file.absolutePath
        attachment.name = file.name
        attachment.contentType =  mimeType
        attachment.size = file.length()

        viewModel.addAttachment(attachment, attachmentMediaAction)
      }
    }

    currentMediaPath = null
  }

  private fun launchVideoIntent() {
    val file = File(
      getExternalFilesDir(Environment.DIRECTORY_MOVIES),
      "${UUID.randomUUID()}.mp4"
    )

    currentMediaPath = file.absolutePath
    val uri = FileProvider.getUriForFile(this, application.packageName + ".fileprovider", file)
    getVideo.launch(uri)
  }

  private fun launchGalleryIntent(mediaAction: MediaAction?) {
    val fieldDefinition = viewModel.getAttachmentField(mediaAction)?.definition as? AttachmentFormField

    val types = if (fieldDefinition == null || fieldDefinition.allowedAttachmentTypes.isEmpty()) {
      arrayOf("image/*", "video/*")
    } else {
      val allowed = mutableListOf<String>()
      if (fieldDefinition.allowedAttachmentTypes.contains(AttachmentType.IMAGE)) {
        allowed.add("image/*")
      }
      if (fieldDefinition.allowedAttachmentTypes.contains(AttachmentType.VIDEO)) {
        allowed.add("video/*")
      }

      allowed.toTypedArray()
    }

    getDocument.launch(types)
  }

  private fun launchAudioIntent() {
    try {
      getAudio.launch()
    } catch(e: Exception) {
      Toast.makeText(applicationContext, "Device has no voice recorder application.", Toast.LENGTH_SHORT).show()
    }
  }

  private fun launchFileIntent() {
    getDocument.launch(arrayOf("*/*"))
  }

  private fun onFieldClick(fieldState: FieldState<*, *>) {
    when(fieldState) {
      is DateFieldState -> {
        val clearable = fieldState.definition.name != viewModel.observationState.value?.timestampFieldState?.definition?.name
        val dialog = DateFieldDialog.newInstance(fieldState.definition.title, fieldState.answer?.date ?: Date(), clearable)
        dialog.listener = object : DateFieldDialog.DateFieldDialogListener {
          override fun onDate(date: Date?) {
            fieldState.answer = if (date != null) FieldValue.Date(date) else null
          }
        }
        dialog.show(supportFragmentManager, "DIALOG_DATE_FIELD")
      }
      is GeometryFieldState -> {
        val clearable = fieldState.definition.name != viewModel.observationState.value?.geometryFieldState?.definition?.name
        val center = viewModel.observation.value?.geometry?.centroid ?: Point(0.0, 0.0)
        val dialog = GeometryFieldDialog.newInstance(
          title = fieldState.definition.title,
          location = fieldState.answer?.location,
          mapCenter = LatLng(center.y, center.x),
          clearable = clearable)

        dialog.listener = object : GeometryFieldDialog.GeometryFieldDialogListener {
          override fun onLocation(location: ObservationLocation?) {
            fieldState.answer = if (location != null) FieldValue.Location(ObservationLocation(location)) else null
          }
        }
        dialog.show(supportFragmentManager, "DIALOG_GEOMETRY_FIELD")
      }
      is SelectFieldState -> {
        val choices = (fieldState.definition as ChoiceFormField).choices.map { it.title }
        val dialog = newInstance(fieldState.definition.title, choices, fieldState.answer?.text)
        dialog.listener = object : SelectFieldDialog.SelectFieldDialogListener {
          override fun onSelect(choices: List<String>) {
            if (choices.isEmpty()) {
              fieldState.answer = null
            } else {
              fieldState.answer = FieldValue.Text(choices[0])
            }
          }
        }
        dialog.show(supportFragmentManager, "DIALOG_SELECT_FIELD")
      }
      is MultiSelectFieldState -> {
        val choices = (fieldState.definition as ChoiceFormField).choices.map { it.title }
        val dialog = newInstance(fieldState.definition.title, choices, fieldState.answer?.choices)
        dialog.listener = object : SelectFieldDialog.SelectFieldDialogListener {
          override fun onSelect(choices: List<String>) {
            if (choices.isEmpty()) {
              fieldState.answer = null
            } else {
              fieldState.answer = FieldValue.Multi(choices)
            }
          }
        }
        dialog.show(supportFragmentManager, "DIALOG_SELECT_FIELD")
      }
    }
  }

  private fun onAttachmentAction(action: AttachmentAction, attachment: Attachment, fieldState: FieldState<*, *>?) {
    when (action) {
      AttachmentAction.VIEW -> viewAttachment(attachment)
      AttachmentAction.DELETE -> deleteAttachment(attachment, fieldState)
    }
  }

  private fun viewAttachment(attachment: Attachment) {
    val intent = Intent(applicationContext, AttachmentViewActivity::class.java)

    if (attachment.id != null) {
      intent.putExtra(AttachmentViewActivity.ATTACHMENT_ID_EXTRA, attachment.id)
    } else {
      intent.putExtra(AttachmentViewActivity.ATTACHMENT_PATH_EXTRA, attachment.localPath)
    }

    startActivity(intent)
  }

  private fun deleteAttachment(attachment: Attachment, fieldState: FieldState<*, *>?) {
    viewModel.deleteAttachment(attachment, fieldState)
  }

  private fun pickForm() {
    val observationState = viewModel.observationState.value
    val totalMax = observationState?.definition?.maxObservationForms
    val totalForms = observationState?.forms?.value?.size ?: 0
    if (totalMax != null && totalForms >= totalMax) {
      Snackbar.make(findViewById(android.R.id.content), "Total number of forms in an observation cannot be more than $totalMax", Snackbar.LENGTH_LONG).show()
      return
    }

    val formPicker = FormPickerBottomSheetFragment()
    formPicker.formPickerListener = object : OnFormClickListener {
      override fun onFormPicked(form: Form) {
        val formMax = form.max
        val totalOfForm = observationState?.forms?.value?.filter { it.definition.id == form.id }?.size ?: 0
        if (formMax != null && totalOfForm >= formMax) {
          Snackbar.make(findViewById(android.R.id.content), "${form.name} cannot be included in an observation more than $formMax ${if (formMax > 1) "times" else "time"}.", Snackbar.LENGTH_LONG).show()
          return
        }

        viewModel.addForm(form)
      }
    }
    formPicker.show(supportFragmentManager, "DIALOG_FORM_PICKER")
  }

  private fun deleteForm(index: Int) {
    viewModel.deleteForm(index)
  }

  private fun reorderForms() {
    val dialog = FormReorderDialog.newInstance()
    dialog.listener = object : FormReorderDialog.FormReorderDialogListener {
      override fun onReorder(forms: List<FormState>) {
        viewModel.reorderForms(forms)
      }
    }
    dialog.show(supportFragmentManager, "DIALOG_FORM_REORDER")
  }

  companion object {
    private val LOG_NAME = ObservationEditActivity::class.java.name

    private const val DRAFT_OBSERVATION_ID = "DRAFT_OBSERVATION_ID"
    private const val DRAFT_OBSERVATION_JSON = "DRAFT_OBSERVATION_JSON"
    private const val CURRENT_MEDIA_PATH = "CURRENT_MEDIA_PATH"
    private const val ATTACHMENT_MEDIA_ACTION = "ATTACHMENT_MEDIA_ACTION"

    private const val NEW_OBSERVATION = -1L

    const val OBSERVATION_ID = "OBSERVATION_ID"
    const val LOCATION = "LOCATION"
    const val INITIAL_LOCATION = "INITIAL_LOCATION"
    const val INITIAL_ZOOM = "INITIAL_ZOOM"
  }
}