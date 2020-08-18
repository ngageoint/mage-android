package mil.nga.giat.mage.data.feed

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.PagedList
import androidx.room.*

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
    fun pagedItems(feedId: String): DataSource.Factory<Int, FeedItem>

    @Query("SELECT * FROM feed_item JOIN feed ON feed.id = feed_item.feed_id AND feed.id = :feedId WHERE feed_item.id = :feedItemId")
    fun item(feedId: String, feedItemId: String): LiveData<ItemWithFeed>

    @Query("DELETE FROM feed_item WHERE feed_id = :feedId")
    fun removeFeedItems(feedId: String)

    @Query("DELETE FROM feed_item")
    fun destroy()
}