package mil.nga.giat.mage.feed.item

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import mil.nga.giat.mage.data.feed.*
import javax.inject.Inject

@HiltViewModel
class FeedItemViewModel @Inject constructor(
   @ApplicationContext val context: Context,
   private val feedItemDao: FeedItemDao
): ViewModel() {

    fun getFeedItem(feedId: String, feedItemId: String): LiveData<ItemWithFeed> {
        return feedItemDao.item(feedId, feedItemId)
    }
}