package mil.nga.giat.mage.data.feed

import android.content.Context
import androidx.annotation.WorkerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    suspend fun syncFeed(feed: Feed): Resource<out FeedContent> {
        return withContext(Dispatchers.IO) {
            val resource = try {
                val event = EventHelper.getInstance(context).currentEvent

                val response = feedService.getFeedItems(event.remoteId, feed.id).execute()
                if (response.isSuccessful) {
                    val content = response.body()!!
                    saveFeed(feed, content)
                    Resource.success(content)
                } else {
                    Resource.error(response.message(), null)
                }
            } catch (e: Exception) {
                Resource.error(e.localizedMessage, null)
            }

            val local = FeedLocal(feed.id)
            local.lastSync = Date().time
            feedLocalDao.upsert(local)

            resource
        }
    }

    @WorkerThread
    private fun saveFeed(feed: Feed, content: FeedContent) {
        if (!feed.itemsHaveIdentity) {
            feedItemDao.removeFeedItems(feed.id)
        }

        for (item in content.items) {
            item.feedId = feed.id

            item.timestamp = null
            if (feed.itemTemporalProperty != null) {
                item.timestamp = item.properties?.asJsonObject?.get(feed.itemTemporalProperty)?.asLong
            }

            feedItemDao.upsert(item)
        }

        val itemIds = content.items.map { it.id }
        feedItemDao.preserveFeedItems(feed.id, itemIds)
    }
}