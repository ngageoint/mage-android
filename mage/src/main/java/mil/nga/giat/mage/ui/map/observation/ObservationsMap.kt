package mil.nga.giat.mage.ui.map.observation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.VisibleRegion
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.TileOverlay

@Composable
fun ObservationsMap(
    viewModel: ObservationsMapViewModel = hiltViewModel(),
    isMapLoaded: Boolean,
    cameraPositionState: CameraPositionState,
    onMapTap: (latLng: LatLng, visibleRegion: VisibleRegion) -> Unit
) {

    val observationTileProvider by viewModel.observationTileProvider.observeAsState()
    val observationLocations by viewModel.observationLocations.collectAsState(emptyList())

    if (isMapLoaded) {
        observationTileProvider?.let {
            TileOverlay(
                tileProvider = it,
                onClick = {
                    Log.d("MapScreen", "tile overlay click")
                }
            )
        }
    }

    observationLocations.forEach { state ->
        key(state.id) {
            Marker(
                state = state.markerState,
                icon = state.icon,
                onClick = { marker ->
                    cameraPositionState.projection?.visibleRegion?.let { visibleRegion ->
                        onMapTap(
                            marker.position,
                            visibleRegion
                        )
                    }
                    true
                }
            )
        }
    }
}