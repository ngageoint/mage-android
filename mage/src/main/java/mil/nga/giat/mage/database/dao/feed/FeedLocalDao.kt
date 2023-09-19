package mil.nga.giat.mage.database.dao.feed

import androidx.room.*
import mil.nga.giat.mage.database.model.feed.FeedAndLocal
import mil.nga.giat.mage.database.model.feed.FeedLocal

@Dao
interface FeedLocalDao {
    @Transaction
    fun upsert(feed: FeedLocal) {
        val id = insert(feed)
        if (id == -1L) {
            update(feed)
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(feed: FeedLocal): Long

    @Update(onConflict = OnConflictStrategy.IGNORE)
    fun update(feed: FeedLocal)

    @Transaction
    @Query("SELECT * FROM feed WHERE event_remote_id = :eventId")
    fun getFeeds(eventId: String): List<FeedAndLocal>

    @Query("DELETE FROM feed_local")
    fun destroy()
}