package mil.nga.giat.mage.map.preference

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.database.model.feed.Feed
import mil.nga.giat.mage.database.dao.feed.FeedDao
import javax.inject.Inject

@HiltViewModel
class MapPreferencesViewModel @Inject constructor(
   feedDao: FeedDao,
   eventLocalDataSource: EventLocalDataSource
): ViewModel() {
    lateinit var feeds: LiveData<List<Feed>>

    init {
        val eventId = eventLocalDataSource.currentEvent?.remoteId
        if (!eventId.isNullOrBlank()) {
            feeds = feedDao.mappableFeeds(eventId)
        }
    }
}