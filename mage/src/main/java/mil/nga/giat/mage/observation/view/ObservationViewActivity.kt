package mil.nga.giat.mage.observation.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import mil.nga.giat.mage.R
import mil.nga.giat.mage.observation.AttachmentViewerActivity
import mil.nga.giat.mage.observation.ImportantDialog
import mil.nga.giat.mage.observation.ImportantRemoveDialog
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import mil.nga.giat.mage.sdk.datastore.user.Permission
import mil.nga.giat.mage.sdk.datastore.user.User
import mil.nga.giat.mage.sdk.datastore.user.UserHelper
import java.lang.Exception
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.view_more_bottom_sheet.view.*
import mil.nga.giat.mage.form.FormState
import mil.nga.giat.mage.form.FormViewModel
import mil.nga.giat.mage.form.edit.dialog.FormReorderDialog
import mil.nga.giat.mage.observation.edit.ObservationEditActivity
import mil.nga.giat.mage.people.PeopleActivity

@AndroidEntryPoint
class ObservationViewActivity : AppCompatActivity() {

  companion object {
    private val LOG_NAME = ObservationViewActivity::class.java.name

    const val OBSERVATION_ID = "OBSERVATION_ID"
    const val INITIAL_LOCATION = "INITIAL_LOCATION"
    const val INITIAL_ZOOM = "INITIAL_ZOOM"
  }

  private var currentUser: User? = null
  private var hasEventUpdatePermission = false

  private var defaultMapZoom: Float? = null
  private var defaultMapCenter: LatLng? = null

  private lateinit var viewModel: FormViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    viewModel = ViewModelProvider(this).get(FormViewModel::class.java)

    defaultMapZoom = intent.getFloatExtra(INITIAL_ZOOM, 0.0f)
    defaultMapCenter = intent.getParcelableExtra(INITIAL_LOCATION) ?: LatLng(0.0, 0.0)

    require(intent?.getLongExtra(OBSERVATION_ID, -1L) != -1L) { "OBSERVATION_ID is required to launch ObservationViewActivity" }
    val observationId = intent.getLongExtra(OBSERVATION_ID, -1L)
    viewModel.setObservation(observationId, observeChanges = true, defaultMapZoom, defaultMapCenter)

    try {
      currentUser = UserHelper.getInstance(this).readCurrentUser()
      hasEventUpdatePermission = currentUser?.role?.permissions?.permissions?.contains(Permission.UPDATE_EVENT) ?: false
    } catch (e: Exception) {
      Log.e(LOG_NAME, "Cannot read current user")
    }
    
