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
    val locations by viewModel.locations.collectAsState(emptyList())

    locations.forEach { state ->
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