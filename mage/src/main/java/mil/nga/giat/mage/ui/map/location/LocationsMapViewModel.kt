package mil.nga.giat.mage.ui.map.location

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transform
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.repository.location.LocationRepository
import mil.nga.giat.mage.glide.transform.LocationAgeTransformation
import mil.nga.giat.mage.map.annotation.MapAnnotation
import mil.nga.giat.mage.ui.map.IconMarkerState
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class LocationsMapViewModel @Inject constructor(
    locationRepository: LocationRepository,
    private val application: Application
): ViewModel() {
    private var locationsStates = mutableMapOf<Long, IconMarkerState>()

    val locations = locationRepository.getLocations().transform { locations ->
        val newStates = mutableMapOf<Long, IconMarkerState>()
        locations.mapNotNull { location ->
            val annotation = MapAnnotation.fromUser(location.user, location)
            val geometry = annotation.geometry.centroid
            val icon = getLocationIcon(annotation, 52 * application.resources.displayMetrics.density.toInt())

            val state = locationsStates[location.user.id]
                ?: run {
                    IconMarkerState(
                        markerState = MarkerState(
                            position = LatLng(geometry.y, geometry.x)
                        ),
                        icon = BitmapDescriptorFactory.fromBitmap(icon),
                        id = location.user.id
                    )
                }
            state?.let {
                it.markerState?.position = LatLng(geometry.y, geometry.x)
                newStates[location.user.id] = it
            }
            newStates[location.user.id]
        }

        emit(newStates.values.toList())
    }.flowOn(Dispatchers.IO)

    private suspend fun getLocationIcon(
        annotation: MapAnnotation<*>,
        iconDimension: Int
    ) = suspendCoroutine { continuation ->
        val transformation = LocationAgeTransformation(application, annotation.timestamp)

        Glide.with(application)
            .load(annotation)
            .error(R.drawable.default_marker)
            .transform(transformation)
            .into(object : CustomTarget<Drawable>(iconDimension, iconDimension) {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    continuation.resume(resource.toBitmap())
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }
}