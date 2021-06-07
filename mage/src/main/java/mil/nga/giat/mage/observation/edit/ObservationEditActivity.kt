package mil.nga.giat.mage.observation.edit

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import dagger.android.support.DaggerAppCompatActivity
import mil.nga.giat.mage.BuildConfig
import mil.nga.giat.mage.R
import mil.nga.giat.mage._server5.observation.edit.FormViewModel_server5
import mil.nga.giat.mage.form.*
import mil.nga.giat.mage.observation.AttachmentViewerActivity
import mil.nga.giat.mage.observation.ObservationLocation
import mil.nga.giat.mage.observation.edit.FormPickerBottomSheetFragment.OnFormClickListener
import mil.nga.giat.mage.form.edit.dialog.DateFieldDialog
import mil.nga.giat.mage.form.edit.dialog.FormReorderDialog
import mil.nga.giat.mage.form.edit.dialog.GeometryFieldDialog
import mil.nga.giat.mage.form.edit.dialog.SelectFieldDialog
import mil.nga.giat.mage.form.edit.dialog.SelectFieldDialog.Companion.newInstance
import mil.nga.giat.mage.form.field.*
import mil.nga.giat.mage.sdk.Compatibility.Companion.isServerVersion5
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import mil.nga.giat.mage.sdk.utils.MediaUtility
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject

open class ObservationEditActivity : DaggerAppCompatActivity() {
  companion object {
    private val LOG_NAME = ObservationEditActivity::class.java.name

    const val OBSERVATION_ID = "OBSERVATION_ID"
    const val LOCATION = "LOCATION"
    const val INITIAL_LOCATION = "INITIAL_LOCATION"
    const val INITIAL_ZOOM = "INITIAL_ZOOM"

    private const val CURRENT_MEDIA_PATH = "CURRENT_MEDIA_PATH"

    private const val PERMISSIONS_REQUEST_CAMERA = 100
    private const val PERMISSIONS_REQUEST_VIDEO = 200
    private const val PERMISSIONS_REQUEST_AUDIO = 300
    private const val PERMISSIONS_REQUEST_STORAGE = 400

    private const val CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100
    private const val CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200
    private const val CAPTURE_VOICE_ACTIVITY_REQUEST_CODE = 300
    private const val GALLERY_ACTIVITY_REQUEST_CODE = 400

    private const val NEW_OBSERVATION = -1L
  }

  @Inject
  lateinit var viewModelFactory: ViewModelProvider.Factory
  protected lateinit var viewModel: FormViewModel

  private var currentMediaPath: String? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    viewModel = if (isServerVersion5(applicationContext)) {
      ViewModelProvider(this, viewModelFactory).get(FormViewModel_server5::class.java)
    } else {
      ViewModelProvider(this, viewModelFactory).get(FormViewModel::class.java)
    }

    val defaultMapLatLng = intent.getParcelableExtra(INITIAL_LOCATION) ?: LatLng(0.0, 0.0)
    val defaultMapZoom = intent.getFloatExtra(INITIAL_ZOOM, 0.0f)

    val observationId = intent.getLongExtra(OBSERVATION_ID, NEW_OBSERVATION)
    if (observationId == NEW_OBSERVATION) {
      val location: ObservationLocation = intent.getParcelableExtra(LOCATION)!!
      val showFormPicker = viewModel.createObservation(Date(), location, defaultMapZoom, defaultMapLatLng)
      if (showFormPicker) { pickForm() }
    } else {
      viewModel.setObservation(observationId)
    }

