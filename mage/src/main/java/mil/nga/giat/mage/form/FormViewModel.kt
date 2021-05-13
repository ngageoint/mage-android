package mil.nga.giat.mage.form

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.JsonObject
import mil.nga.giat.mage.dagger.module.ApplicationContext
import mil.nga.giat.mage.form.Form.Companion.fromJson
import mil.nga.giat.mage.observation.ObservationLocation
import mil.nga.giat.mage.observation.form.FieldState
import mil.nga.giat.mage.observation.form.FormState
import mil.nga.giat.mage.sdk.datastore.observation.Observation
import mil.nga.giat.mage.sdk.datastore.observation.ObservationForm
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import mil.nga.giat.mage.sdk.utils.GeometryUtility
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory
import java.util.*
import javax.inject.Inject

enum class FormMode {
  VIEW,
  EDIT;
}

class FormViewModel @Inject constructor(
  @ApplicationContext val context: Context
) : ViewModel() {

  data class FieldModel(val definition: FormField<Any>, val liveData: MutableLiveData<Any?>)
  data class FormModel(val definition: Form, val fieldModels: List<FieldModel>)

  var formMode = FormMode.VIEW
  private lateinit var fieldMap: Map<String, FormField<Any>>

  private val timestamp = MutableLiveData<FormField<Date>>()
  private val location = MutableLiveData<FormField<ObservationLocation>>()
  private val forms = MutableLiveData<List<FormModel>>(emptyList())

  fun setFieldValue(index: Int, fieldName: String, value: Any?) {
    forms.value?.get(index)?.let { form ->
      form.fieldModels.find { it.definition.name == fieldName }?.let { model ->
        model.liveData.value = value
      }
    }
  }

  fun getTimestamp(): LiveData<FormField<Date>> {
    return timestamp
  }

  fun setTimestamp(timestamp: FormField<Date>) {
    this.timestamp.value = timestamp
  }

  fun getLocation(): LiveData<FormField<ObservationLocation>> {
    return location
  }

  fun setLocation(location: FormField<ObservationLocation>) {
    this.location.value = location
  }

  fun getForms(): LiveData<List<FormModel>> {
    return forms
  }

  fun getFormDefinitions(): Collection<Form> {
    val jsonForms = EventHelper.getInstance(context).currentEvent.forms
    val forms = mutableListOf<Form>()
    for (jsonForm in jsonForms) {
      fromJson(jsonForm as JsonObject)?.let { form ->
          forms.add(form)
      }
    }

    return forms
  }


  fun initializeForms(observation: Observation): List<FormState> {
    val jsonForms = EventHelper.getInstance(context).currentEvent.forms
    val forms = mutableListOf<FormState>()
    for (jsonForm in jsonForms) {
      fromJson(jsonForm as JsonObject)?.let { form ->
        if (form.default) {
          val fields = mutableListOf<FieldState>()
//          val properties = mutableListOf<ObservationProperty>()
          for (field in form.fields) {
            val fieldState = FieldState(field)
            fieldState.value = field.value
            fields.add(fieldState)
//            properties.add(ObservationProperty(field.name, field.serialize()))
          }

//          val observationForm = ObservationForm()
//          observationForm.formId = form.id
//          observationForm.setObservation(observation)
//          observationForm.properties = properties

          val formState = FormState(form)
          formState.fields = fields
          forms.add(formState)
        }
      }
    }

    return forms
  }

  fun setForms(observationForms: Collection<ObservationForm>) {
    // TODO each form needs a form definition as well

    val forms = mutableListOf<FormModel>()
    for (observationForm in observationForms) {
      val formMap = EventHelper.getInstance(context).currentEvent.formMap
      val formJson = formMap[observationForm.formId]
      fromJson(formJson)?.let { form ->
        // TODO multi-form, do I still need this
//                val defaults: MutableMap<String, Any> = HashMap()
//                for ((key, value) in observationForm.propertiesMap) {
//                    defaults[key] = value.value
//                }
//
//                setUserDefaults(form, defaults)
//
//                form.fields.map { it.name to it }.toMap().let {
//                    fieldMap = it
//                }

//        forms.add(FormWithModel(form, observationForm))
      }
    }

    this.forms.value = forms
  }

  fun addForm(observationForm: ObservationForm) {
    val formMap = EventHelper.getInstance(context).currentEvent.formMap
    val formJson = formMap[observationForm.formId]
    fromJson(formJson)?.let { form ->
      val forms = this.forms.value?.toMutableList() ?: mutableListOf()
//      forms.add(FormWithModel(form, observationForm))
      this.forms.value = forms
    }
  }

  fun getField(key: String): FormField<Any>? {
    return fieldMap[key]
  }

//  private fun setUserDefaults(form: Form, defaults: Map<String, Any?>) {
//    if (defaults.isEmpty()) return
//
//    form.fields.let { fields ->
//      for (field in fields) {
//        setValue(field, defaults.get(field.name))
//      }
//    }
//  }

  private fun setValue(field: FormField<Any>, value: Any?) {
    when (field.type) {
      FieldType.DATE ->
        if (value is String) {
          field.value = ISO8601DateFormatFactory.ISO8601().parse(value)
        } else if (value is Date) {
          field.value = value
        }
      FieldType.GEOMETRY ->
        when (value) {
          is ObservationLocation -> field.value = value
          is ByteArray -> field.value = ObservationLocation(
            ObservationLocation.MANUAL_PROVIDER,
            GeometryUtility.toGeometry(value)
          )
        }
      else -> field.value = value
    }
  }
}