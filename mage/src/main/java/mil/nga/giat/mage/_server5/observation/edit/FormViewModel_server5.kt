package mil.nga.giat.mage._server5.observation.edit

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.gson.JsonObject
import mil.nga.giat.mage.dagger.module.ApplicationContext
import mil.nga.giat.mage.form.*
import mil.nga.giat.mage.form.Form.Companion.fromJson
import mil.nga.giat.mage.form.defaults.FormPreferences
import mil.nga.giat.mage.form.field.*
import mil.nga.giat.mage.observation.*
import mil.nga.giat.mage.sdk.datastore.observation.*
import mil.nga.giat.mage.sdk.datastore.user.*
import mil.nga.giat.mage.sdk.exceptions.UserException
import java.util.*
import javax.inject.Inject

class FormViewModel_server5 @Inject constructor(
  @ApplicationContext context: Context,
) : FormViewModel(context) {

  override fun createObservation(timestamp: Date, location: ObservationLocation, defaultMapZoom: Float?, defaultMapCenter: LatLng?): Boolean {
    if (_observationState.value != null) return false

    val jsonForms = event.forms
    val forms = mutableListOf<FormState>()
    val formDefinitions = mutableListOf<Form>()
    for ((index, jsonForm) in jsonForms.withIndex()) {
      fromJson(jsonForm as JsonObject)?.let { form ->
        formDefinitions.add(form)

        if (form.default) {
          val defaultForm = FormPreferences(context, event.id, form.id).getDefaults()
          val formState = FormState.fromForm(eventId = event.remoteId, form = form, defaultForm = defaultForm)
          formState.expanded.value = index == 0
          forms.add(formState)
        }
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
      eventName = event.name,
      userDisplayName = user?.displayName,
      forms = forms)
    _observationState.value = observationState

    return observationState.forms.value.isEmpty() && formDefinitions.isNotEmpty()
  }

  override fun createObservationState(observation: Observation, defaultMapZoom: Float?, defaultMapCenter: LatLng?) {
    _observation.value = observation

    val formDefinitions = mutableMapOf<Long, Form>()
    for (jsonForm in event.forms) {
      fromJson(jsonForm as JsonObject)?.let { form ->
        formDefinitions.put(form.id, form)
      }
    }

    val forms = mutableListOf<FormState>()
    for ((index, observationForm) in observation.forms.withIndex()) {
      val form = formDefinitions[observationForm.formId]
      if (form != null) {
        val fields = mutableListOf<FieldState<*, out FieldValue>>()
        for (field in form.fields) {
          val property = observationForm.properties.find { it.key == field.name }
          val fieldState = FieldState.fromFormField(field, property?.value)
          fields.add(fieldState)
        }

        val formState = FormState(observationForm.id, event.remoteId, form, fields)
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
      eventName = event.name,
      userDisplayName = user?.displayName,
      forms = forms,
      attachments = observation.attachments,
      important = importantState,
      favorite = isFavorite)

    _observationState.value = observationState
  }
}