package mil.nga.giat.mage.form.defaults

import android.content.Context
import android.content.Intent
import android.os.Bundle

import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.form.ChoiceFormField
import mil.nga.giat.mage.form.Form

import mil.nga.giat.mage.form.edit.dialog.DateFieldDialog
import mil.nga.giat.mage.form.edit.dialog.GeometryFieldDialog
import mil.nga.giat.mage.form.edit.dialog.SelectFieldDialog
import mil.nga.giat.mage.form.field.*
import mil.nga.giat.mage.observation.ObservationLocation
import mil.nga.giat.mage.sdk.datastore.user.Event
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class FormDefaultActivity : AppCompatActivity() {

  companion object {
    private const val EVENT_ID_EXTRA = "EVENT_ID_EXTRA"
    private const val FORM_ID_EXTRA = "FORM_ID_EXTRA"

    fun intent(context: Context, event: Event, form: Form): Intent {
      val intent = Intent(context, FormDefaultActivity::class.java)
      intent.putExtra(EVENT_ID_EXTRA, event.id)
      intent.putExtra(FORM_ID_EXTRA, form.id)
      return intent
    }
  }

  private lateinit var viewModel: FormDefaultViewModel

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    require(intent.hasExtra(EVENT_ID_EXTRA)) { "EVENT_ID_EXTRA is required to launch FormDefaultActivity" }
    require(intent.hasExtra(FORM_ID_EXTRA)) { "FORM_ID_EXTRA is required to launch FormDefaultActivity" }

    viewModel = ViewModelProvider(this).get(FormDefaultViewModel::class.java)

    viewModel.setForm(
      eventId = intent.getLongExtra(EVENT_ID_EXTRA, 0),
      formId = intent.getLongExtra(FORM_ID_EXTRA, 0)
    )

    setContent {
      FormDefaultScreen(
        formStateLiveData = viewModel.formState,
        onClose = { finish() },
        onSave = { saveDefaults() },
        onReset = { resetDefaults() },
        onFieldClick = { fieldState ->  onFieldClick(fieldState = fieldState) },
      )
    }
  }

  private fun saveDefaults() {
    viewModel.saveDefaults()
    finish()
  }

  private fun resetDefaults() {
    viewModel.resetDefaults()
  }

  private fun onFieldClick(fieldState: FieldState<*, *>) {
    when(fieldState) {
      is DateFieldState -> {
        val dialog = DateFieldDialog.newInstance(fieldState.definition.title, fieldState.answer?.date ?: Date())
        dialog.listener = object : DateFieldDialog.DateFieldDialogListener {
          override fun onDate(date: Date?) {
            fieldState.answer = if (date != null) FieldValue.Date(date) else null
          }
        }
        dialog.show(supportFragmentManager, "DIALOG_DATE_FIELD")
      }
      is GeometryFieldState -> {
        val dialog = GeometryFieldDialog.newInstance(fieldState.definition.title, fieldState.answer?.location)
        dialog.listener = object : GeometryFieldDialog.GeometryFieldDialogListener {
          override fun onLocation(location: ObservationLocation?) {
            fieldState.answer = if (location != null) FieldValue.Location(ObservationLocation(location)) else null
          }
        }
        dialog.show(supportFragmentManager, "DIALOG_GEOMETRY_FIELD")
      }
      is SelectFieldState -> {
        val choices = (fieldState.definition as ChoiceFormField).choices.map { it.title }
        val dialog = SelectFieldDialog.newInstance(
          fieldState.definition.title,
          choices,
          fieldState.answer?.text
        )
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
        val dialog = SelectFieldDialog.newInstance(
          fieldState.definition.title,
          choices,
          fieldState.answer?.choices
        )
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
}
