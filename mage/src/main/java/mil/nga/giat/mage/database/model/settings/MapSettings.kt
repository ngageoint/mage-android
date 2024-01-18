package mil.nga.giat.mage.database.model.settings

import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName

enum class MapSearchType {
   NONE,
   NATIVE,
   NOMINATIM
}

data class MapSettings(
   @SerializedName("mobileSearchType")
   @ColumnInfo(name = "search_type")
   val searchType: MapSearchType,

   @SerializedName("mobileNominatimUrl")
   @ColumnInfo(name = "search_url")
   val searchUrl : String? = null
)