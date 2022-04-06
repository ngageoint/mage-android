package mil.nga.giat.mage

import android.app.Application
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mil.nga.giat.mage.data.feed.Feed
import mil.nga.giat.mage.data.feed.FeedDao
import mil.nga.giat.mage.data.feed.FeedItemDao
import mil.nga.giat.mage.map.FeedItemId
import mil.nga.giat.mage.map.annotation.MapAnnotation
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import mil.nga.giat.mage.sdk.datastore.user.UserHelper
import mil.nga.sf.Geometry
import javax.inject.Inject

@HiltViewModel
class LandingViewModel @Inject constructor(
   private val application: Application,
   private val feedDao: FeedDao,
   private val feedItemDao: FeedItemDao
): ViewModel() {

   enum class NavigationTab { MAP, OBSERVATIONS, PEOPLE }
   enum class NavigableType { OBSERVATION, USER, FEED, OTHER }
   data class Navigable<T: Any>(val id: T, val type: NavigableType, val geometry: Geometry, val icon: Any?)

   val userHelper: UserHelper = UserHelper.getInstance(application)
   val eventHelper: EventHelper = EventHelper.getInstance(application)
   val locationHelper: LocationHelper = LocationHelper.getInstance(application)
   val observationHelper: ObservationHelper = ObservationHelper.getInstance(application)

   private val _navigationTab = MutableLiveData<NavigationTab>()
   val navigationTab: LiveData<NavigationTab> = _navigationTab
   fun setNavigationTab(tab: NavigationTab) {
      _navigationTab.value = tab
   }

   private val eventId = MutableLiveData<String>()
   val feeds: LiveData<List<Feed>> = Transformations.switchMap(eventId) {
      feedDao.feedsLiveData(it)
   }

   fun setEvent(eventId: String) {
      this.eventId.value = eventId
   }

   private val _navigateTo = MutableLiveData<Navigable<*>?>()
   val navigateTo: LiveData<Navigable<*>?> = _navigateTo
   fun startNavigation(navigable: Navigable<*>) {
      setNavigationTab(NavigationTab.MAP)
      _navigateTo.value = navigable
   }

   fun startObservationNavigation(id: Long) {
      setNavigationTab(NavigationTab.MAP)

      viewModelScope.launch(Dispatchers.IO) {
         val observation = observationHelper.read(id)
         _navigateTo.postValue(
            Navigable(
               id.toString(),
               NavigableType.OBSERVATION,
               observation.geometry,
               MapAnnotation.fromObservation(observation, application)
            )
         )
      }
   }

   fun startUserNavigation(id: Long) {
      setNavigationTab(NavigationTab.MAP)

      viewModelScope.launch(Dispatchers.IO) {
         val user = userHelper.read(id)
         val event = eventHelper.currentEvent
         val location = locationHelper.getUserLocations(user.id, event.id, 1, true).first()
         _navigateTo.postValue(
            Navigable(
               id.toString(),
               NavigableType.USER,
               location.geometry,
               MapAnnotation.fromUser(user, location)
            )
         )
      }
   }

   fun startFeedNavigation(feedId: String, itemId: String) {
      viewModelScope.launch {
         val itemWithFeed = feedItemDao.item(feedId, itemId).first()
         val icon = MapAnnotation.fromFeedItem(itemWithFeed, application)
         _navigateTo.postValue(
            Navigable(
               FeedItemId(itemWithFeed.feed.id, itemWithFeed.item.id),
               NavigableType.FEED,
               itemWithFeed.item.geometry!!,
               icon
            )
         )
      }
   }

   fun stopNavigation() {
      _navigateTo.value = null
   }
}