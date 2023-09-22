package mil.nga.giat.mage.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.JsonElement

class JsonTypeConverter {

    @TypeConverter
    fun fromString(value: String?): JsonElement? {
        return value?.let {
            Gson().fromJson(value, JsonElement::class.java)
        }
    }

    @TypeConverter
    fun fromFeature(json: JsonElement?): String? {
        var value: String? = null

        json?.let {
            value = Gson().toJson(json)
        }

        return value
    }

}