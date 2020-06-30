package mil.nga.giat.mage.feed.item

import android.content.Context
import com.google.gson.JsonElement
import mil.nga.giat.mage.data.feed.Feed
import mil.nga.giat.mage.utils.DateFormatFactory
import java.util.*

class FeedItemPropertyFactory(context: Context) {
    private val dateFormat = DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), context)

    fun createFeedItemProperty(feed: Feed, property: Pair<String, JsonElement>): FeedItemProperty {
        return if (property.first == feed.itemTemporalProperty) {
            createTimestampProperty(property)
        } else createProperty(property)
    }

    private fun createProperty(property: Pair<String, JsonElement>): FeedItemProperty {
        return FeedItemProperty(property.first, property.second.asString)
    }

    private fun createTimestampProperty(property: Pair<String, JsonElement>): FeedItemProperty {
        return FeedItemProperty(property.first, dateFormat.format(Date(property.second.asJsonPrimitive.asLong)))
    }
}