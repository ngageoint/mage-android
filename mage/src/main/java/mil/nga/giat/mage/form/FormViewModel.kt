package mil.nga.giat.mage.form

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.database.model.observation.Attachment
import mil.nga.giat.mage.database.model.observation.Observation
import mil.nga.giat.mage.database.model.observation.ObservationForm
import mil.nga.giat.mage.data.datasource.observation.ObservationLocalDataSource
import mil.nga.giat.mage.database.model.observation.ObservationImportant
import mil.nga.giat.mage.database.model.observation.ObservationProperty
import mil.nga.giat.mage.database.model.observation.State
import mil.nga.giat.mage.database.model.permission.Permission
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.form.Form.Companion.fromJson
import mil.nga.giat.mage.form.defaults.FormPreferences
import mil.nga.giat.mage.form.field.*
import mil.nga.giat.mage.observation.*
import mil.nga.giat.mage.observation.edit.MediaAction
import mil.nga.giat.mage.observation.sync.ObservationSyncWorker
import mil.nga.giat.mage.sdk.event.IObservationEventListener
import mil.nga.giat.mage.sdk.exceptions.ObservationException
import mil.nga.giat.mage.sdk.exceptions.UserException
import java.util.*
import javax.inject.Inject

@HiltViewModel
open class FormViewModel @Inject constructor(
   private val application: Application,
   private val userLocalDataSource: UserLocalDataSource,
   private val observationLocalDataSource: ObservationLocalDataSource,
   eventLocalDataSource: EventLocalDataSource
) : ViewModel() {

  companion object {
    private val LOG_NAME = FormViewModel::class.java.name
  }
  private var observeChanges = false

  protected val _observation = MutableLiveData<Observation>()
  val observation: LiveData<Observation> = _observation

  protected val _observationState: MutableLiveData<ObservationState> = MutableLiveData()
  val observationState: LiveData<ObservationState> = _observationState

  val event = eventLocalDataSource.currentEvent

  val listener = object : IObservationEventListener {
    override fun onObservationUpdated(updated: Observation) {
      if (!observeChanges) return

      val observation = _observation.value
      if (updated.id == observation?.id && observation?.lastModified != updated.lastModified) {
        viewModelScope.launch(Dispatchers.Main) {
          createObservationState(updated)
        }
      }
    }

    override fun onObservationCreated(observations: MutableCollection<Observation>?, sendUserNotifcations: Boolean?) {}
    override fun onObservationDeleted(observation: Observation?) {}
    override fun onError(error: Throwable?) {}
  }

  init {
    observationLocalDataSource.addListener(listener)
  }

  override fun onCleared() {
    super.onCleared()
    observationLocalDataSource.removeListener(listener)
  }

  open fun createObservation(timestamp: Date, location: ObservationLocation, defaultMapZoom: Float? = null, defaultMapCenter: LatLng? = null): Boolean {
    if (_observationState.value != null) return false

    val forms = mutableListOf<FormState>()
    val formDefinitions = mutableListOf<Form>()
    event?.forms?.mapNotNull { form ->
      fromJson(form.json)
    }
    ?.filterNot { it.archived }
    ?.forEachIndexed { index, form ->
      formDefinitions.add(form)

      val defaultForm = FormPreferences(application, event.id, form.id).getDefaults()
      val formMin = form.min ?: 0
      val formCount = formMin + if (form.default && formMin == 0) 1 else 0
      repeat(formCount) {
        val formState = FormState.fromForm(eventId = event.remoteId, form = form, defaultForm = defaultForm)
        formState.expanded.value = index == 0
        forms.add(formState)
      }
    }

    val observation = Observation()
    _observation.value = observation
    observation.event = event
    observation.geometry = location.geometry

    var user: User? = null
    try {
      user = userLocalDataSource.readCurrentUser()
      if (user != null) {
        observation.userId = user.remoteId
      }
    } catch (_: UserException) { }

    val timestampFieldState = DateFieldState(
      DateFormField(
        id = 0,
        type = FieldType.DATE,
        name ="timestamp",
        title = "Date",
        required = true,
        archived = false
      ) as FormField<Date>
    )
    timestampFieldState.answer = FieldValue.Date(timestamp)

    val geometryFieldState = GeometryFieldState(
      GeometryFormField(
        id = 0,
        type = FieldType.GEOMETRY,
        name = "geometry",
        title = "Location",
        required = true,
        archived = false
      ) as FormField<ObservationLocation>,
      defaultMapZoom = defaultMapZoom,
      defaultMapCenter = defaultMapCenter
    )
    geometryFieldState.answer = FieldValue.Location(ObservationLocation(location.geometry, location.provider, location.accuracy))

    val definition =  ObservationDefinition(
      event?.minObservationForms,
      event?.maxObservationForms,
      forms = formDefinitions
    )
    val observationState = ObservationState(
      status = ObservationStatusState(),
      definition = definition,
      timestampFieldState = timestampFieldState,
      geometryFieldState = geometryFieldState,
      userDisplayName = user?.displayName,
      forms = forms)
    _observationState.value = observationState

    return observationState.forms.value.isEmpty() && formDefinitions.isNotEmpty()
  }

  fun setObservation(observationId: Long, observeChanges: Boolean = false, defaultMapZoom: Float? = null, defaultMapCenter: LatLng? = null) {
    if (_observationState.value != null) return

    this.observeChanges = observeChanges

    try {
      val observation = observationLocalDataSource.read(observationId)
      createObservationState(observation, defaultMapZoom, defaultMapCenter)
    } catch (e: ObservationException) {
      Log.e(LOG_NAME, "Problem reading observation.", e)
    }
  }

  fun setObservation(observation: Observation) {
    createObservationState(observation)
  }

  protected open fun createObservationState(observation: Observation, defaultMapZoom: Float? = null, defaultMapCenter: LatLng? = null) {
    _observation.value = observation
    val currentEvent = event ?: return

    val formDefinitions = currentEvent.forms
      .mapNotNull { fromJson(it.json) }
      .associateBy { it.id }

    val forms = mutableListOf<FormState>()
    observation.forms.forEachIndexed { index, observationForm ->
      val formDefinition = formDefinitions[observationForm.formId]
      if (formDefinition != null) {
        val fields = mutableListOf<FieldState<*, out FieldValue>>()
        for (fieldDefinition in formDefinition.fields) {
          val value: Any? = if (fieldDefinition.type == FieldType.ATTACHMENT) {
            val attachments = observation.attachments.filter {
              it.fieldName == fieldDefinition.name && it.observationFormId == observationForm.remoteId
            }
            val value = observationForm.properties.find { it.key == fieldDefinition.name }?.value as? List<Attachment> ?: listOf()
            attachments.plus(value)
          } else {
            observationForm.properties.find { it.key == fieldDefinition.name }?.value
          }
          val fieldState = FieldState.fromFormField(fieldDefinition, value)
          fields.add(fieldState)
        }

        val formState = FormState(observationForm.id, observationForm.remoteId, currentEvent.remoteId, formDefinition, fields)
        formState.expanded.value = index == 0
        forms.add(formState)
      }
    }

    val timestampFieldState = DateFieldState(
      DateFormField(
        id = 0,
        type = FieldType.DATE,
        name ="timestamp",
        title = "Date",
        required = true,
        archived = false
      ) as FormField<Date>
    )
    timestampFieldState.answer = FieldValue.Date(observation.timestamp)

    val geometryFieldState = GeometryFieldState(
      GeometryFormField(
        id = 0,
        type = FieldType.GEOMETRY,
        name = "geometry",
        title = "Location",
        required = true,
        archived = false
      ) as FormField<ObservationLocation>,
      defaultMapZoom = defaultMapZoom,
      defaultMapCenter = defaultMapCenter
    )
    geometryFieldState.answer = FieldValue.Location(ObservationLocation(observation))

    val currentUser: User? = try {
      userLocalDataSource.readCurrentUser()
    } catch (ue: UserException) { null }

    val permissions = mutableSetOf<ObservationPermission>()
    val userPermissions: Collection<Permission>? = currentUser?.role?.permissions?.permissions
    if (userPermissions?.contains(Permission.UPDATE_OBSERVATION_ALL) == true ||
      userPermissions?.contains(Permission.UPDATE_OBSERVATION_EVENT) == true) {
      permissions.add(ObservationPermission.EDIT)
    }

    if (userPermissions?.contains(Permission.DELETE_OBSERVATION) == true || observation.userId.equals(currentUser)) {
      permissions.add(ObservationPermission.DELETE)
    }

    if (userPermissions?.contains(Permission.UPDATE_EVENT) == true || hasUpdatePermissionsInEventAcl(currentUser)) {
      permissions.add(ObservationPermission.FLAG)
    }

    val isFavorite = if (currentUser != null) {
      val favorite = observation.favoritesMap[currentUser.remoteId]
      favorite != null && favorite.isFavorite
    } else false
    val favorites = observation.favoritesMap.size

    val status = ObservationStatusState(observation.isDirty, observation.lastModified, observation.error?.message)
    val definition =  ObservationDefinition(
      event.minObservationForms,
      event.maxObservationForms,
      forms = formDefinitions.values
    )

    val importantState = if (observation.important?.isImportant == true) {
      val importantUser: User? = try {
        observation.important?.userId?.let { userLocalDataSource.read(it) }
      } catch (ue: UserException) { null }

      ObservationImportantState(
        description = observation.important?.description,
        user = importantUser?.displayName
      )
    } else null

    val user: User? = try {
      userLocalDataSource.read(observation.userId)
    } catch (ue: UserException) { null }

    val observationState = ObservationState(
      id = observation.id,
      status = status,
      definition = definition,
      permissions = permissions,
      timestampFieldState = timestampFieldState,
      geometryFieldState = geometryFieldState,
      userDisplayName = user?.displayName,
      forms = forms,
      attachments = observation.attachments,
      important = importantState,
      favorite = isFavorite,
      favorites = favorites)

    _observationState.value = observationState
  }

  open fun saveObservation(): Boolean {
    val observation = _observation.value!!

    observation.state = State.ACTIVE
    observation.isDirty = true
    observation.timestamp = observationState.value!!.timestampFieldState.answer!!.date

    val location: ObservationLocation = observationState.value!!.geometryFieldState.answer!!.location
    observation.geometry = location.geometry
    observation.accuracy = location.accuracy

    var provider = location.provider
    if (provider == null || provider.trim { it <= ' ' }.isEmpty()) {
      provider = "manual"
    }
    observation.provider = provider

    if (!"manual".equals(provider, ignoreCase = true)) {
      // TODO multi-form, what is locationDelta supposed to represent
      observation.locationDelta = location.time.toString()
    }

    val observationForms: MutableCollection<ObservationForm> = ArrayList()
    val formsState: List<FormState> = observationState.value?.forms?.value ?: emptyList()
    for (formState in formsState) {
      val properties: MutableCollection<ObservationProperty> = ArrayList()
      for (fieldState in formState.fields) {
        val answer = fieldState.answer
        if (answer != null) {
          properties.add(
            ObservationProperty(
              fieldState.definition.name,
              answer.serialize()
            )
          )
        }
      }

      val observationForm =
        ObservationForm()
      observationForm.remoteId = formState.remoteId
      observationForm.formId = formState.definition.id
      observationForm.addProperties(properties)
      observationForms.add(observationForm)
    }

    observation.forms = observationForms

    try {
      if (observation.id == null) {
        val newObs = observationLocalDataSource.create(observation)
        Log.i(LOG_NAME, "Created new observation with id: " + newObs?.id)
      } else {
        observationLocalDataSource.update(observation)
        Log.i(LOG_NAME, "Updated observation with remote id: " + observation.remoteId)
      }
    } catch (e: java.lang.Exception) {
      Log.e(LOG_NAME, e.message, e)
    }

    return true
  }

  fun draftObservation(): Observation {
    val observation = _observation.value!!

    observation.state = State.ACTIVE
    observation.isDirty = true
    observation.timestamp = observationState.value!!.timestampFieldState.answer!!.date

    val location: ObservationLocation = observationState.value!!.geometryFieldState.answer!!.location
    observation.geometry = location.geometry
    observation.accuracy = location.accuracy

    var provider = location.provider
    if (provider == null || provider.trim { it <= ' ' }.isEmpty()) {
      provider = "manual"
    }
    observation.provider = provider

    if (!"manual".equals(provider, ignoreCase = true)) {
      // TODO multi-form, what is locationDelta supposed to represent
      observation.locationDelta = location.time.toString()
    }

    val observationForms: MutableCollection<ObservationForm> = ArrayList()
    val formsState: List<FormState> = observationState.value?.forms?.value ?: emptyList()
    for (formState in formsState) {
      val properties: MutableCollection<ObservationProperty> = ArrayList()
      for (fieldState in formState.fields) {
        val answer = fieldState.answer
        if (answer != null) {
            properties.add(
              ObservationProperty(
                fieldState.definition.name,
                answer.serialize()
              )
            )
        }
      }

      val observationForm =
        ObservationForm()
      observationForm.remoteId = formState.remoteId
      observationForm.formId = formState.definition.id
      observationForm.addProperties(properties)
      observationForms.add(observationForm)
    }

    observation.forms = observationForms

    return observation
  }

  fun deleteObservation() {
    _observation.value?.let {
      observationLocalDataSource.archive(it)
    }
  }

  fun syncObservation() {
    ObservationSyncWorker.scheduleWork(application)
  }

  fun addForm(form: Form) {
    val currentEvent = event ?: return
    val forms = observationState.value?.forms?.value?.toMutableList() ?: mutableListOf()
    val defaultForm = FormPreferences(application, currentEvent.id, form.id).getDefaults()
    val formState = FormState.fromForm(eventId = currentEvent.remoteId, form = form, defaultForm = defaultForm)
    formState.expanded.value = true
    forms.add(formState)
    _observationState.value?.forms?.value = forms
  }

  fun deleteForm(index: Int) {
    try {
      val forms = observationState.value?.forms?.value?.toMutableList() ?: mutableListOf()
      forms.removeAt(index)
      observationState.value?.forms?.value = forms
    } catch(_: IndexOutOfBoundsException) {}
  }

  fun reorderForms(forms: List<FormState>) {
    observationState.value?.forms?.value = forms
    saveObservation()
  }

  open fun addAttachment(attachment: Attachment, action: MediaAction?) {
    val fieldState = getAttachmentField(action)
    attachment.fieldName = fieldState?.definition?.name
    val attachments = fieldState?.answer?.attachments?.toMutableList() ?: mutableListOf()
    attachments.add(attachment)
    fieldState?.answer = FieldValue.Attachment(attachments)
  }

  fun getAttachmentField(action: MediaAction?): AttachmentFieldState? {
    return observationState.value?.forms?.value?.getOrNull(action?.formIndex ?: -1)?.fields?.find {
      it.definition.name == action?.fieldName
    } as? AttachmentFieldState
  }

  fun deleteAttachment(attachment: Attachment, fieldState: FieldState<*, *>?) {
    val attachmentFieldState = fieldState as? AttachmentFieldState
    attachmentFieldState?.answer?.attachments?.let { attachments ->
      if (attachment.url?.isNotEmpty() == true) {
        // remote attachment, mark for delete
        attachments.find { it.name == attachment.name }?.let {
          it.action = Media.ATTACHMENT_DELETE_ACTION
        }

        fieldState.answer = FieldValue.Attachment(attachments)
      } else {
        // local attachment, just remove from list
        val filtered = attachments.filter { it.name != attachment.name }
        fieldState.answer = FieldValue.Attachment(filtered)
      }
    }
  }

  fun flagObservation(description: String?) {
    val observation = _observation.value ?: return
    val observationImportant = observation.important

    val important = if (observationImportant == null) {
      val important = ObservationImportant()
      observation.important = important
      important
    } else observationImportant

    try {
      val user: User? = try {
        userLocalDataSource.readCurrentUser()
      } catch (e: Exception) { null }

      important.userId = user?.remoteId
      important.timestamp = Date()
      important.description = description

      observationLocalDataSource.addImportant(observation)
      _observationState.value?.important?.value = ObservationImportantState(description = description, user = user?.displayName)
    } catch (e: ObservationException) {
      Log.e(LOG_NAME, "Error updating important flag for observation:" + observation?.remoteId)
    }
  }

  fun unflagObservation() {
    val observation = _observation.value ?: return

    try {
      observationLocalDataSource.removeImportant(observation)
      _observationState.value?.important?.value = null
    } catch (e: ObservationException) {
      Log.e(LOG_NAME, "Error removing important flag for observation: " + observation.remoteId)
    }
  }

  fun toggleFavorite() {
    val observation = _observation.value ?: return
    val user = userLocalDataSource.readCurrentUser() ?: return

    val isFavorite: Boolean = _observationState.value?.favorite?.value == true
    try {
      if (isFavorite) {
        observationLocalDataSource.unfavoriteObservation(observation, user)
      } else {
        observationLocalDataSource.favoriteObservation(observation, user)
      }

      _observationState.value?.favorite?.value = !isFavorite
    } catch (e: ObservationException) {
      Log.e(LOG_NAME, "Could not toggle observation favorite", e)
    }
  }

  protected fun hasUpdatePermissionsInEventAcl(user: User?): Boolean {
    val currentEvent = event ?: return false
    return if (user != null) {
      currentEvent.acl[user.remoteId]
        ?.asJsonObject
        ?.get("permissions")
        ?.asJsonArray
        ?.toString()
        ?.contains("update") == true
    } else false
  }
}