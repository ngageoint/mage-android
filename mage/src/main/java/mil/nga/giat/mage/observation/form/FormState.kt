package mil.nga.giat.mage.observation.form

import android.os.Parcelable
import androidx.compose.runtime.mutableStateOf
import mil.nga.giat.mage.form.Form
import androidx.compose.runtime.*
import kotlinx.android.parcel.Parcelize
import mil.nga.giat.mage.form.FormField
import mil.nga.giat.mage.sdk.datastore.observation.Observation

class FieldState(val definition: FormField<Any>) {
  var value: Any? by mutableStateOf(null)
}

class FormState(val definition: Form) {
  var fields: List<FieldState> by mutableStateOf(emptyList<FieldState>())

  companion object {
    fun fromObservation(observation: Observation, formDefinitions: Collection<Form>): MutableList<FormState> {
      val forms = mutableListOf<FormState>()
      for (definition in formDefinitions) {
        if (definition.default) {
          val fields = mutableListOf<FieldState>()
          for (field in definition.fields) {
            val fieldState = FieldState(field)
            fieldState.value = field.value
            fields.add(fieldState)
          }

          val formState = FormState(definition)
          formState.fields = fields
          forms.add(formState)
        }
      }

      return forms
    }
  }
}

@Parcelize
class FormsState(): Parcelable {
  private var forms: MutableList<FormState> = mutableListOf()

  private constructor(forms: MutableList<FormState>): this() {
    this.forms = forms
  }

  fun forms(): List<FormState> {
    return forms
  }

  fun addForm(formDefinition: Form) {
    val fields = mutableListOf<FieldState>()
    for (field in formDefinition.fields) {
      val fieldState = FieldState(field)
      fieldState.value = field.value
      fields.add(fieldState)
    }

    val formState = FormState(formDefinition)
    formState.fields = fields
    forms.add(formState)
  }

  fun removeForm(index: Int) {

  }

  companion object {
    fun fromNew(formDefinitions: Collection<Form>): FormsState {
      val forms = mutableListOf<FormState>()
      for (definition in formDefinitions) {
        if (definition.default) {
          val fields = mutableListOf<FieldState>()
          for (field in definition.fields) {
            val fieldState = FieldState(field)
            fieldState.value = field.value
            fields.add(fieldState)
          }

          val formState = FormState(definition)
          formState.fields = fields
          forms.add(formState)
        }
      }

      return FormsState(forms)
    }

    fun fromObservation(observation: Observation, formDefinitions: Collection<Form>): FormsState {
      val forms = mutableListOf<FormState>()
      val definitions = formDefinitions.map { it.id to it }.toMap()
      for (observationForm in observation.forms) {
        val fields = mutableListOf<FieldState>()
        val definition = definitions[observationForm.formId]
        if (definition != null) {
          for (field in definition.fields) {
            val fieldState = FieldState(field)
            fieldState.value = field.value
            fields.add(fieldState)
          }

          val formState = FormState(definition)
          formState.fields = fields
          forms.add(formState)
        }
      }

      return FormsState(forms)
    }
  }
}

