package mil.nga.giat.mage.map.preference

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import mil.nga.giat.mage.dagger.module.ApplicationContext
import mil.nga.giat.mage.data.feed.Feed
import mil.nga.giat.mage.data.feed.FeedDao
import mil.nga.giat.mage.data.feed.FeedItemDao
import mil.nga.giat.mage.data.feed.FeedWithItems
import javax.inject.Inject

class MapPreferencesViewModel @Inject constructor(
        @ApplicationContext val context: Context,
        private val feedDao: FeedDao
): ViewModel() {

    private val eventId = MutableLiveData<String>()
    val feeds: LiveData<List<Feed>> = Transformations.switchMap(eventId) {
        feedDao.mappableFeeds(it)
    }

    fun setEvent(eventId: String) {
        this.eventId.value = eventId
    }
}