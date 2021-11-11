package mil.nga.giat.mage.observation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import mil.nga.giat.mage.form.Form
import mil.nga.giat.mage.form.FormState
import mil.nga.giat.mage.form.field.DateFieldState
import mil.nga.giat.mage.form.field.GeometryFieldState
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import java.util.*

enum class ObservationPermission {
  EDIT, DELETE, FLAG
}

// TODO multi-form, this state class has gotten rather big
class ObservationState(
  id: Long? = null,
  status: ObservationStatusState,
  val definition: ObservationDefinition,
  val timestampFieldState: DateFieldState,
  val geometryFieldState: GeometryFieldState,
  val userDisplayName: String?,
  val permissions: Set<ObservationPermission> = emptySet(),
  forms: List<FormState>,
  attachments: Collection<Attachment> = emptyList(),
  important: ObservationImportantState? = null,
  favorite: Boolean = false,
  favorites: Int = 0
) {
  val id by mutableStateOf(id)
  val status = mutableStateOf(status)
  val forms = mutableStateOf(forms)
  val attachments = mutableStateOf(attachments)
  val important = mutableStateOf(important)
  val editImportantState = ObservationEditImportantState(important?.description)
  val favorite = mutableStateOf(favorite)
  val favorites = mutableStateOf(favorites)

  fun validate(): ObservationValidationResult {
    if (definition.minObservationForms != null && definition.minObservationForms > forms.value.size) {
      return ObservationValidationResult.Invalid("Total number of forms in observation must be at least ${definition.minObservationForms}.")
    }

    if (definition.maxObservationForms != null && definition.maxObservationForms < forms.value.size) {
      return ObservationValidationResult.Invalid("Total number of forms in observation cannot be more than ${definition.maxObservationForms}.")
    }

    val frequenciesByFormId = forms.value.groupingBy { it.definition.id }.eachCount()
    for (formDefinition in definition.forms) {
      val frequency = frequenciesByFormId[formDefinition.id] ?: 0
      if (formDefinition.min != null && frequency < formDefinition.min) {
        return ObservationValidationResult.Invalid("${formDefinition.name} must be included in observation at least ${formDefinition.min} ${if (formDefinition.min > 1) "times" else "time"}.")
      }

      if (formDefinition.max != null && frequency > formDefinition.max) {
        return ObservationValidationResult.Invalid("${formDefinition.name} cannot be included in observation more than ${formDefinition.min} ${if (formDefinition.max > 1) "times" else "time"}.")
      }
    }

    var fieldsValid = true
    for (formState in forms.value) {
      if (!formState.isValid()) {
        fieldsValid = false
        formState.expanded.value = true
      }
    }

    if (!fieldsValid) {
      return ObservationValidationResult.Invalid("Observation is invalid, please fix all invalid fields and try again.")
    }

    return ObservationValidationResult.Valid
  }
}

class ObservationDefinition(
  val minObservationForms: Int? = null,
  val maxObservationForms: Int? = null,
  val forms: Collection<Form> = emptyList()
)

class ObservationStatusState(
  val dirty: Boolean = true,
  val lastModified: Date? = null,
  val error: String? = null
)

class ObservationImportantState(
  val description: String? = null,
  val user: String? = null
)

class ObservationEditImportantState(description: String? = null) {
  var edit by mutableStateOf(false)
  var description by mutableStateOf(description)
}

sealed class ObservationValidationResult {
  object Valid : ObservationValidationResult()
  data class Invalid(val error: String) : ObservationValidationResult()
}