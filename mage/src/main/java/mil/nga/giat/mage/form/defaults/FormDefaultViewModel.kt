package mil.nga.giat.mage.form.defaults

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.JsonObject
import mil.nga.giat.mage.dagger.module.ApplicationContext
import mil.nga.giat.mage.form.*
import mil.nga.giat.mage.form.field.*
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import mil.nga.giat.mage.sdk.exceptions.EventException
import javax.inject.Inject

class FormDefaultViewModel @Inject constructor(
  @ApplicationContext val context: Context,
) : ViewModel() {

  private val _formState = MutableLiveData<FormState?>()
  val formState: LiveData<FormState?> = _formState

  private var formJson: JsonObject? = null
  private var formPreferences: FormPreferences? = null

  fun setForm(eventId: Long, formId: Long) {
    formPreferences = FormPreferences(context, eventId, formId)

    // TODO get this in background coroutine
    val eventHelper: EventHelper = EventHelper.getInstance(context)
    try {
      val event = eventHelper.read(eventId)
      formJson = event?.formMap?.get(formId)

      Form.fromJson(formJson)?.let { form ->
        val defaultForm = FormPreferences(context, event.id, form.id).getDefaults()
        _formState.value = FormState.fromForm(eventId = event.remoteId, form = form, defaultForm = defaultForm)

      }
    } catch (e: EventException) { }
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