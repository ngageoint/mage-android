package mil.nga.giat.mage.data.repository.map

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mil.nga.geopackage.BoundingBox
import mil.nga.geopackage.GeoPackageManager
import mil.nga.giat.mage.data.repository.location.LocationRepository
import mil.nga.giat.mage.ui.map.AnnotationProvider
import mil.nga.giat.mage.ui.map.MapAnnotation2
import mil.nga.sf.GeometryEnvelope
import mil.nga.sf.Point
import mil.nga.sf.geojson.Feature
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class BottomSheetRepository @Inject constructor(
    private val observationsTileRepository: ObservationsTileRepository,
    private val locationsRepository: LocationRepository
) {
    private val _mapAnnotations = MutableLiveData<List<MapAnnotation2>>()
    val mapAnnotations: LiveData<List<MapAnnotation2>> = _mapAnnotations

    fun clearAnnotations() {
        _mapAnnotations.value = emptyList()
    }

    suspend fun setLocation(point: LatLng, bounds: LatLngBounds, longitudePerPixel: Float, latitudePerPixel: Float, zoom: Float): Int {
        val annotations = getMapAnnotations(
            point = point,
            bounds = bounds,
            longitudePerPixel = longitudePerPixel,
            latitudePerPixel = latitudePerPixel,
            zoom = zoom
        )

        _mapAnnotations.value = annotations
        return annotations.size
    }

    private suspend fun getMapAnnotations(
        point: LatLng,
        bounds: LatLngBounds,
        longitudePerPixel: Float,
        latitudePerPixel: Float,
        zoom: Float
    ) = withContext(Dispatchers.IO) {

        val observationMapItems = observationsTileRepository.getObservationMapItems(
            minLatitude = bounds.southwest.latitude,
            minLongitude = bounds.southwest.longitude,
            maxLatitude = bounds.northeast.latitude,
            maxLongitude = bounds.northeast.longitude,
            zoom = zoom,
            longitudePerPixel = longitudePerPixel,
            latitudePerPixel = latitudePerPixel,
            precise = true
        )?.map {
            val key = MapAnnotation2.Key(it.observationId.toString(), MapAnnotation2.Type.OBSERVATION)
            MapAnnotation2(key, it.latitude, it.longitude)
        } ?: emptyList()

        val locations = locationsRepository.getLocations(
            minLatitude = bounds.southwest.latitude,
            minLongitude = bounds.southwest.longitude,
            maxLatitude = bounds.northeast.latitude,
            maxLongitude = bounds.northeast.longitude
        ).map {
            val location = it.geometry.centroid
            val key = MapAnnotation2.Key(it.id.toString(), MapAnnotation2.Type.LOCATION)
            MapAnnotation2(key, location.y, location.x)
        }

        // get geopackage items
        /**
         * val features = cacheOverlays.values.flatMap { overlay ->
         *          overlay?.getFeaturesNearClick(latLng, binding.mapView, map, application) ?: emptyList()
         *       }
         */


        observationMapItems + locations
    }
}