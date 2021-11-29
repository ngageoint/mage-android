package mil.nga.giat.mage.map

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import mil.nga.giat.mage.data.feed.FeedItemDao
import mil.nga.giat.mage.data.feed.FeedWithItems
import mil.nga.giat.mage.data.layer.LayerRepository
import mil.nga.giat.mage.data.location.LocationRepository
import mil.nga.giat.mage.data.observation.ObservationRepository
import mil.nga.giat.mage.map.preference.MapLayerPreferences
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import javax.inject.Inject
import kotlin.collections.set

@HiltViewModel
class MapViewModel @Inject constructor(
    private val application: Application,
    private val preferences: SharedPreferences,
    private val mapLayerPreferences: MapLayerPreferences,
    private val feedItemDao: FeedItemDao,
    private val geocoder: Geocoder,
    observationRepository: ObservationRepository,
    locationRepository: LocationRepository,
    private val layerRepository: LayerRepository
): ViewModel() {
    val eventHelper: EventHelper = EventHelper.getInstance(application)

    val observationEvents = observationRepository.getObservationEvents()
    val locationEvents = locationRepository.getLocationEvents()

    fun getStaticFeatureEvents(layerId: Long): Flow<LayerRepository.StaticFeatureEvent> {
        return layerRepository.getStaticFeatureEvents(layerId)
    }

    private val _feeds = MutableLiveData<MutableMap<String, LiveData<FeedWithItems>>>()
    val feeds: LiveData<MutableMap<String, LiveData<FeedWithItems>>> = _feeds

    private val _feedIds = MutableLiveData<Set<String>>()
    fun setEvent(eventId: String) {
        _feedIds.value = mapLayerPreferences.getEnabledFeeds(eventId)
    }

    val items: LiveData<MutableMap<String, LiveData<FeedWithItems>>> = Transformations.switchMap(_feedIds) { feedIds ->
        val items = mutableMapOf<String, LiveData<FeedWithItems>>()
        feedIds.forEach { feedId ->
            var liveData = _feeds.value?.get(feedId)
            if (liveData == null) {
                liveData = feedItemDao.feedWithItems(feedId)
            }

            items[feedId] = liveData
        }

        _feeds.value = items
        feeds
    }

    private val searchText = MutableLiveData<String>()
    val searchResult = Transformations.switchMap(searchText) {
        liveData {
            emit(geocoder.search(it))
        }
    }

    fun search(text: String) {
        searchText.value = text
    }
}