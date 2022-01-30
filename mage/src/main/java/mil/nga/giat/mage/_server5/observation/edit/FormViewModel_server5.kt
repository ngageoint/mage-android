package mil.nga.giat.mage._server5.observation.edit

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import mil.nga.giat.mage.form.*
import mil.nga.giat.mage.form.Form.Companion.fromJson
import mil.nga.giat.mage.form.defaults.FormPreferences
import mil.nga.giat.mage.form.field.DateFieldState
import mil.nga.giat.mage.form.field.FieldState
import mil.nga.giat.mage.form.field.FieldValue
import mil.nga.giat.mage.form.field.GeometryFieldState
import mil.nga.giat.mage.observation.*
import mil.nga.giat.mage.observation.edit.MediaAction
import mil.nga.giat.mage.sdk.datastore.observation.*
import mil.nga.giat.mage.sdk.datastore.user.Permission
import mil.nga.giat.mage.sdk.datastore.user.User
import mil.nga.giat.mage.sdk.datastore.user.UserHelper
import mil.nga.giat.mage.sdk.exceptions.UserException
import java.util.*
import javax.inject.Inject

@HiltViewModel
class FormViewModel_server5 @Inject constructor(
  @ApplicationContext context: Context
) : FormViewModel(context) {

  companion object {
    private val LOG_NAME = FormViewModel_server5::class.java.name
  }

  val attachments = mutableListOf<Attachment>()

  override fun createObservation(timestamp: Date, location: ObservationLocation, defaultMapZoom: Float?, defaultMapCenter: LatLng?): Boolean {
    if (_observationState.value != null) return false

    val forms = mutableListOf<FormState>()
    val formDefinitions = mutableListOf<Form>()
    event.forms.mapNotNull { form ->
      fromJson(form.json)
    }
    .forEachIndexed { index, form ->
      formDefinitions.add(form)

      val defaultForm = FormPreferences(context, event.id, form.id).getDefaults()
      repeat((form.min ?: 0) + if (form.default) 1 else 0) {
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
      user = UserHelper.getInstance(context).readCurrentUser()
      if (user != null) {
        observation.userId = user.remoteId
      }
    } catch (ue: UserException) { }

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
    geometryFieldState.answer = FieldValue.Location(ObservationLocation(location.geometry))

    val definition =  ObservationDefinition(
      minObservationForms = if (formDefinitions.isEmpty()) 0 else 1,
      maxObservationForms = 1,
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

  override fun createObservationState(observation: Observation, defaultMapZoom: Float?, defaultMapCenter: LatLng?) {
    _observation.value = observation

    val formDefinitions = mutableMapOf<Long, Form>()
    for (form in event.forms) {
      fromJson(form.json)?.let { it ->
        formDefinitions.put(it.id, it)
      }
    }

    val forms = mutableListOf<FormState>()
    observation.forms.forEachIndexed { index, observationForm ->
      val form = formDefinitions[observationForm.formId]
      if (form != null) {
        val fields = mutableListOf<FieldState<*, out FieldValue>>()
        for (field in form.fields) {
          val property = observationForm.properties.find { it.key == field.name }
          val fieldState = FieldState.fromFormField(field, property?.value)
          fields.add(fieldState)
        }

        val formState = FormState(observationForm.id, observationForm.remoteId, event.remoteId, form, fields)
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

    val user: User? = try {
      UserHelper.getInstance(context).read(observation.userId)
    } catch (ue: UserException) { null }

    val permissions = mutableSetOf<ObservationPermission>()
    val userPermissions: Collection<Permission>? = user?.role?.permissions?.permissions
    if (userPermissions?.contains(Permission.UPDATE_OBSERVATION_ALL) == true ||
      userPermissions?.contains(Permission.UPDATE_OBSERVATION_EVENT) == true) {
      permissions.add(ObservationPermission.EDIT)
    }

    if (userPermissions?.contains(Permission.DELETE_OBSERVATION) == true || observation.userId.equals(user)) {
      permissions.add(ObservationPermission.DELETE)
    }

    if (userPermissions?.contains(Permission.UPDATE_EVENT) == true || hasUpdatePermissionsInEventAcl(user)) {
      permissions.add(ObservationPermission.FLAG)
    }

    val isFavorite = if (user != null) {
      val favorite = observation.favoritesMap[user.remoteId]
      favorite != null && favorite.isFavorite
    } else false

    val status = ObservationStatusState(observation.isDirty, observation.lastModified, observation.error?.message)
    val definition =  ObservationDefinition(
      minObservationForms = if (forms.isEmpty()) 0 else 1,
      maxObservationForms = 1,
      forms = formDefinitions.values
    )

    val importantState = if (observation.important?.isImportant == true) {
      val importantUser: User? = try {
        UserHelper.getInstance(context).read(observation.important?.userId)
      } catch (ue: UserException) { null }

      ObservationImportantState(
        description = observation.important?.description,
        user = importantUser?.displayName
      )
    } else null

    val observationState = ObservationState(
      status = status,
      definition = definition,
      permissions = permissions,
      timestampFieldState = timestampFieldState,
      geometryFieldState = geometryFieldState,
      userDisplayName = user?.displayName,
      forms = forms,
      attachments = observation.attachments,
      important = importantState,
      favorite = isFavorite)

    _observationState.value = observationState
  }

  override fun addAttachment(attachment: Attachment, action: MediaAction?) {
    this.attachments.add(attachment)

    val attachments = observationState.value?.attachments?.value?.toMutableList() ?: mutableListOf()
    attachments.add(attachment)

    _observationState.value?.attachments?.value = attachments
  }

  override fun saveObservation(): Boolean {
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
          // TODO, attachment field value, how to serialize/deserialize
          properties.add(ObservationProperty(fieldState.definition.name, answer.serialize()))
        }
      }

      val observationForm = ObservationForm()
      observationForm.remoteId = formState.remoteId
      observationForm.formId = formState.definition.id
      observationForm.addProperties(properties)
      observationForms.add(observationForm)
    }

    observation.forms = observationForms
    observation.attachments.addAll(attachments)

    try {
      if (observation.id == null) {
        val newObs = ObservationHelper.getInstance(context).create(observation)
        Log.i(LOG_NAME, "Created new observation with id: " + newObs.id)
      } else {
        ObservationHelper.getInstance(context).update(observation)
        Log.i(LOG_NAME, "Updated observation with remote id: " + observation.remoteId)
      }
    } catch (e: java.lang.Exception) {
      Log.e(LOG_NAME, e.message, e)
    }

    return true
  }
}