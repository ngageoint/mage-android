package mil.nga.giat.mage.data.feed

import androidx.room.*
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

@Entity(tableName = "feed"
//        foreignKeys = [
//            ForeignKey(entity = FeedLocal::class,
//                    parentColumns = arrayOf("id"),
//                    childColumns = arrayOf("feed_id"),
//                    onDelete = CASCADE)
//        ]
)
data class Feed(
        @SerializedName("id")
        @PrimaryKey
        @ColumnInfo(name = "id")
        val id: String,

        @SerializedName("title")
        @ColumnInfo(name = "title")
        val title: String,

        @SerializedName("summary")
        @ColumnInfo(name = "summary")
        val summary: String?,

        @SerializedName("itemsHaveIdentity")
        @ColumnInfo(name = "items_have_identity")
        val itemsHaveIdentity: Boolean,

        @SerializedName("itemsHaveSpatialDimension")
        @ColumnInfo(name = "items_have_spatial_dimension")
        val itemsHaveSpatialDimension: Boolean,

        @ColumnInfo(name = "event_remote_id")
        var eventRemoteId: String
) {

    @SerializedName("updateFrequencySeconds")
    @ColumnInfo(name = "update_frequency")
    var updateFrequency: Long? = null
        get() {
            return if (field == null) 10 * 60 else field
        }

    @SerializedName("itemTemporalProperty")
    @ColumnInfo(name = "item_temporal_property")
    var itemTemporalProperty: String? = null

    @SerializedName("itemPrimaryProperty")
    @ColumnInfo(name = "item_primary_property")
    var itemPrimaryProperty: String? = null

    @SerializedName("itemSecondaryProperty")
    @ColumnInfo(name = "item_secondary_property")
    var itemSecondaryProperty: String? = null

    @SerializedName("style")
    @Embedded(prefix = "style_")
    var style: Style? = null
        get() {
            return if (field == null) Style() else field
        }

    @SerializedName("constant_params")
    @ColumnInfo(name = "constant_params")
    var constantParams: JsonElement? = null

    @SerializedName("variable_params")
    @ColumnInfo(name = "variable_params")
    var variableParams: JsonElement? = null

    @Ignore
    @SerializedName(value = "items")
    var items: List<FeedItem> = ArrayList()
}