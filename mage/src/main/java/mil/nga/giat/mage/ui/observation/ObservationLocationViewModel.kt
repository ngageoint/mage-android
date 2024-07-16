package mil.nga.giat.mage.ui.observation

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.data.repository.observation.ObservationLocationRepository
import mil.nga.giat.mage.data.repository.observation.ObservationRepository
import mil.nga.giat.mage.database.model.event.Form
import mil.nga.giat.mage.database.model.observation.Observation
import mil.nga.giat.mage.database.model.observation.ObservationLocation
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.map.ObservationLocationMapState
import mil.nga.giat.mage.map.ObservationMapState
import mil.nga.giat.mage.map.annotation.MapAnnotation
import mil.nga.giat.mage.observation.ObservationImportantState
import mil.nga.giat.mage.sdk.exceptions.UserException
import mil.nga.giat.mage.utils.DateFormatFactory
import java.text.DateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ObservationLocationViewModel @Inject constructor(
    private val application: Application,
    private val repository: ObservationLocationRepository,
    private val observationRepository: ObservationRepository,
    private val userLocalDataSource: UserLocalDataSource,
    private val eventLocalDataSource: EventLocalDataSource,
) : ViewModel() {
    var dateFormat: DateFormat =
        DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), application)

    private val referenceFlow = MutableSharedFlow<Long>(replay = 1)
    fun setObservationLocationId(id: Long) {
        Log.d("ObservationLocationViewModel", "setObservationLocationId: $id")
        viewModelScope.launch {
            referenceFlow.emit(id)
        }
    }

    private val observationIdFlow = MutableSharedFlow<Long>(replay = 1)
    fun setObservationId(id: Long) {
        Log.d("ObservationViewModel", "setObservationId: $id")
        viewModelScope.launch {
            observationIdFlow.emit(id)
        }
    }

//    private var _observation = MutableStateFlow<Map<Long, MapAnnotation<Long>>>(HashMap())
//    val observationShapes: StateFlow<Map<Long, MapAnnotation<Long>>> = _observationShapes.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val observationLocation = referenceFlow.flatMapLatest { id ->
        Log.d("ObservationLocationViewModel", "reference flow observationLocation: $id")
        repository.observeObservationLocation(id).transform {
            Log.d("ObservationLocationViewModel", "map observation location: $it")
            it?.let { emit(toObservationItemState(it)) }
        }
    }.asLiveData()

    private fun toObservationItemState(observationLocation: ObservationLocation): ObservationLocationMapState {
        val event = eventLocalDataSource.currentEvent
        var primary: String? = null
        var secondary: String? = null
        var form: Form? = null
        var title: String = ""

        val observation = observationRepository.observeObservation(observationLocation.observationId).asLiveData().value
        observation?.let {
            val user = userLocalDataSource.read(observation.userId)
            title = "${user?.displayName} \u2022 ${dateFormat.format(observation.timestamp)}"
        }

        if (observationLocation.fieldName == ObservationLocation.PRIMARY_OBSERVATION_GEOMETRY) {
            observation?.let {
                val currentUser = userLocalDataSource.readCurrentUser()
                val isFavorite = if (currentUser != null) {
                    val favorite = observation.favoritesMap?.get(currentUser.remoteId)
                    favorite != null && favorite.isFavorite
                } else false

                val importantState = if (observation.important?.isImportant == true) {
                    val importantUser: User? = try {
                        observation.important?.userId?.let { userLocalDataSource.read(it) }
                    } catch (ue: UserException) {
                        null
                    }

                    ObservationImportantState(
                        description = observation.important?.description,
                        user = importantUser?.displayName
                    )
                } else null

                val observationForm = observation.forms?.firstOrNull()
                val formDefinition = observationForm?.formId?.let {
                    eventLocalDataSource.getForm(it)
                }

                if (observation.forms?.isNotEmpty() == true) {
                    primary =
                        observationForm?.properties?.find { it.key == formDefinition?.primaryMapField }?.value as? String
                    secondary =
                        observationForm?.properties?.find { it.key == formDefinition?.secondaryMapField }?.value as? String
                }

                val iconFeature = MapAnnotation.fromObservation(
                    event = event,
                    observation = observation,
                    formDefinition = formDefinition,
                    observationForm = observationForm,
                    geometryType = observation.geometry.geometryType,
                    context = application
                )

                Log.d("ObservationViewModel", "toObservationItemState: $title $primary")
                return ObservationLocationMapState(
                    id = observationLocation.id,
                    observationId = observation.id,
                    title = title,
                    geometry = observation.geometry,
                    primary = primary,
                    secondary = secondary,
                    iconAnnotation = iconFeature,
                    isPrimary = true,
                    favorite = isFavorite,
                    importantState = importantState
                )
            }
        }

        primary = observationLocation.primaryFieldText
        secondary = observationLocation.secondaryFieldText

        val iconFeature = MapAnnotation.fromObservationProperties(
            id = observationLocation.observationId,
            geometry = observationLocation.geometry,
            timestamp = 0,
            accuracy = null,
            eventId = event?.remoteId ?: "",
            formId = observationLocation.formId,
            primary = primary,
            secondary = secondary,
            context = application
        )

        return ObservationLocationMapState(
            id = observationLocation.id,
            observationId = observationLocation.observationId,
            title = title,
            geometry = observationLocation.geometry,
            primary = primary,
            secondary = secondary,
            iconAnnotation = iconFeature,
            isPrimary = false,
            favorite = false,
            importantState = null
        )
    }

    private fun toObservationItemState(observation: Observation): ObservationMapState {
        val currentUser = userLocalDataSource.readCurrentUser()
        val isFavorite = if (currentUser != null) {
            val favorite = observation.favoritesMap[currentUser.remoteId]
            favorite != null && favorite.isFavorite
        } else false

        val importantState = if (observation.important?.isImportant == true) {
            val importantUser: User? = try {
                observation.important?.userId?.let { userLocalDataSource.read(it) }
            } catch (ue: UserException) { null }

            ObservationImportantState(
                description = observation.important?.description,
                user = importantUser?.displayName
            )
        } else null

        var primary: String? = null
        var secondary: String? = null
        val observationForm = observation.forms.firstOrNull()
        val formDefinition = observationForm?.formId?.let {
            eventLocalDataSource.getForm(it)
        }

        if (observation.forms.isNotEmpty()) {
            primary = observationForm?.properties?.find { it.key == formDefinition?.primaryMapField }?.value as? String
            secondary = observationForm?.properties?.find { it.key == formDefinition?.secondaryMapField }?.value as? String
        }

        val event = eventLocalDataSource.currentEvent
        val iconFeature = MapAnnotation.fromObservation(
            event = event,
            observation = observation,
            formDefinition = formDefinition,
            observationForm = observationForm,
            geometryType = observation.geometry.geometryType,
            context = application
        )

        val user = userLocalDataSource.read(observation.userId)
        val title = "${user?.displayName} \u2022 ${dateFormat.format(observation.timestamp)}"

        Log.d("ObservationViewModel", "toObservationItemState: $title $primary")
        return ObservationMapState(
            observation.id,
            title,
            observation.geometry,
            primary,
            secondary,
            iconFeature,
            isFavorite,
            importantState
        )
    }
}