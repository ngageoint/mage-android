package mil.nga.giat.mage.data.repository.feed

import android.util.Log
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
   companion object {
      private const val LOG_TAG = "FeedRepository"
   }

   suspend fun syncFeed(feed: Feed) = withContext(Dispatchers.IO) {
      val resource = try {
         eventLocalDataSource.currentEvent?.let { event ->
            val response = feedService.getFeedItems(event.remoteId, feed.id)
            Log.d(LOG_TAG, "Feed ${feed.title} sync response: ${response.code()} successful=${response.isSuccessful}")
            if (response.isSuccessful) {
               val content = response.body()
               Log.d(LOG_TAG, "Feed ${feed.title} body parsed: ${content != null}, items=${content?.items?.size ?: "null"}")
               content?.let {
                  saveFeed(feed, it)
                  Resource.success(it)
               } ?: Resource.error("Error parsing feed content body", null)
            } else {
               Log.e(LOG_TAG, "Feed ${feed.title} sync failed: ${response.code()} ${response.message()}")
               Resource.error(response.message(), null)
            }
         }
      } catch (e: Exception) {
         Log.e(LOG_TAG, "Feed ${feed.title} sync exception", e)
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