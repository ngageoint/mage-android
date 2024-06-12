package mil.nga.giat.mage.ui.map.observation

import android.app.Application
import android.content.SharedPreferences
import android.util.TypedValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.repository.event.EventRepository
import mil.nga.giat.mage.data.repository.map.MapLocation
import mil.nga.giat.mage.data.repository.map.MapRepository
import mil.nga.giat.mage.data.repository.map.ObservationMapImage
import mil.nga.giat.mage.data.repository.map.ObservationsTileRepository
import mil.nga.giat.mage.data.repository.observation.ObservationLocationRepository
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.sdk.preferences.getBooleanFlowForKey
import mil.nga.giat.mage.sdk.preferences.getIntegerFlowForKey
import mil.nga.giat.mage.ui.map.IconMarkerState
import mil.nga.giat.mage.ui.map.overlay.DataSourceTileProvider
import javax.inject.Inject

@HiltViewModel
class ObservationsMapViewModel @Inject constructor(
    private val observationLocationRepository: ObservationLocationRepository,
    private val observationsTileRepository: ObservationsTileRepository,
    private val eventRepository: EventRepository,
    private val mapRepository: MapRepository,
    private val application: Application,
    val preferences: SharedPreferences
): ViewModel() {

    private val event = flow {
        val event = eventRepository.getCurrentEvent()
        emit(event)
    }

    // when any of these change, we can trigger a refresh
    private val preferencesFlow = combine(
        preferences.getIntegerFlowForKey(application.getString(R.string.activeTimeFilterKey)),
        preferences.getBooleanFlowForKey(application.getString(R.string.activeImportantFilterKey)),
        preferences.getBooleanFlowForKey(application.getString(R.string.activeFavoritesFilterKey)),
        ::Triple
    )

    val observationTileProvider = observationsTileRepository.refresh.map { date ->
        val provider = DataSourceTileProvider(application, observationsTileRepository)
        provider.maximumZoom = observationsTileRepository.maximumZoom
        provider
    }

    val observationLocations: StateFlow<List<IconMarkerState>> = observationAnnotationFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private var observationLocationsStates = mutableMapOf<Long, IconMarkerState>()

    private fun observationAnnotationFlow() = combine(
        mapRepository.mapLocationWithRegion,
        event,
        preferencesFlow
    ) { mapLocation, event, preferencesFlow ->
        refresh(event, mapLocation)
    }.flowOn(Dispatchers.IO)

    private fun refresh(event: Event?, mapLocation: MapLocation): List<IconMarkerState> {
        if (event == null ||
            event.remoteId == null ||
            mapLocation.visibleRegion == null ||
            mapLocation.zoom <= observationsTileRepository.maximumZoom
        ) {
            observationLocationsStates = mutableMapOf()
            return emptyList()
        }
        var newStates = mutableMapOf<Long, IconMarkerState>()
        observationLocationRepository.getMapItems(
            eventRemoteId = event.remoteId,
            minLatitude = mapLocation.visibleRegion.latLngBounds.southwest.latitude,
            maxLatitude = mapLocation.visibleRegion.latLngBounds.northeast.latitude,
            minLongitude = mapLocation.visibleRegion.latLngBounds.southwest.longitude,
            maxLongitude = mapLocation.visibleRegion.latLngBounds.northeast.longitude
        ).mapNotNull { mapItem ->
            mapItem.geometry?.let { geometry ->
                val state = observationLocationsStates[mapItem.id]
                    ?: run {
                        val observationMapImage = ObservationMapImage(mapItem)
                        val image = observationMapImage.getBitmap(application)?.let {
                            val width = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                32.0f,
                                application.resources.displayMetrics
                            ).toDouble()
                            observationMapImage.resizeBitmapToWidthAspectScaled(
                                it,
                                width.toInt()
                            )
                        }
                        observationLocationsStates[mapItem.id] = IconMarkerState(
                            markerState = MarkerState(
                                position = LatLng(
                                    geometry.centroid.y,
                                    geometry.centroid.x
                                )
                            ),
                            icon = image?.let { BitmapDescriptorFactory.fromBitmap(it) },
                            id = mapItem.id
                        )
                        observationLocationsStates[mapItem.id]
                    }
                state?.let {
                    it.markerState.position = LatLng(
                        geometry.centroid.y,
                        geometry.centroid.x
                    )
                    newStates[mapItem.id] = it
                }
            }
            newStates[mapItem.id]
        }
        observationLocationsStates = newStates
        return observationLocationsStates.values.toList()
    }
}