package mil.nga.giat.mage.ui.map.observation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.VisibleRegion
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
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
    val observationShapes by viewModel.observationShapes.collectAsState(emptyMap())

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

    observationShapes.forEach { (id, annotation) ->
        key(id) {
            when (annotation.geometry) {
                is mil.nga.sf.Polygon -> {
                    Polygon(
                        points = annotation.geometry.getRing(0).points.map { point -> LatLng(point.y, point.x) },
                        fillColor = Color(annotation.shapeStyle?.fillColor ?: Color.Transparent.toArgb()),
                        strokeColor = Color(annotation.shapeStyle?.strokeColor ?: Color.Transparent.toArgb()),
                    )
                }
                is mil.nga.sf.LineString -> {
                    Polyline(
                        points = annotation.geometry.points.map { point -> LatLng(point.y, point.x) },
                        color = Color(annotation.shapeStyle?.strokeColor ?: Color.Transparent.toArgb())
                    )
                }
                else -> {}
            }
        }
    }
}