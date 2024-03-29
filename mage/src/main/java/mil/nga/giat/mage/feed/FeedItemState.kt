package mil.nga.giat.mage.feed

import android.content.Context
import com.google.gson.JsonElement
import mil.nga.giat.mage.database.model.feed.ItemWithFeed
import mil.nga.giat.mage.map.FeedItemId
import mil.nga.giat.mage.network.Server
import mil.nga.giat.mage.network.gson.asLongOrNull
import mil.nga.giat.mage.network.gson.asStringOrNull
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory
import mil.nga.giat.mage.utils.DateFormatFactory
import mil.nga.sf.Geometry
import java.text.DateFormat
import java.text.ParseException
import java.util.*

data class FeedItemState(
   val id: FeedItemId,
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
               createFeedItemProperty(feed.itemTemporalProperty, propertiesSchema, property.toPair(), dateFormat)
            }
         } else emptyList()

         val date = if (feed.itemTemporalProperty != null) {
            val temporalElement = item.properties?.asJsonObject?.get(feed.itemTemporalProperty)
            val timestamp = temporalElement?.asLongOrNull() ?: run {
               temporalElement?.asStringOrNull()?.let { date ->
                  try {
                     ISO8601DateFormatFactory.ISO8601().parse(date)?.time
                  } catch (ignore: ParseException) { null }
               }
            }

            if (timestamp != null) {
               dateFormat.format(Date(timestamp))
            } else ""
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
            FeedItemId(feed.id, item.id),
            title = feed.title,
            date = date,
            primary = primary,
            secondary = secondary,
            geometry = item.geometry,
            iconUrl = iconUrl,
            properties = properties
         )
      }

      private fun createFeedItemProperty(
         temporalProperty: String?,
         propertiesSchema: JsonElement?,
         property: Pair<String, JsonElement>,
         dateFormat: DateFormat
      ): Pair<String, String> {
         val propertySchema = propertiesSchema?.asJsonObject?.get(property.first)?.asJsonObject

         val title = propertySchema?.get("title")?.asString ?: property.first
         val value: String = propertySchema?.get("type")?.asString?.let { type ->
            if (temporalProperty == property.first) {
               val timestamp = property.second.asLongOrNull() ?: run {
                  property.second.asStringOrNull()?.let { date ->
                     try {
                        ISO8601DateFormatFactory.ISO8601().parse(date)?.time
                     } catch (ignore: ParseException) { null }
                  }
               }

               if (timestamp != null) {
                  dateFormat.format(Date(timestamp))
               } else ""
            } else if (type == "number") {
               val format = propertySchema.get("format")?.asString
               if (format == "date") {
                  property.second.asLongOrNull()?.let {
                     dateFormat.format(Date(it))
                  }
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