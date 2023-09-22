package mil.nga.giat.mage.data.repository.feed

import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mil.nga.giat.mage.database.model.feed.Feed
import mil.nga.giat.mage.database.model.feed.FeedContent
import mil.nga.giat.mage.database.dao.feed.FeedItemDao
import mil.nga.giat.mage.database.model.feed.FeedLocal
import mil.nga.giat.mage.database.dao.feed.FeedLocalDao
import mil.nga.giat.mage.network.Resource
import mil.nga.giat.mage.network.feed.FeedService
import mil.nga.giat.mage.network.gson.asLongOrNull
import mil.nga.giat.mage.network.gson.asStringOrNull
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory
import java.text.ParseException
import java.util.*
import javax.inject.Inject

class FeedRepository @Inject constructor(
   private val feedLocalDao: FeedLocalDao,
   private val feedItemDao: FeedItemDao,
   private val feedService: FeedService,
   private val eventLocalDataSource: EventLocalDataSource
) {
   suspend fun syncFeed(feed: Feed) = withContext(Dispatchers.IO) {
      val resource = try {
         eventLocalDataSource.currentEvent?.let { event ->
            val response = feedService.getFeedItems(event.remoteId, feed.id)
            if (response.isSuccessful) {
               response.body()?.let { content ->
                  saveFeed(feed, content)
                  Resource.success(content)
               } ?: Resource.error("Error parsing feed content body", null)
            } else {
               Resource.error(response.message(), null)
            }
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