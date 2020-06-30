package mil.nga.giat.mage.data.feed

import android.content.Context
import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mil.nga.giat.mage.dagger.module.ApplicationContext
import mil.nga.giat.mage.network.Resource
import mil.nga.giat.mage.network.api.FeedService
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import java.lang.Exception
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepository @Inject constructor(
    @ApplicationContext private val  context: Context,
    private val feedLocalDao: FeedLocalDao,
    private val feedItemDao: FeedItemDao,
    private val feedService: FeedService
) {
    suspend fun syncFeed(feedId: String): Resource<out Array<FeedItem>> {
        return withContext(Dispatchers.IO) {
            val resource = try {
                val event = EventHelper.getInstance(context).currentEvent

                val response = feedService.getFeedItems(event.remoteId, feedId).execute()
                if (response.isSuccessful) {
                    val items = response.body()!!
                    saveFeed(feedId, items)
                    Resource.success(items)
                } else {
                    Resource.error(response.message(), null)
                }
            } catch (e: Exception) {
                Resource.error(e.localizedMessage, null)
            }

            val local = FeedLocal(feedId)
            local.lastSync = Date().time
            feedLocalDao.upsert(local)

            resource
        }
    }

    @WorkerThread
    private fun saveFeed(feedId: String, items: Array<FeedItem>) {
        // TODO delete all items if non-stable feed items ids
        for (item in items) {
            item.feedId = feedId
            feedItemDao.upsert(item)
        }
    }
}