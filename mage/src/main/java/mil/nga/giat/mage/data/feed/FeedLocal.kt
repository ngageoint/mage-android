package mil.nga.giat.mage.data.feed

import androidx.room.*
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

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