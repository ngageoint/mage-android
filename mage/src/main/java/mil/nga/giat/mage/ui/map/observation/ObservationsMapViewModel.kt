package mil.nga.giat.mage.ui.map.observation

import android.app.Application
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
import mil.nga.giat.mage.data.repository.event.EventRepository
import mil.nga.giat.mage.data.repository.map.MapRepository
import mil.nga.giat.mage.data.repository.map.ObservationMapImage
import mil.nga.giat.mage.data.repository.map.ObservationsTileRepository
import mil.nga.giat.mage.data.repository.observation.ObservationLocationRepository
import mil.nga.giat.mage.map.annotation.MapAnnotation
import mil.nga.giat.mage.ui.map.IconMarkerState
import mil.nga.giat.mage.ui.map.overlay.DataSourceTileProvider
import javax.inject.Inject

@HiltViewModel
class ObservationsMapViewModel @Inject constructor(
    private val observationLocationRepository: ObservationLocationRepository,
    private val observationsTileRepository: ObservationsTileRepository,
    private val eventRepository: EventRepository,
    private val mapRepository: MapRepository,
    private val application: Application
): ViewModel() {
    val observationTileProvider = observationsTileRepository.refresh.map { date ->
        val provider = DataSourceTileProvider(application, observationsTileRepository)
        provider.maximumZoom = observationsTileRepository.maximumZoom
        provider
    }

    private val event = flow {
        val event = eventRepository.getCurrentEvent()
        emit(event)
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
        event
    ) { mapLocation, event ->
        if (event != null && event.remoteId != null && mapLocation.visibleRegion != null) {
            if (mapLocation.zoom > observationsTileRepository.maximumZoom) {
                var newStates = mutableMapOf<Long, IconMarkerState>()
                observationLocationRepository.getMapItems(
                    eventRemoteId = event.remoteId,
                    minLatitude = mapLocation.visibleRegion.latLngBounds.southwest.latitude,
                    maxLatitude = mapLocation.visibleRegion.latLngBounds.northeast.latitude,
                    minLongitude = mapLocation.visibleRegion.latLngBounds.southwest.longitude,
                    maxLongitude = mapLocation.visibleRegion.latLngBounds.northeast.longitude
                ).mapNotNull { mapItem ->
                    observationLocationsStates[mapItem.id]
                        ?: mapItem.geometry?.let {
                            val observationMapImage = ObservationMapImage(mapItem)
                            val image = observationMapImage.getBitmap(application)?.let {
                                val width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32.0f, application.resources.displayMetrics).toDouble()
                                observationMapImage.resizeBitmapToWidthAspectScaled(it, width.toInt())
                            }
                            observationLocationsStates[mapItem.id] = IconMarkerState(
                                markerState = MarkerState(
                                    position = LatLng(
                                        it.centroid.y,
                                        it.centroid.x
                                    )
                                ),
                                icon = image?.let { BitmapDescriptorFactory.fromBitmap(it) },
                                id = mapItem.id
                            )
                            observationLocationsStates[mapItem.id]
                        }
                    newStates[mapItem.id] = observationLocationsStates[mapItem.id]!!
                    newStates[mapItem.id]
                }
                observationLocationsStates = newStates
                newStates.values.toList()
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    .flowOn(Dispatchers.IO)
}