package mil.nga.giat.mage.database.dao.feed

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import mil.nga.giat.mage.database.model.feed.FeedItem
import mil.nga.giat.mage.database.model.feed.FeedWithItems
import mil.nga.giat.mage.database.model.feed.ItemWithFeed

@Dao
interface FeedItemDao {
    @Transaction
    fun upsert(item: FeedItem) {
        val id = insert(item)
        if (id == -1L) {
            update(item)
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(item: FeedItem): Long

    @Update(onConflict = OnConflictStrategy.IGNORE)
    fun update(item: FeedItem)

    @Query("SELECT * FROM feed_item WHERE feed_id = :feedId")
    fun items(feedId: String): LiveData<List<FeedItem>>

    @Query("SELECT * FROM feed_item WHERE feed_id = :feedId")
    fun feedItems(feedId: String): List<FeedItem>

    @Transaction
    @Query("SELECT * FROM feed WHERE id = :feedId")
    fun feedWithItems(feedId: String): LiveData<FeedWithItems>

    @Query("SELECT * FROM feed_item WHERE feed_id = :feedId ORDER BY timestamp DESC, id ASC")
    fun pagingSource(feedId: String): PagingSource<Int, FeedItem>

    @Query("SELECT feed.*, feed_item.id AS item_id, feed_item.geometry AS item_geometry, feed_item.properties AS item_properties, feed_item.timestamp AS item_timestamp, feed_item.feed_id AS item_feed_id FROM feed_item JOIN feed ON feed.id = feed_item.feed_id AND feed.id = :feedId WHERE feed_item.id = :feedItemId")
    suspend fun getFeedItem(feedId: String, feedItemId: String): ItemWithFeed

    @Query("SELECT feed.*, feed_item.id AS item_id, feed_item.geometry AS item_geometry, feed_item.properties AS item_properties, feed_item.timestamp AS item_timestamp, feed_item.feed_id AS item_feed_id FROM feed_item JOIN feed ON feed.id = feed_item.feed_id AND feed.id = :feedId WHERE feed_item.id = :feedItemId")
    fun item(feedId: String, feedItemId: String): Flow<ItemWithFeed>

    @Query("DELETE FROM feed_item WHERE feed_id = :feedId")
    fun removeFeedItems(feedId: String)

    @Query("DELETE FROM feed_item WHERE feed_id = :feedId AND id NOT IN (:itemIds)")
    fun preserveFeedItems(feedId: String, itemIds: List<String>)

    @Query("DELETE FROM feed_item")
    fun destroy()
}