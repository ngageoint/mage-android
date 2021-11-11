package mil.nga.giat.mage.data.feed

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface FeedDao {
    @Transaction
    fun upsert(feed: Feed): Boolean {
        val id = insert(feed)
        if (id == -1L) {
            update(feed)
        }

        return id != -1L;
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(feed: Feed): Long

    @Update(onConflict = OnConflictStrategy.IGNORE)
    fun update(feed: Feed)

    @Query("SELECT * FROM feed WHERE event_remote_id = :eventId")
    fun feeds(eventId: String): List<Feed>

    @Query("SELECT * FROM feed WHERE event_remote_id = :eventId")
    fun feedsLiveData(eventId: String): LiveData<List<Feed>>

    @Query("SELECT * FROM feed WHERE event_remote_id = :eventId AND items_have_spatial_dimension = 1")
    fun mappableFeeds(eventId: String): LiveData<List<Feed>>

    @Query("SELECT * FROM feed WHERE id = :feedId")
    fun feed(feedId: String): LiveData<Feed>

    @Query("SELECT * FROM feed WHERE id IN(:feedIds)")
    fun feeds(feedIds: List<String>): LiveData<List<Feed>>

    @Transaction
    @Query("SELECT * FROM feed WHERE id = :feedId")
    fun feedWithItems(feedId: String): List<FeedWithItems>

    @Query("DELETE FROM feed WHERE event_remote_id = :eventId AND id NOT IN (:feedIds)")
    fun preserveFeeds(eventId: String, feedIds: List<String>)

    @Query("DELETE FROM feed")
    fun destroy()
}