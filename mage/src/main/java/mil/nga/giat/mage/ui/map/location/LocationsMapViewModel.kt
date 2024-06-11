package mil.nga.giat.mage.ui.map.location

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transform
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.repository.location.LocationRepository
import mil.nga.giat.mage.glide.transform.LocationAgeTransformation
import mil.nga.giat.mage.map.annotation.MapAnnotation
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class LocationsMapViewModel @Inject constructor(
    locationRepository: LocationRepository,
    private val application: Application
): ViewModel() {
    val locations = locationRepository.getLocations().transform { locations ->
        val states = locations.map { location ->
            val annotation = MapAnnotation.fromUser(location.user, location)

            val icon = getLocationIcon(annotation, 52 * application.resources.displayMetrics.density.toInt())
            annotation.icon = icon
            annotation
        }

        emit(states)
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