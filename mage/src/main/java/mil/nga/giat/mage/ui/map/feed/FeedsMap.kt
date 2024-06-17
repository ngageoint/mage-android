package mil.nga.giat.mage.ui.map.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.VisibleRegion
import com.google.maps.android.compose.CameraPositionState

@Composable
fun FeedsMap(
    viewModel: FeedsMapViewModel = hiltViewModel(),
    cameraPositionState: CameraPositionState,
    onMapTap: (latLng: LatLng, visibleRegion: VisibleRegion) -> Unit
) {
    val feedIds by viewModel.feedIds.collectAsState(emptyList())

    feedIds?.forEach { feedId ->
        FeedMap(
            id = feedId,
            cameraPositionState = cameraPositionState,
            onMapTap = onMapTap
        )
    }
}