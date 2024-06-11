package mil.nga.giat.mage.ui.map.location

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.VisibleRegion
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberMarkerState

@Composable
fun LocationsMap(
    viewModel: LocationsMapViewModel = hiltViewModel(),
    cameraPositionState: CameraPositionState,
    onMapTap: (latLng: LatLng, visibleRegion: VisibleRegion) -> Unit
) {

    val locationIcons = remember { mutableMapOf<Long, Bitmap>() }
    val locationStates = remember { mutableMapOf<Long, MarkerState>() }
    val locations by viewModel.locations.collectAsState(emptyList())

    locations.forEach { location ->
        val icon = location.icon?.let { BitmapDescriptorFactory.fromBitmap(it) }
        val position = location.geometry.centroid
        val state = locationStates[location.id]
        if (state == null) {
            locationStates[location.id] = rememberMarkerState(
                position = LatLng(position.y, position.x)
            )
        } else {
            state.position = LatLng(position.y, position.x)
        }
        state?.let { markerState ->
            key(markerState) {
                Marker(
                    state = markerState,
                    icon = icon,
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
}