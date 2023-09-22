package mil.nga.giat.mage.map.preference

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import mil.nga.giat.mage.database.model.feed.Feed
import mil.nga.giat.mage.database.dao.feed.FeedDao
import javax.inject.Inject

@HiltViewModel
class MapPreferencesViewModel @Inject constructor(
   private val feedDao: FeedDao
): ViewModel() {

    private val eventId = MutableLiveData<String>()
    val feeds: LiveData<List<Feed>> = eventId.switchMap {
        feedDao.mappableFeeds(it)
    }

    fun setEvent(eventId: String?) {
        this.eventId.value = eventId
    }
}