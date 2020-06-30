package mil.nga.giat.mage.data.feed

import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import mil.nga.sf.Geometry

@Entity(tableName = "feed_item", primaryKeys = ["id", "feed_id"])
data class FeedItem(
        @SerializedName("id")
        @ColumnInfo(name = "id")
        val id: String,

        @SerializedName("geometry")
        @ColumnInfo(name = "geometry", typeAffinity = ColumnInfo.BLOB)
        val geometry: Geometry?,

        @SerializedName("properties")
        @ColumnInfo(name = "properties")
        val properties: JsonElement?,

        @ForeignKey(entity = Feed::class,
                parentColumns = ["id"],
                childColumns = ["feed_id"],
                onDelete = CASCADE)
        @ColumnInfo(name = "feed_id")
        var feedId: String
)