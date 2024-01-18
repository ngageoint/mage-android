package mil.nga.giat.mage.ui.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import mil.nga.giat.mage.search.Geocoder
import mil.nga.giat.mage.search.SearchResponse
import javax.inject.Inject

sealed class SearchState {
   data object Searching: SearchState()
   data class Complete(val response: SearchResponse): SearchState()
}

@HiltViewModel
class PlacenameSearchViewModel @Inject constructor(
   private val geocoder: Geocoder,
): ViewModel() {

   private val searchText = MutableLiveData<String>()
   val searchState = searchText.switchMap {
      liveData {
         emit(SearchState.Searching)
         val response = geocoder.search(it)
         emit(SearchState.Complete(response))
      }
   }

   fun search(text: String) {
      searchText.value = text
   }
}