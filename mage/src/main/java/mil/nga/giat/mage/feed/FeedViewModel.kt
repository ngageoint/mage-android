package mil.nga.giat.mage.feed

import android.content.Context
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mil.nga.giat.mage.data.feed.*
import mil.nga.giat.mage.network.Resource
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val feedDao: FeedDao,
    private val feedItemDao: FeedItemDao,
    private val feedRepository: FeedRepository
): ViewModel() {

    companion object {
        private const val FEED_ITEM_PAGE_SIZE = 30
    }

    data class FeedWithPagedItems(val feed: Feed, val pagedItems: PagedList<FeedItem>)

    private val _feedId = MutableLiveData<String>()
    val feed: LiveData<Feed> = Transformations.switchMap(_feedId) { feedId ->
        feedDao.feed(feedId)
    }

    val feedItems: LiveData<FeedWithPagedItems> = Transformations.switchMap(feed) { feed ->
        Transformations.map(LivePagedListBuilder(feedItemDao.pagedItems(feed.id), FEED_ITEM_PAGE_SIZE).build()) { pagedList ->
            FeedWithPagedItems(feed, pagedList)
        }
    }

    fun setFeedId(feedId: String) {
        _feedId.postValue(feedId)
    }

    private val _refresh = MutableLiveData<Resource<*>>()
    val refresh: LiveData<Resource<*>> = _refresh
    fun refresh() {
        val feed = this.feed.value ?: return
        GlobalScope.launch {
            _refresh.postValue(Resource.loading(null))
            val resource = feedRepository.syncFeed(feed)
            _refresh.postValue(resource)
        }
    }
}