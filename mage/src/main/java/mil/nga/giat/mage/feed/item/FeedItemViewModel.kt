package mil.nga.giat.mage.feed.item

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import mil.nga.giat.mage.dagger.module.ApplicationContext
import mil.nga.giat.mage.data.feed.*
import javax.inject.Inject

class FeedItemViewModel @Inject constructor(
        @ApplicationContext val context: Context,
        private val feedItemDao: FeedItemDao
): ViewModel() {

    fun getFeedItem(feedId: String, feedItemId: String): LiveData<ItemWithFeed> {
        return feedItemDao.item(feedId, feedItemId)
    }
}