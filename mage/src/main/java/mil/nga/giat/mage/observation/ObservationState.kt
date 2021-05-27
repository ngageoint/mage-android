package mil.nga.giat.mage.observation.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import mil.nga.giat.mage.form.FormState
import mil.nga.giat.mage.form.field.DateFieldState
import mil.nga.giat.mage.form.field.GeometryFieldState
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import mil.nga.giat.mage.sdk.datastore.observation.ObservationImportant
import java.util.*

enum class ObservationPermission {
  EDIT, DELETE, FLAG
}

class ObservationState(
  status: ObservationStatusState,
  val timestampFieldState: DateFieldState,
  val geometryFieldState: GeometryFieldState,
  val eventName: String,
  val userDisplayName: String?,
  val permissions: Set<ObservationPermission> = emptySet(),
  forms: List<FormState>,
  attachments: Collection<Attachment> = emptyList(),
  important: ObservationImportant? = null,
  favorite: Boolean = false
) {
  val id by mutableStateOf<Long?>(null)
  val status = mutableStateOf(status)
  val forms = mutableStateOf(forms)
  val attachments = mutableStateOf(attachments)
  val important = mutableStateOf(important)
  val favorite = mutableStateOf(favorite)
}

class ObservationStatusState(
  val dirty: Boolean = true,
  val lastModified: Date? = null,
  val error: String? = null
)