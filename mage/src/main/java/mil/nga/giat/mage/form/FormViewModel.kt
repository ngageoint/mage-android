package mil.nga.giat.mage.observation.form

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mil.nga.giat.mage.dagger.module.ApplicationContext
import mil.nga.giat.mage.form.*
import mil.nga.giat.mage.form.Form.Companion.fromJson
import mil.nga.giat.mage.form.field.*
import mil.nga.giat.mage.observation.ObservationLocation
import mil.nga.giat.mage.sdk.datastore.observation.*
import mil.nga.giat.mage.sdk.datastore.user.*
import mil.nga.giat.mage.sdk.event.IObservationEventListener
import mil.nga.giat.mage.sdk.exceptions.ObservationException
import mil.nga.giat.mage.sdk.exceptions.UserException
import java.util.*
import javax.inject.Inject

enum class FormMode {
  VIEW,
  EDIT;
}

class FormViewModel @Inject constructor(
  @ApplicationContext val context: Context,
) : ViewModel() {

  companion object {
    private val LOG_NAME = FormViewModel::class.java.name
  }

  private val _observation = MutableLiveData<Observation>()
  val observation: LiveData<Observation> = _observation

  private val _observationState: MutableLiveData<ObservationState> = MutableLiveData()
  val observationState: LiveData<ObservationState> = _observationState

  var formMode = FormMode.VIEW

  private val event: Event = EventHelper.getInstance(context).currentEvent

  val attachments = mutableListOf<Attachment>()

  val listener = object : IObservationEventListener {
    override fun onObservationUpdated(updated: Observation) {
      val observation = _observation.value
      if (updated.id == observation?.id && observation?.lastModified != updated.lastModified) {
        // Update observation state, something changed
        GlobalScope.launch(Dispatchers.Main) {
          createObservationState(updated)
        }
      }
    }

    override fun onObservationCreated(observations: MutableCollection<Observation>?, sendUserNotifcations: Boolean?) {}
    override fun onObservationDeleted(observation: Observation?) {}
    override fun onError(error: Throwable?) {}
  }

  init {
    ObservationHelper.getInstance(context).addListener(listener)
  }

  override fun onCleared() {
    super.onCleared()
    ObservationHelper.getInstance(context).removeListener(listener)
  }

  fun createObservation(timestamp: Date, location: ObservationLocation, defaultMapZoom: Float? = null, defaultMapCenter: LatLng? = null) {
    if (_observationState.value != null) return
    
    val jsonForms = event.forms
    val forms = mutableListOf<FormState>()
    for ((index, jsonForm) in jsonForms.withIndex()) {
      fromJson(jsonForm as JsonObject)?.let { form ->
        if (form.default) {
          val fields = mutableListOf<FieldState<*, out FieldValue>>()
          for (field in form.fields) {
            fields.add(toFieldState(field, field.value))
          }

          val formState = FormState(event.remoteId, form, fields)
          formState.expanded.value = index == 0
          forms.add(formState)
        }
      }
    }

    val observation = Observation()
    observation.event = event
    observation.geometry = location.geometry

    var user: User? = null
    try {
      user = UserHelper.getInstance(context).readCurrentUser()
      if (user != null) {
        observation.userId = user.remoteId
      }
    } catch (ue: UserException) { }

    // TODO new stuff
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

    val observationState = ObservationState(
      status = ObservationStatusState(),
      timestampFieldState = timestampFieldState,
      geometryFieldState = geometryFieldState,
      eventName = event.name,
      userDisplayName = user?.displayName,
      forms = forms)
    _observationState.value = observationState
  }

  fun setObservation(observationId: Long, defaultMapZoom: Float? = null, defaultMapCenter: LatLng? = null) {
    if (_observationState.value != null) return

    try {
      val observation = ObservationHelper.getInstance(context).read(observationId)
      createObservationState(observation, defaultMapZoom, defaultMapCenter)
    } catch (e: ObservationException) {
      Log.e(LOG_NAME, "Problem reading observation.", e)
    }
  }

  private fun createObservationState(observation: Observation, defaultMapZoom: Float? = null, defaultMapCenter: LatLng? = null) {
    _observation.value = observation

    val jsonForms = event.formMap
    val forms = mutableListOf<FormState>()
    for ((index, observationForm) in observation.forms.withIndex()) {
      val formJson = jsonForms[observationForm.formId]
      val fields = mutableListOf<FieldState<*, out FieldValue>>()

      fromJson(formJson)?.let { form ->
        for (field in form.fields) {
          val property = observationForm.properties.find { it.key == field.name }
          fields.add(toFieldState(field, property?.value))
        }

        val formState = FormState(event.remoteId, form, fields)
        formState.expanded.value = index == 0
        forms.add(formState)
      }
    }

    // TODO new stuff
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

    val observationState = ObservationState(
      status = status,
      permissions = permissions,
      timestampFieldState = timestampFieldState,
      geometryFieldState = geometryFieldState,
      eventName = event.name,
      userDisplayName = user?.displayName,
      forms = forms,
      attachments = observation.attachments,
      important = observation.important,
      favorite = isFavorite)

    _observationState.value = observationState
  }

  fun saveObservation() {
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
          properties.add(ObservationProperty(fieldState.definition.name, answer.serialize()))
        }
      }
      val observationForm = ObservationForm()
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
  }

  fun deleteObservation() {
    val observation = _observation.value
    ObservationHelper.getInstance(context).archive(observation)
  }

  fun addForm(form: Form) {
    val forms = observationState.value?.forms?.value?.toMutableList() ?: mutableListOf()
    val fields = mutableListOf<FieldState<*, out FieldValue>>()
    for (field in form.fields) {
      fields.add(toFieldState(field, field.value))
    }

    forms.add(FormState(event.remoteId, form, fields))

    _observationState.value?.forms?.value = forms
  }

  fun deleteForm(index: Int) {
    try {
      val forms = observationState.value?.forms?.value?.toMutableList() ?: mutableListOf()
      forms.removeAt(index)
      observationState.value?.forms?.value = forms
    } catch(e: IndexOutOfBoundsException) {}
  }

  fun addAttachment(attachment: Attachment) {
    this.attachments.add(attachment)

    val attachments = observationState.value?.attachments?.value?.toMutableList() ?: mutableListOf()
    attachments.add(attachment)

    _observationState.value?.attachments?.value = attachments
  }
  
  fun flagObservation(observationImportant: ObservationImportant?, description: String?) {
    val observation = _observation.value
    try {
      val important = if (observationImportant == null) {
        val newImportant = ObservationImportant()
        observation?.important = newImportant
        newImportant
      } else observationImportant

      val user: User? = try {
        UserHelper.getInstance(context).readCurrentUser()
      } catch (e: Exception) { null }
      
      important.userId = user?.remoteId
      important.timestamp = Date()
      important.description = description

      ObservationHelper.getInstance(context).addImportant(observation)
      _observationState.value?.important?.value = important
    } catch (e: ObservationException) {
      Log.e(LOG_NAME, "Error updating important flag for observation:" + observation?.remoteId)
    }
  }
  
  fun unflagObservation() {
    val observation = _observation.value

    try {
      ObservationHelper.getInstance(context).removeImportant(observation)
      _observationState.value?.important?.value = null
    } catch (e: ObservationException) {
      Log.e(LOG_NAME, "Error removing important flag for observation: " + observation?.remoteId)
    }
  }

  fun toggleFavorite() {
    val observation = _observation.value

    val observationHelper = ObservationHelper.getInstance(context)
    val isFavorite: Boolean = _observationState.value?.favorite?.value == true
    try {
      val user: User? = try {
        UserHelper.getInstance(context).readCurrentUser()
      } catch (e: Exception) { null }

      if (isFavorite) {
        observationHelper.unfavoriteObservation(observation, user)
      } else {
        observationHelper.favoriteObservation(observation, user)
      }

      _observationState.value?.favorite?.value = !isFavorite
    } catch (e: ObservationException) {
      Log.e(LOG_NAME, "Could not toggle observation favorite", e)
    }
  }

  private fun toFieldState(fieldDefinition: FormField<Any>, value: Any?): FieldState<*, out FieldValue> {
    return when (fieldDefinition.type) {
      FieldType.CHECKBOX -> {
        val fieldState = BooleanFieldState(fieldDefinition as BooleanFormField)
        val boolean = value as? Boolean
        if (boolean != null) {
          fieldState.answer = FieldValue.Boolean(boolean)
        }
        fieldState
      }
      FieldType.DATE -> {
        val fieldState = DateFieldState(fieldDefinition as DateFormField)
        val date = value as? Date
        if (date != null) {
          fieldState.answer = FieldValue.Date(date)
        }
        fieldState
      }
      FieldType.DROPDOWN -> {
        val fieldState = SelectFieldState(fieldDefinition as SingleChoiceFormField)
        val text = value as? String
        if (text != null) {
          fieldState.answer = FieldValue.Text(text)
        }
        fieldState
      }
      FieldType.EMAIL -> {
        val fieldState = EmailFieldState(fieldDefinition as TextFormField)
        val email = value as? String
        if (email != null) {
          fieldState.answer = FieldValue.Text(email)
        }
        fieldState
      }
      FieldType.GEOMETRY -> {
        val fieldState = GeometryFieldState(fieldDefinition as GeometryFormField)
        val geometry = value as? ObservationLocation
        if (geometry != null) {
          fieldState.answer = FieldValue.Location(geometry)
        }
        fieldState
      }
      FieldType.MULTISELECTDROPDOWN -> {
        val fieldState = MultiSelectFieldState(fieldDefinition as MultiChoiceFormField)
        val choices = value as? Collection<*>
        if (choices != null) {
          fieldState.answer = FieldValue.Multi(choices.map { it.toString() })
        }
        fieldState
      }
      FieldType.NUMBERFIELD -> {
        val fieldState = NumberFieldState(fieldDefinition as NumberFormField)
        val number = value as? Number
        if (number != null) {
          fieldState.answer = FieldValue.Number(number)
        }
        fieldState
      }
      FieldType.RADIO -> {
        val fieldState = RadioFieldState(fieldDefinition as SingleChoiceFormField)
        val text = value as? String
        if (text != null) {
          fieldState.answer = FieldValue.Text(text)
        }
        fieldState
      }
      FieldType.TEXTAREA, FieldType.TEXTFIELD -> {
        val fieldState = TextFieldState(fieldDefinition as TextFormField)
        val text = value as? String
        if (text != null) {
          fieldState.answer = FieldValue.Text(text)
        }
        fieldState
      }
    }
  }

  private fun hasUpdatePermissionsInEventAcl(user: User?): Boolean {
    return if (user != null) {
      event.acl[user.remoteId]
        ?.asJsonObject
        ?.get("permissions")
        ?.asJsonArray
        ?.toString()
        ?.contains("update") == true
    } else false
  }
}