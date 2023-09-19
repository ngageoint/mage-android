package mil.nga.giat.mage.database.model.feed

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import mil.nga.sf.Geometry

@Entity(tableName = "feed_item",
   primaryKeys = ["id", "feed_id"],
   foreignKeys = [
      ForeignKey(entity = Feed::class,
         parentColumns = ["id"],
         childColumns = ["feed_id"],
         onDelete = ForeignKey.CASCADE)
   ]
)
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

   @ColumnInfo(name = "feed_id")
   var feedId: String
) {
   @ColumnInfo(name = "timestamp")
   var timestamp: Long? = null
}

@Entity(tableName = "feed_item",
   primaryKeys = ["id", "feed_id"],
   foreignKeys = [
      ForeignKey(entity = Feed::class,
         parentColumns = ["id"],
         childColumns = ["feed_id"],
         onDelete = ForeignKey.CASCADE)
   ]
)
data class MappableFeedItem(
   @SerializedName("id")
   @ColumnInfo(name = "id")
   val id: String,

   @SerializedName("geometry")
   @ColumnInfo(name = "geometry", typeAffinity = ColumnInfo.BLOB)
   val geometry: Geometry,

   @SerializedName("properties")
   @ColumnInfo(name = "properties")
   val properties: JsonElement?,

   @ColumnInfo(name = "feed_id")
   var feedId: String
) {
   @ColumnInfo(name = "timestamp")
   var timestamp: Long? = null
}