package mil.nga.giat.mage.database.model.feed

import androidx.room.*
import mil.nga.giat.mage.database.model.feed.Feed

@Entity(tableName = "feed_local",
   foreignKeys = [
       ForeignKey(entity = Feed::class,
          parentColumns = ["id"],
          childColumns = ["feed_id"],
          onDelete = ForeignKey.CASCADE)
   ]
)
data class FeedLocal(
    @ColumnInfo(name = "feed_id")
    @PrimaryKey val feedId: String
) {
    @ColumnInfo(name = "last_sync")
    var lastSync: Long? = null
}