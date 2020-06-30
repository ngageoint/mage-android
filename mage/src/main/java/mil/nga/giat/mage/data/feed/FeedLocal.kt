package mil.nga.giat.mage.data.feed

import androidx.room.*
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

@Entity(tableName = "feed_local")
data class FeedLocal(
    @ColumnInfo(name = "feed_id")
    @PrimaryKey val feedId: String
) {
    @ColumnInfo(name = "last_sync")
    var lastSync: Long? = null
}