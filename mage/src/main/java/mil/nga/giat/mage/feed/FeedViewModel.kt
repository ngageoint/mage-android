package mil.nga.giat.mage.feed

import android.content.Context
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mil.nga.giat.mage.dagger.module.ApplicationContext
import mil.nga.giat.mage.data.feed.*
import mil.nga.giat.mage.network.Resource
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import javax.inject.Inject

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
        GlobalScope.launch {
            _refresh.postValue(Resource.loading(null))
            val resource = feedRepository.syncFeed(_feedId.value!!)
            _refresh.postValue(resource)
        }
    }
}