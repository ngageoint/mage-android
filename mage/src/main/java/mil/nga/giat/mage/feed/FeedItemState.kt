package mil.nga.giat.mage.feed

import android.content.Context
import com.google.gson.JsonElement
import mil.nga.giat.mage.data.feed.ItemWithFeed
import mil.nga.giat.mage.network.Server
import mil.nga.giat.mage.utils.DateFormatFactory
import mil.nga.sf.Geometry
import java.text.DateFormat
import java.util.*

data class FeedItemState(
   val id: String,
   val feedId: String,
   val title: String?,
   val date: String?,
   val primary: String?,
   val secondary: String?,
   val geometry: Geometry?,
   val iconUrl: String?,
   val properties: List<Pair<String, String>>
) {
   companion object {
      fun fromItem(itemWithFeed: ItemWithFeed, context: Context): FeedItemState {
         val dateFormat = DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), context)

         val feed = itemWithFeed.feed
         val item = itemWithFeed.item

         val propertiesSchema = feed.itemPropertiesSchema?.asJsonObject?.get("properties")
         val properties: List<Pair<String, String>> = if (item.properties?.isJsonNull == false) {
            item.properties.asJsonObject.entrySet().map { property ->
               createFeedItemProperty(propertiesSchema, property.toPair(), dateFormat)
            }
         } else emptyList()

         val date = if (feed.itemTemporalProperty != null) {
            val timestamp = item.properties?.asJsonObject?.get(feed.itemTemporalProperty)?.asLong
            if (timestamp != null) dateFormat.format(timestamp) else ""
         } else null

         val primary = if (feed.itemPrimaryProperty != null) {
            item.properties?.asJsonObject?.get(feed.itemPrimaryProperty)?.asString ?: ""
         } else null

         val secondary = if (feed.itemSecondaryProperty != null)  {
            item.properties?.asJsonObject?.get(feed.itemSecondaryProperty)?.asString ?: ""
         } else null

         val iconUrl = feed.mapStyle?.iconStyle?.id?.let { id ->
            "${Server(context).baseUrl}/api/icons/${id}/content"
         }

         return FeedItemState(
            id = item.id,
            feedId = feed.id,
            title = feed.title,
            date = date,
            primary = primary,
            secondary = secondary,
            geometry = item.geometry,
            iconUrl = iconUrl,
            properties = properties
         )
      }

      private fun createFeedItemProperty(propertiesSchema: JsonElement?, property: Pair<String, JsonElement>, dateFormat: DateFormat): Pair<String, String> {
         val propertySchema = propertiesSchema?.asJsonObject?.get(property.first)?.asJsonObject

         val title =  propertySchema?.get("title")?.asString ?: property.first
         val value: String = propertySchema?.get("type")?.asString?.let { type ->
            if (type == "number") {
               val format = propertySchema.get("format")?.asString
               if (format == "date") {
                  val timestamp = property.second.asNumber.toLong()
                  dateFormat.format(Date(timestamp))
               } else {
                  property.second.asString
               }
            } else {
               property.second.asString
            }
         } ?: property.second.asString

         return Pair(title, value)
      }
   }
}