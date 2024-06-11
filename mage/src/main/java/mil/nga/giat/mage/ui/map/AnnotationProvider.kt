package mil.nga.giat.mage.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import mil.nga.giat.mage.location.LocationProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnotationProvider @Inject constructor(val locationProvider: LocationProvider) {
    private val _annotation = MutableLiveData<MapAnnotation2?>()
    val annotation: LiveData<MapAnnotation2?> = _annotation
    fun setMapAnnotation(annotation: MapAnnotation2?) {
        _annotation.value = annotation
    }
}