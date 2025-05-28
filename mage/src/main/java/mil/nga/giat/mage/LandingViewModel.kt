package mil.nga.giat.mage

import android.app.Application
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mil.nga.giat.mage.database.model.feed.Feed
import mil.nga.giat.mage.database.dao.feed.FeedDao
import mil.nga.giat.mage.database.dao.feed.FeedItemDao
import mil.nga.giat.mage.map.FeedItemId
import mil.nga.giat.mage.map.annotation.MapAnnotation
import mil.nga.giat.mage.data.datasource.location.LocationLocalDataSource
import mil.nga.giat.mage.data.datasource.observation.ObservationLocalDataSource
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.data.repository.user.UserRepository
import mil.nga.sf.Geometry
import javax.inject.Inject

@HiltViewModel
class LandingViewModel @Inject constructor(
   private val application: Application,
   private val feedDao: FeedDao,
   private val feedItemDao: FeedItemDao,
   private val userRepository: UserRepository,
   private val userLocalDataSource: UserLocalDataSource,
   private val eventLocalDataSource: EventLocalDataSource,
   private val locationLocalDataSource: LocationLocalDataSource,
   private val observationLocalDataSource: ObservationLocalDataSource
): ViewModel() {

   enum class NavigationTab { MAP, OBSERVATIONS, PEOPLE }
   enum class NavigableType { OBSERVATION, USER, FEED, OTHER }
   data class Navigable<T: Any>(val id: T, val type: NavigableType, val geometry: Geometry, val icon: Any?)

   private val _filterText = MutableLiveData<String>()
   val filterText: LiveData<String> = _filterText
   fun setFilterText(subtitle: String) {
      _filterText.value = subtitle
   }

   private val _navigationTab = MutableLiveData<NavigationTab>()
   val navigationTab: LiveData<NavigationTab> = _navigationTab
   fun setNavigationTab(tab: NavigationTab) {
      _navigationTab.value = tab
   }

   private val eventId = MutableLiveData<String>()
   val feeds: LiveData<List<Feed>> = eventId.switchMap {
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
         eventLocalDataSource.currentEvent?.let { event ->
            val observation = observationLocalDataSource.read(id)
            val observationForm = observation.forms.firstOrNull()
            val formDefinition = observationForm?.formId?.let {
               eventLocalDataSource.getForm(it)
            }

            val icon = MapAnnotation.getAnnotationWithStyleFromObservation(
               event = event,
               formDefinition = formDefinition,
               observationForm = observationForm,
               geometryType = observation.geometry.geometryType,
               observation = observation,
               context = application
            )

            _navigateTo.postValue(
               Navigable(
                  id = id.toString(),
                  type = NavigableType.OBSERVATION,
                  geometry = observation.geometry,
                  icon = icon
               )
            )
         }
      }
   }

   fun startUserNavigation(id: Long) {
      setNavigationTab(NavigationTab.MAP)

      viewModelScope.launch(Dispatchers.IO) {
         val user = userLocalDataSource.read(id)
         eventLocalDataSource.currentEvent?.let { event ->
            val location = locationLocalDataSource.getUserLocations(user.id, event.id, 1, true).first()
            _navigateTo.postValue(
               Navigable(
                  id.toString(),
                  NavigableType.USER,
                  location.geometry,
                  MapAnnotation.getAnnotationWithBaseStyleFromUser(user, location)
               )
            )
         }
      }
   }

   fun startFeedNavigation(feedId: String, itemId: String) {
      viewModelScope.launch {
         val itemWithFeed = feedItemDao.item(feedId, itemId).first()
         val icon = MapAnnotation.getAnnotationWithBaseStyleFromFeedItem(itemWithFeed, application)
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