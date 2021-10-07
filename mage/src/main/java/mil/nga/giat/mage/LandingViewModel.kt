package mil.nga.giat.mage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mil.nga.giat.mage.data.feed.Feed
import mil.nga.giat.mage.data.feed.FeedDao
import javax.inject.Inject

@HiltViewModel
class LandingViewModel @Inject constructor(
    private val feedDao: FeedDao
): ViewModel() {

    private val eventId = MutableLiveData<String>()
    val feeds: LiveData<List<Feed>> = Transformations.switchMap(eventId) {
        feedDao.feedsLiveData(it)
    }

    fun setEvent(eventId: String) {
        this.eventId.value = eventId
    }
}