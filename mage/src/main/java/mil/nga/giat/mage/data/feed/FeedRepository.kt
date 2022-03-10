package mil.nga.giat.mage.data.feed

import android.content.Context
import androidx.annotation.WorkerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mil.nga.giat.mage.network.Resource
import mil.nga.giat.mage.network.api.FeedService
import mil.nga.giat.mage.network.gson.asLongOrNull
import mil.nga.giat.mage.network.gson.asStringOrNull
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory
import java.text.ParseException
import java.util.*
import javax.inject.Inject

class FeedRepository @Inject constructor(
    @ApplicationContext private val  context: Context,
    private val feedLocalDao: FeedLocalDao,
    private val feedItemDao: FeedItemDao,
    private val feedService: FeedService
) {
    suspend fun syncFeed(feed: Feed) = withContext(Dispatchers.IO) {
        val resource = try {
            val event = EventHelper.getInstance(context).currentEvent

            val response = feedService.getFeedItems(event.remoteId, feed.id)
            if (response.isSuccessful) {
                val content = response.body()!!
                saveFeed(feed, content)
                Resource.success(content)
            } else {
                Resource.error(response.message(), null)
            }
        } catch (e: Exception) {
            Resource.error(e.localizedMessage ?: e.toString(), null)
        }

        val local = FeedLocal(feed.id)
        local.lastSync = Date().time
        feedLocalDao.upsert(local)

        resource
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
                val temporalElement = item.properties?.asJsonObject?.get(feed.itemTemporalProperty)
                item.timestamp = temporalElement?.asLongOrNull() ?: run {
                    temporalElement?.asStringOrNull()?.let { date ->
                        try {
                            ISO8601DateFormatFactory.ISO8601().parse(date)?.time
                        } catch (ignore: ParseException) { null }
                    }
                }
            }

            feedItemDao.upsert(item)
        }

        val itemIds = content.items.map { it.id }
        feedItemDao.preserveFeedItems(feed.id, itemIds)
    }
}