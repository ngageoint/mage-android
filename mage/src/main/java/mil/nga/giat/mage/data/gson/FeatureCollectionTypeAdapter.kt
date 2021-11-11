package mil.nga.giat.mage.data.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import mil.nga.giat.mage.data.feed.FeedItem
import java.lang.UnsupportedOperationException
import java.lang.reflect.Type
import java.util.*

class FeatureCollectionTypeAdapter : TypeAdapter<List<FeedItem>>() {

    val gson: Gson = GsonBuilder()
            .registerTypeAdapterFactory(GeometryTypeAdapterFactory())
            .create()

    override fun read(`in`: JsonReader): List<FeedItem>? {
        var items = emptyList<FeedItem>()
        if (`in`.peek() == JsonToken.NULL) {
            `in`.nextNull()
            return items
        }

        if (`in`.peek() != JsonToken.BEGIN_OBJECT) {
            `in`.skipValue()
            return items
        }

        `in`.beginObject()
        while (`in`.hasNext()) {
            when(`in`.nextName()) {
                "features" -> {
                    val type: Type = object : TypeToken<ArrayList<FeedItem>>() {}.type
                    items = gson.fromJson<ArrayList<FeedItem>>(`in`, type)
                }
                else -> `in`.skipValue()
            }
        }
        `in`.endObject()

        return items
    }

    override fun write(out: JsonWriter?, value: List<FeedItem>?) {
        throw UnsupportedOperationException()
    }

}