    setContent { 
      ObservationViewScreen(
        viewModel = viewModel,
        onClose = { finish() },
        onAction = { onAction(it) },
        onLocationClick = { onLocationClick(it) },
        onAttachmentClick = { onAttachmentClick(it) }
      )
    }
  }

  private fun onAction(action: ObservationAction) {
    when (action) {
      ObservationAction.EDIT -> onEditObservation()
      ObservationAction.FLAG -> onFlagObservation()
      ObservationAction.FAVORITE -> onFavoriteObservation()
      ObservationAction.FAVORITED_BY -> onFavoritedBy()
      ObservationAction.DIRECTIONS -> onDirections()
      ObservationAction.MORE -> onMore()
    }
  }

  private fun onEditObservation() {
    if (!UserHelper.getInstance(applicationContext).isCurrentUserPartOfCurrentEvent) {
      AlertDialog.Builder(this)
        .setTitle("Not a member of this event")
        .setMessage("You are an administrator and not a member of the current event.  You can not edit this observation.")
        .setPositiveButton(android.R.string.ok) { _, _ -> }.show()
      return
    }

    val intent = Intent(this, ObservationEditActivity::class.java)
    intent.putExtra(ObservationEditActivity.OBSERVATION_ID, viewModel.observation.value?.id)
    intent.putExtra(ObservationEditActivity.INITIAL_LOCATION, defaultMapCenter)
    intent.putExtra(ObservationEditActivity.INITIAL_ZOOM, defaultMapZoom)

    startActivity(intent)
  }

  private fun onFlagObservation() {
    val important = viewModel.observationState.value?.important?.value
    if (important != null) {
      val dialog = BottomSheetDialog(this)
      val view = layoutInflater.inflate(R.layout.view_important_bottom_sheet, null)
      view.findViewById<View>(R.id.update_button).setOnClickListener {
        onUpdateFlag()
        dialog.dismiss()
      }
      view.findViewById<View>(R.id.remove_button).setOnClickListener {
        onRemoveFlag()
        dialog.dismiss()
      }
      dialog.setContentView(view)
      dialog.show()
    } else {
      val dialog = ImportantDialog.newInstance(important)
      dialog.setOnImportantListener { description: String? ->
        viewModel.flagObservation(description)
      }

      dialog.show(supportFragmentManager, "OBSERVATION_IMPORTANT")
    }
  }

  private fun onUpdateFlag() {
    val important = viewModel.observationState.value?.important?.value
    val dialog = ImportantDialog.newInstance(important?.description)
    dialog.setOnImportantListener { description: String? ->
      viewModel.flagObservation(description)
    }

    dialog.show(supportFragmentManager, "OBSERVATION_IMPORTANT")
  }

  private fun onRemoveFlag() {
    val dialog = ImportantRemoveDialog()
    dialog.setOnRemoveImportantListener {
      viewModel.unflagObservation()
    }

    dialog.show(supportFragmentManager, "OBSERVATION_IMPORTANT")
  }

  private fun onFavoritedBy() {
    val userIds = viewModel.observation.value?.favorites?.map { it.userId } ?: listOf()
    val intent = Intent(this, PeopleActivity::class.java)
    intent.putStringArrayListExtra(PeopleActivity.USER_REMOTE_IDS, ArrayList(userIds))
    startActivity(intent)
  }

  private fun onFavoriteObservation() {
    viewModel.toggleFavorite()
  }

  private fun onDirections() {
    val observation = viewModel.observation.value
    val intent = Intent(Intent.ACTION_VIEW, observation?.googleMapsUri)
    startActivity(intent)
  }

  private fun onLocationClick(location: String) {
    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Observation Location", location)
    if (clipboard == null || clip == null) return
    clipboard.setPrimaryClip(clip)

    Snackbar.make(findViewById(android.R.id.content), R.string.location_text_copy_message, Snackbar.LENGTH_SHORT).show()
  }

  private fun onAttachmentClick(attachment: Attachment) {
    val intent = Intent(applicationContext, AttachmentViewerActivity::class.java)
    intent.putExtra(AttachmentViewerActivity.ATTACHMENT_ID, attachment.id)
    intent.putExtra(AttachmentViewerActivity.EDITABLE, false)
    startActivity(intent)
  }

  private fun onMore() {
    val dialog = BottomSheetDialog(this)
    val view = layoutInflater.inflate(R.layout.view_more_bottom_sheet, null)

    val formCount = viewModel.observationState.value?.forms?.value?.size ?: 0
    view.reorder.visibility = if (formCount > 1) View.VISIBLE else View.GONE

    view.delete.setOnClickListener {
      onDeleteObservation()
      dialog.dismiss()
    }

    view.edit.setOnClickListener {
      onEditObservation()
      dialog.dismiss()
    }

    view.reorder.setOnClickListener {
      dialog.dismiss()
      val reorderDialog = FormReorderDialog.newInstance()
      reorderDialog.listener = object : FormReorderDialog.FormReorderDialogListener {
        override fun onReorder(forms: List<FormState>) {
          viewModel.reorderForms(forms)
        }
      }
      reorderDialog.show(supportFragmentManager, "DIALOG_FORM_REORDER")
    }

    dialog.setContentView(view)
    dialog.show()
  }

  private fun onDeleteObservation() {
    AlertDialog.Builder(this)
      .setTitle("Delete Observation")
      .setMessage("Are you sure you want to remove this observation?")
      .setPositiveButton("Delete") { _, _ ->
        viewModel.deleteObservation()
        finish()
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }
}