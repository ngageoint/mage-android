package mil.nga.giat.mage.ui.map.geoPackage

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.VisibleRegion
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.TileOverlay

@Composable
fun GeoPackagesMap(
    viewModel: GeoPackagesMapViewModel = hiltViewModel(),
    isMapLoaded: Boolean,
    cameraPositionState: CameraPositionState,
    onMapTap: (latlng: LatLng, visibleRegion: VisibleRegion) -> Unit
) {
    val tileProviders by viewModel.tileProviders.collectAsState(initial = emptyMap())

    if (isMapLoaded) {
        tileProviders.forEach { tileProvider ->
            TileOverlay(
                tileProvider = tileProvider.value,
                onClick = {
                    Log.d("MapScreen", "tile overlay click")
                }
            )
        }
    }
}