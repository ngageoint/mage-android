package mil.nga.giat.mage.database.model.feed

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.google.gson.annotations.SerializedName

class IconStyle {
    @SerializedName("id")
    @ColumnInfo(name = "id")
    var id: String? = null
}

class MapStyle {
    @SerializedName("icon")
    @Embedded(prefix = "icon_style_")
    var iconStyle: IconStyle? = null

    @SerializedName("stroke")
    @ColumnInfo(name = "stroke")
    var stroke: String? = null
        get() {
            return if (field == null) "#000000" else field
        }

    @SerializedName("strokeOpacity")
    @ColumnInfo(name = "stroke_opacity")
    var strokeOpacity: Double? = null
        get() {
            return if (field == null) 1.0 else field
        }

    @SerializedName("strokeWidth")
    @ColumnInfo(name = "stroke_width")
    var strokeWidth: Int? = null
        get() {
            return if (field == null) 1 else field
        }

    @SerializedName("fill")
    @ColumnInfo(name = "fill")
    var fill: String? = null
        get() {
            return if (field == null) "#000000" else field
        }

    @SerializedName(value = "fillOpacity")
    @ColumnInfo(name = "fill_opacity")
    var fillOpacity: Double? = null
        get() {
            return if (field == null) .2 else field
        }
}