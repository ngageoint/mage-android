package mil.nga.giat.mage.map

import android.content.Context
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import mil.nga.giat.mage.dagger.module.ApplicationContext
import mil.nga.giat.mage.data.feed.*
import mil.nga.giat.mage.feed.FeedViewModel
import mil.nga.giat.mage.map.preference.MapLayerPreferences
import javax.inject.Inject

class MapViewModel @Inject constructor(
        @ApplicationContext val context: Context,
        private val mapLayerPreferences: MapLayerPreferences,
        private val feedItemDao: FeedItemDao
): ViewModel() {

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
}