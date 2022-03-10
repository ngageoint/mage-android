package mil.nga.giat.mage

import android.app.Application
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import mil.nga.giat.mage.data.feed.Feed
import mil.nga.giat.mage.data.feed.FeedDao
import mil.nga.giat.mage.data.feed.FeedItemDao
import mil.nga.giat.mage.map.FeedItemId
import mil.nga.giat.mage.map.annotation.MapAnnotation
import mil.nga.sf.Geometry
import javax.inject.Inject

@HiltViewModel
class LandingViewModel @Inject constructor(
   private val application: Application,
   private val feedDao: FeedDao,
   private val feedItemDao: FeedItemDao,
   private val savedStateHandle: SavedStateHandle,
): ViewModel() {

   enum class NavigationTab { MAP, OBSERVATIONS, PEOPLE }
   enum class NavigableType { OBSERVATION, USER, FEED, OTHER }
   data class Navigable<T: Any>(val id: T, val type: NavigableType, val geometry: Geometry, val icon: Any?)

   val navigationTab: LiveData<NavigationTab> = savedStateHandle.getLiveData("navigation_tab")
   fun setNavigationTab(tab: NavigationTab) {
      savedStateHandle.set("navigation_tab", tab)
   }

   private val eventId = MutableLiveData<String>()
   val feeds: LiveData<List<Feed>> = Transformations.switchMap(eventId) {
      feedDao.feedsLiveData(it)
   }

   fun setEvent(eventId: String) {
      this.eventId.value = eventId
   }

   private val _navigateTo = MutableLiveData<Navigable<Any>?>()
   val navigateTo: LiveData<Navigable<Any>?> = _navigateTo
   fun startNavigation(navigable: Navigable<Any>) {
      setNavigationTab(NavigationTab.MAP)
      _navigateTo.value = navigable
   }

   fun startFeedNavigation(feedId: String, itemId: String) {
      viewModelScope.launch {
         feedItemDao.item(feedId, itemId).asFlow().collect { itemWithFeed ->
            val icon = MapAnnotation.fromFeedItem(itemWithFeed, application)
            val navigable = Navigable<Any>(
               FeedItemId(itemWithFeed.feed.id, itemWithFeed.item.id),
               NavigableType.FEED,
               itemWithFeed.item.geometry!!,
               icon
            )

            _navigateTo.postValue(navigable)
         }
      }
   }

   fun stopNavigation() {
      _navigateTo.value = null
   }
}