package mil.nga.giat.mage.ui.geoPackage

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mil.nga.giat.mage.data.repository.cache.CacheOverlayRepository
import mil.nga.giat.mage.data.repository.cache.GeoPackageFeatureKey
import javax.inject.Inject

@HiltViewModel
class GeoPackageFeatureViewModel @Inject constructor(
    private val cacheOverlayRepository: CacheOverlayRepository
): ViewModel() {
    private val referenceFlow = MutableSharedFlow<String>(replay = 1)
    fun setGeoPackageFeatureId(id: String) {
        Log.d("GeoPackageFeatureViewModel", "setId: $id")
        viewModelScope.launch {
            referenceFlow.emit(id)
        }
    }

    val feature = referenceFlow.map { id ->
        Log.d("GeoPackageFeatureViewModel", "reference flow feature: $id")
        cacheOverlayRepository.getFeature(GeoPackageFeatureKey.fromKey(id))
    }.asLiveData()
}