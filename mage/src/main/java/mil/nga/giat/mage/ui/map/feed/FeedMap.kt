package mil.nga.giat.mage.ui.map.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.VisibleRegion
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Marker

@Composable
fun FeedMap(
    id: String,
    cameraPositionState: CameraPositionState,
    onMapTap: (latLng: LatLng, visibleRegion: VisibleRegion) -> Unit
) {
    val viewModel = hiltViewModel<FeedMapViewModel, FeedMapViewModel.FeedMapViewModelFactory> { factory ->
        factory.create(id)
    }

    val feedItems by viewModel.feedItems.collectAsState(initial = emptyList())

    feedItems.forEach { state ->
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