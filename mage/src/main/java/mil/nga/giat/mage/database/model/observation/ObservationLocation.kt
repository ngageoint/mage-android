package mil.nga.giat.mage.database.model.observation

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import mil.nga.giat.mage.database.model.feed.FeedItem
import mil.nga.giat.mage.database.model.feed.MapStyle
import mil.nga.sf.Geometry
import androidx.room.TypeConverter


@Entity(tableName = "observation_location")
data class ObservationLocation(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @SerializedName("eventRemoteId")
    @ColumnInfo(name = "event_remote_id")
    val eventRemoteId: String,

    @SerializedName("observationId")
    @ColumnInfo(name = "observation_id")
    val observationId: Long,

    @SerializedName("geometry")
    @ColumnInfo(name = "geometry", typeAffinity = ColumnInfo.BLOB)
    val geometry: Geometry,

    @SerializedName("latitude")
    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @SerializedName("longitude")
    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @SerializedName("maxLatitude")
    @ColumnInfo(name = "max_latitude")
    val maxLatitude: Double,

    @SerializedName("maxLongitude")
    @ColumnInfo(name = "max_longitude")
    val maxLongitude: Double,

    @SerializedName("minLatitude")
    @ColumnInfo(name = "min_latitude")
    val minLatitude: Double,

    @SerializedName("minLongitude")
    @ColumnInfo(name = "min_longitude")
    val minLongitude: Double,
) {

    @SerializedName("accuracy")
    @ColumnInfo(name = "accuracy")
    var accuracy: Float? = null

    @SerializedName("fieldName")
    @ColumnInfo(name = "field_name")
    var fieldName: String? = null

    @SerializedName("formId")
    @ColumnInfo(name = "form_id")
    var formId: Long? = null

    @SerializedName("order")
    @ColumnInfo(name = "order")
    var order: Int? = null

    @SerializedName("primaryFieldText")
    @ColumnInfo(name = "primary_field_text")
    var primaryFieldText: String? = null

    @SerializedName("provider")
    @ColumnInfo(name = "provider")
    var provider: String? = null

    @SerializedName("secondaryFieldText")
    @ColumnInfo(name = "secondary_field_text")
    var secondaryFieldText: String? = null
}

class UriTypeConverter {
    @TypeConverter
    fun fromUri(value: Uri?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toUri(value: String?): Uri? {
        return value?.let { Uri.parse(value) }
    }
}