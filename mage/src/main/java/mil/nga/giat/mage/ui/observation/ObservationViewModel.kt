package mil.nga.giat.mage.ui.observation

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.data.datasource.observation.ObservationLocationLocalDataSource
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.data.repository.map.ObservationTileRepository
import mil.nga.giat.mage.data.repository.observation.ObservationRepository
import mil.nga.giat.mage.database.model.observation.Observation
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.map.ObservationMapState
import mil.nga.giat.mage.map.annotation.MapAnnotation
import mil.nga.giat.mage.observation.ObservationImportantState
import mil.nga.giat.mage.sdk.exceptions.UserException
import mil.nga.giat.mage.ui.map.overlay.DataSourceTileProvider
import mil.nga.giat.mage.utils.DateFormatFactory
import java.text.DateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ObservationViewModel @Inject constructor(
    private val application: Application,
    private val repository: ObservationRepository,
    private val dataSource: ObservationLocationLocalDataSource,
    private val userLocalDataSource: UserLocalDataSource,
    private val eventLocalDataSource: EventLocalDataSource,
): ViewModel() {
    var dateFormat: DateFormat =
        DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), application)

    private val referenceFlow = MutableSharedFlow<Long>(replay = 1)
    fun setObservationId(id: Long) {
        Log.d("ObservationViewModel", "setObservationId: $id")
        viewModelScope.launch {
            referenceFlow.emit(id)
        }
    }

    val tileProvider = referenceFlow.map { id ->
        val tileRepository = ObservationTileRepository(observationId = id, localDataSource = dataSource)
        DataSourceTileProvider(application, tileRepository)
    }.asLiveData()

    @OptIn(ExperimentalCoroutinesApi::class)
    val observation = referenceFlow.flatMapLatest { id ->
        Log.d("ObservationViewModel", "reference flow observation: $id")
        repository.observeObservation(id).transform {
            Log.d("ObservationViewModel", "map observation: $it")
            it?.let { emit(toObservationItemState(it)) }
        }
    }.asLiveData()

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