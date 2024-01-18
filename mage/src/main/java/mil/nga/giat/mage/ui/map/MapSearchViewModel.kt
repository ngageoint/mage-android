package mil.nga.giat.mage.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import mil.nga.giat.mage.data.repository.map.MapRepository
import mil.nga.giat.mage.search.GeocoderResult
import javax.inject.Inject

@HiltViewModel
class MapSearchViewModel @Inject constructor(
   mapRepository: MapRepository
): ViewModel() {

   val baseMap = mapRepository.baseMapType.asLiveData()

   private val _searchResult = MutableLiveData<GeocoderResult>()
   val searchResult: LiveData<GeocoderResult> = _searchResult

   fun setSearchResult(result: GeocoderResult) {
      _searchResult.value = result
   }
}