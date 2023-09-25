package mil.nga.giat.mage.observation.view

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.R
import mil.nga.giat.mage.databinding.ViewMoreBottomSheetBinding
import mil.nga.giat.mage.form.FormState
import mil.nga.giat.mage.form.FormViewModel
import mil.nga.giat.mage.form.edit.dialog.FormReorderDialog
import mil.nga.giat.mage.observation.attachment.AttachmentViewActivity
import mil.nga.giat.mage.observation.edit.ObservationEditActivity
import mil.nga.giat.mage.people.PeopleActivity
import mil.nga.giat.mage.database.model.observation.Attachment
import mil.nga.giat.mage.sdk.datastore.user.Permission
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.utils.googleMapsUri
import javax.inject.Inject

@AndroidEntryPoint
class ObservationViewActivity : AppCompatActivity() {

  companion object {
    private val LOG_NAME = ObservationViewActivity::class.java.name

    const val OBSERVATION_RESULT_TYPE ="OBSERVATION_RESULT_TYPE"
    const val OBSERVATION_ID_EXTRA = "OBSERVATION_ID"
    const val INITIAL_LOCATION_EXTRA = "INITIAL_LOCATION"
    const val INITIAL_ZOOM_EXTRA = "INITIAL_ZOOM"
  }

  enum class ResultType { NAVIGATE }

  private var currentUser: User? = null
  private var hasEventUpdatePermission = false

  private var defaultMapZoom: Float? = null
  private var defaultMapCenter: LatLng? = null

  private lateinit var viewModel: FormViewModel

  @Inject lateinit var userLocalDataSource: UserLocalDataSource

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    viewModel = ViewModelProvider(this).get(FormViewModel::class.java)

    defaultMapZoom = intent.getFloatExtra(INITIAL_ZOOM_EXTRA, 0.0f)
    defaultMapCenter = intent.getParcelableExtra(INITIAL_LOCATION_EXTRA) ?: LatLng(0.0, 0.0)

    require(intent?.getLongExtra(OBSERVATION_ID_EXTRA, -1L) != -1L) { "OBSERVATION_ID is required to launch ObservationViewActivity" }
    val observationId = intent.getLongExtra(OBSERVATION_ID_EXTRA, -1L)
    viewModel.setObservation(observationId, observeChanges = true, defaultMapZoom, defaultMapCenter)

    try {
      currentUser = userLocalDataSource.readCurrentUser()
      hasEventUpdatePermission = currentUser?.role?.permissions?.permissions?.contains(
         Permission.UPDATE_EVENT) ?: false
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
      is ObservationAction.Edit -> onEditObservation()
      is ObservationAction.Important -> onFlagObservation(action)
      is ObservationAction.Favorite -> onFavoriteObservation()
      is ObservationAction.FavoriteBy -> onFavoritedBy()
      is ObservationAction.Directions -> onDirections()
      is ObservationAction.More -> onMore()
      is ObservationAction.Sync -> onSync()
    }
  }

  private fun onEditObservation() {
    if (!userLocalDataSource.isCurrentUserPartOfCurrentEvent()) {
      AlertDialog.Builder(this)
        .setTitle(R.string.no_event_title)
        .setMessage(R.string.observation_no_event_edit_message)
        .setPositiveButton(android.R.string.ok) { _, _ -> }.show()
      return
    }

    val intent = Intent(this, ObservationEditActivity::class.java)
    intent.putExtra(ObservationEditActivity.OBSERVATION_ID, viewModel.observation.value?.id)
    intent.putExtra(ObservationEditActivity.INITIAL_LOCATION, defaultMapCenter)
    intent.putExtra(ObservationEditActivity.INITIAL_ZOOM, defaultMapZoom)

    startActivity(intent)
  }

  private fun onFlagObservation(action: ObservationAction.Important) {
    if (action.type == ObservationAction.Important.Type.FLAG) {
      viewModel.flagObservation(action.description)
    } else if (action.type == ObservationAction.Important.Type.REMOVE) {
      viewModel.unflagObservation()
    }
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
    viewModel.observation.value?.let { observation ->
      AlertDialog.Builder(this)
        .setTitle(application.resources.getString(R.string.navigation_choice_title))
        .setItems(R.array.navigationOptions) { _: DialogInterface?, which: Int ->
          when (which) {
            0 -> {
              val intent = Intent(Intent.ACTION_VIEW, observation.geometry.googleMapsUri())
              startActivity(intent)
            }
            1 -> {
              val intent = Intent()
              intent.putExtra(OBSERVATION_ID_EXTRA, observation.id)
              intent.putExtra(OBSERVATION_RESULT_TYPE, ResultType.NAVIGATE)
              setResult(Activity.RESULT_OK, intent)
              finish()
            }
          }
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
    }
  }

  private fun onLocationClick(location: String) {
    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
    val clip = ClipData.newPlainText("Observation Location", location)
    if (clipboard == null || clip == null) return
    clipboard.setPrimaryClip(clip)

    Snackbar.make(findViewById(android.R.id.content), R.string.location_text_copy_message, Snackbar.LENGTH_SHORT).show()
  }

  private fun onAttachmentClick(attachment: Attachment) {
    val intent = Intent(applicationContext, AttachmentViewActivity::class.java)
    intent.putExtra(AttachmentViewActivity.ATTACHMENT_ID_EXTRA, attachment.id)
    startActivity(intent)
  }

  private fun onMore() {
    val dialog = BottomSheetDialog(this)
    val binding = ViewMoreBottomSheetBinding.inflate(layoutInflater, null, false)

    val formCount = viewModel.observationState.value?.forms?.value?.size ?: 0
    binding.reorder.visibility = if (formCount > 1) View.VISIBLE else View.GONE

    binding.delete.setOnClickListener {
      onDeleteObservation()
      dialog.dismiss()
    }

    binding.edit.setOnClickListener {
      onEditObservation()
      dialog.dismiss()
    }

    binding.reorder.setOnClickListener {
      dialog.dismiss()
      val reorderDialog = FormReorderDialog.newInstance()
      reorderDialog.listener = object : FormReorderDialog.FormReorderDialogListener {
        override fun onReorder(forms: List<FormState>) {
          viewModel.reorderForms(forms)
        }
      }
      reorderDialog.show(supportFragmentManager, "DIALOG_FORM_REORDER")
    }

    dialog.setContentView(binding.root)
    dialog.show()
  }

  private fun onSync() {
    viewModel.syncObservation()
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