    setContent {
      ObservationEditScreen(
        viewModel = viewModel,
        onSave = { save() },
        onCancel = { cancel() },
        onAction = { onAction(it) },
        onAddForm = { pickForm() },
        onDeleteForm = { deleteForm(it) },
        onReorderForms = { reorderForms() },
        onFieldClick = { fieldState ->  onFieldClick(fieldState = fieldState) },
        onAttachmentClick = { attachment ->  onAttachmentClick(attachment) }
      )
    }
  }

  override fun onBackPressed() {
    cancel()
  }

  override fun onRestoreInstanceState(savedInstanceState: Bundle) {
    super.onRestoreInstanceState(savedInstanceState)

    savedInstanceState.getParcelableArrayList<Attachment>("attachmentsToCreate")?.let {
      for (attachment in it) {
        viewModel.addAttachment(attachment)
      }
    }

    currentMediaPath = savedInstanceState.getString(CURRENT_MEDIA_PATH)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)

    val attachments = arrayListOf<Attachment>().apply {
      addAll(viewModel.attachments)
    }
    outState.putParcelableArrayList("attachmentsToCreate", attachments)

    outState.putString(CURRENT_MEDIA_PATH, currentMediaPath)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (resultCode != RESULT_OK) {
      return
    }

    when (requestCode) {
      CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE -> {
        val attachment = Attachment()
        attachment.localPath = currentMediaPath
        MediaUtility.addImageToGallery(applicationContext, Uri.fromFile(File(currentMediaPath)))
        viewModel.addAttachment(attachment)
      }
      GALLERY_ACTIVITY_REQUEST_CODE, CAPTURE_VOICE_ACTIVITY_REQUEST_CODE -> {
        val uris: Collection<Uri> = getUris(data)
        for (uri in uris) {
          try {
            val file = MediaUtility.copyMediaFromUri(applicationContext, uri)
            val attachment = Attachment()
            attachment.localPath = file.absolutePath
            viewModel.addAttachment(attachment)
          } catch (e: IOException) {
            Log.e(LOG_NAME, "Error copying gallery file to local storage", e)
          }
        }
      }
    }
  }

  private fun getUris(intent: Intent?): Collection<Uri> {
    val uris: MutableSet<Uri> = HashSet()
    intent?.data?.let { uris.add(it) }

    uris.addAll(getClipDataUris(intent))
    return uris
  }

  @TargetApi(16)
  private fun getClipDataUris(intent: Intent?): Collection<Uri> {
    val uris: MutableCollection<Uri> = ArrayList()
    val cd = intent?.clipData
    if (cd != null) {
      for (i in 0 until cd.itemCount) {
        uris.add(cd.getItemAt(i).uri)
      }
    }
    return uris
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    when (requestCode) {
      PERMISSIONS_REQUEST_CAMERA, PERMISSIONS_REQUEST_VIDEO -> {
        val grants: MutableMap<String, Int> = HashMap()
        grants[Manifest.permission.CAMERA] = PackageManager.PERMISSION_GRANTED
        grants[Manifest.permission.WRITE_EXTERNAL_STORAGE] = PackageManager.PERMISSION_GRANTED
        for (i in grantResults.indices) {
          grants[permissions[i]] = grantResults[i]
        }

        if (grants[Manifest.permission.CAMERA] == PackageManager.PERMISSION_GRANTED &&
          grants[Manifest.permission.WRITE_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED) {
          if (requestCode == PERMISSIONS_REQUEST_CAMERA) {
            launchCameraIntent()
          } else {
            launchVideoIntent()
          }
        } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) && grants[Manifest.permission.WRITE_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED ||
          !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) && grants[Manifest.permission.CAMERA] == PackageManager.PERMISSION_GRANTED ||
          !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
          // User denied camera or storage with never ask again.  Since they will get here
          // by clicking the camera button give them a dialog that will
          // guide them to settings if they want to enable the permission
          showDisabledPermissionsDialog(
            resources.getString(R.string.camera_access_title),
            resources.getString(R.string.camera_access_message))
        }
      }
      PERMISSIONS_REQUEST_AUDIO -> {
        val grants: MutableMap<String, Int> = HashMap()
        grants[Manifest.permission.RECORD_AUDIO] = PackageManager.PERMISSION_GRANTED
        grants[Manifest.permission.WRITE_EXTERNAL_STORAGE] = PackageManager.PERMISSION_GRANTED
        if (grants[Manifest.permission.RECORD_AUDIO] == PackageManager.PERMISSION_GRANTED && grants[Manifest.permission.WRITE_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED) {
          launchAudioIntent()
        } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO) && grants[Manifest.permission.WRITE_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED ||
          !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) && grants[Manifest.permission.RECORD_AUDIO] == PackageManager.PERMISSION_GRANTED ||
          !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO) && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
          // User denied camera or storage with never ask again.  Since they will get here
          // by clicking the camera button give them a dialog that will
          // guide them to settings if they want to enable the permission
          showDisabledPermissionsDialog(
            resources.getString(R.string.camera_access_title),
            resources.getString(R.string.camera_access_message))
        }
      }
      PERMISSIONS_REQUEST_STORAGE -> {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          launchGalleryIntent()
        } else {
          if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            // User denied storage with never ask again.  Since they will get here
            // by clicking the gallery button give them a dialog that will
            // guide them to settings if they want to enable the permission
            showDisabledPermissionsDialog(
              resources.getString(R.string.gallery_access_title),
              resources.getString(R.string.gallery_access_message))
          }
        }
      }
    }
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

  private fun onAction(mediaAction: ObservationMediaAction) {
    when (mediaAction) {
      ObservationMediaAction.PHOTO -> onCameraAction()
      ObservationMediaAction.VIDEO -> onVideoAction()
      ObservationMediaAction.GALLERY -> onGalleryAction()
      ObservationMediaAction.VOICE -> onVoiceAction()
    }
  }

  private fun onCameraAction() {
    if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
      ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
    ) {
      ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_CAMERA)
    } else {
      launchCameraIntent()
    }
  }

  private fun onVideoAction() {
    if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
      ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
    ) {
      ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_VIDEO)
    } else {
      launchVideoIntent()
    }
  }

  private fun onGalleryAction() {
    if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_STORAGE)
    } else {
      launchGalleryIntent()
    }
  }

  private fun onVoiceAction() {
    if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
      ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
    ) {
      ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_AUDIO)
    } else {
      launchAudioIntent()
    }
  }

  private fun launchCameraIntent() {
    try {
      val file = MediaUtility.createImageFile()
      currentMediaPath = file.absolutePath
      val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
      intent.putExtra(MediaStore.EXTRA_OUTPUT, getUriForFile(file))
      intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
      startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE)
    } catch (e: IOException) {
      Log.e(LOG_NAME, "Error creating video media file", e)
    }
  }

  private fun launchVideoIntent() {
    try {
      val file = MediaUtility.createVideoFile()
      currentMediaPath = file.absolutePath
      val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
      intent.putExtra(MediaStore.EXTRA_OUTPUT, getUriForFile(file))
      intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
      startActivityForResult(intent, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE)
    } catch (e: IOException) {
      Log.e(LOG_NAME, "Error creating video media file", e)
    }
  }

  private fun launchGalleryIntent() {
    val intent = Intent(Intent.ACTION_GET_CONTENT)
    intent.type = "image/*, video/*"
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    startActivityForResult(intent, GALLERY_ACTIVITY_REQUEST_CODE)
  }

  private fun launchAudioIntent() {
    val intent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
    val resolveInfo = applicationContext.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    if (resolveInfo.size > 0) {
      startActivityForResult(intent, CAPTURE_VOICE_ACTIVITY_REQUEST_CODE)
    } else {
      Toast.makeText(applicationContext, "Device has no voice recorder application.", Toast.LENGTH_SHORT).show()
    }
  }

  private fun getUriForFile(file: File): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", file)
    } else {
      Uri.fromFile(file)
    }
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
        val dialog = GeometryFieldDialog.newInstance(fieldState.definition.title, fieldState.answer?.location, clearable)
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

  private fun onAttachmentClick(attachment: Attachment) {
    val intent = Intent(applicationContext, AttachmentViewerActivity::class.java)

    if (attachment.id != null) {
      intent.putExtra(AttachmentViewerActivity.ATTACHMENT_ID, attachment.id)
    } else {
      intent.putExtra(AttachmentViewerActivity.ATTACHMENT_PATH, attachment.localPath)
    }

    intent.putExtra(AttachmentViewerActivity.EDITABLE, false)
    startActivity(intent)
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
}