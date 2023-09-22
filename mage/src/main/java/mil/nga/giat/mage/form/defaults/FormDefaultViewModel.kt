package mil.nga.giat.mage.form.defaults

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mil.nga.giat.mage.form.Form
import mil.nga.giat.mage.form.FormField
import mil.nga.giat.mage.form.FormState
import mil.nga.giat.mage.form.field.*
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.sdk.exceptions.EventException
import javax.inject.Inject

@HiltViewModel
class FormDefaultViewModel @Inject constructor(
  val application: Application,
  private val eventLocalDataSource: EventLocalDataSource
) : ViewModel() {

  val event = eventLocalDataSource.currentEvent

  private val _formState = MutableLiveData<FormState?>()
  val formState: LiveData<FormState?> = _formState

  private var formJson: String? = null
  private var formPreferences: FormPreferences? = null

  fun setForm(eventId: Long, formId: Long) {
    formPreferences = FormPreferences(application, eventId, formId)

    // TODO get this in background coroutine
    try {
      val event = eventLocalDataSource.read(eventId)
      formJson = eventLocalDataSource.getForm(formId)?.json
      Form.fromJson(formJson)?.let { form ->
        val defaultForm = FormPreferences(application, event.id, form.id).getDefaults()
        _formState.value = FormState.fromForm(eventId = event.remoteId, form = form, defaultForm = defaultForm)
      }

    } catch (_: EventException) { }
  }

  fun saveDefaults() {
    formPreferences?.let { preferences ->
      Form.fromJson(formJson)?.let { defaultForm ->
        val fieldStates = _formState.value?.fields?.toMutableList() ?: mutableListOf()
        for (fieldState in fieldStates) {
          defaultForm.fields.find { it.name == fieldState.definition.name }?.apply {
            value = fieldStateValue(fieldState)
          }
        }

        val transform : (FormField<Any>) -> Pair<String, FormField<Any>> = { it.name to it }
        val formMap = defaultForm.fields.associateTo(mutableMapOf(), transform)

        Form.fromJson(formJson)?.let { serverForm ->
          val defaultFormMap = serverForm.fields.associateTo(mutableMapOf(), transform)
          if (formMap == defaultFormMap) {
            preferences.clearDefaults()
          } else {
            preferences.saveDefaults(defaultForm)
          }
        }
      }
    }
  }

  fun resetDefaults() {
    _formState.value?.let { formState ->
      Form.fromJson(formJson)?.let { form ->
        _formState.value = FormState.fromForm(eventId = formState.eventId, form = form)
      }
    }
  }

  private fun fieldStateValue(fieldState: FieldState<*, *>): Any? {
    return when(fieldState) {
      is BooleanFieldState -> {
        fieldState.answer?.boolean
      }
      is DateFieldState -> {
        fieldState.answer?.date
      }
      is EmailFieldState -> {
        fieldState.answer?.text
      }
      is GeometryFieldState -> {
        fieldState.answer?.location
      }
      is MultiSelectFieldState -> {
        fieldState.answer?.choices
      }
      is NumberFieldState -> {
        fieldState.answer?.number
      }
      is RadioFieldState -> {
        fieldState.answer?.text
      }
      is SelectFieldState -> {
        fieldState.answer?.text
      }
      is TextFieldState -> {
        fieldState.answer?.text
      }
      else -> null
    }
  }
}