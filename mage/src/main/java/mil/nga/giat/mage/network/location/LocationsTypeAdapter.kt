import android.util.Log
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import mil.nga.giat.mage.database.model.location.Location
import mil.nga.giat.mage.database.model.location.LocationProperty
import mil.nga.giat.mage.network.geometry.GeometrySerializer
import mil.nga.giat.mage.network.geometry.GeometryTypeAdapter
import mil.nga.giat.mage.network.gson.nextBooleanOrNull
import mil.nga.giat.mage.network.gson.nextNumberOrNull
import mil.nga.giat.mage.network.gson.nextStringOrNull
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory
import java.io.IOException
import java.io.Serializable
import java.text.ParseException

class LocationsTypeAdapter: TypeAdapter<List<Location>>() {
   private val geometryDeserializer = GeometryTypeAdapter()

   override fun read(reader: JsonReader): List<Location> {
      val locations = mutableListOf<Location>()

      if (reader.peek() != JsonToken.BEGIN_ARRAY) {
         reader.skipValue()
         return locations
      }

      reader.beginArray()

      while (reader.hasNext()) {
         locations.add(readLocation(reader))
      }

      reader.endArray()

      return locations
   }

   @Throws(IOException::class)
   private fun readLocation(reader: JsonReader): Location {
      val location = Location()

      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
         reader.skipValue()
         return location
      }

      reader.beginObject()

      var userId: String? = null
      var properties = mutableListOf<LocationProperty>()
      while(reader.hasNext()) {
         when(reader.nextName()) {
            "_id" -> location.remoteId = reader.nextString()
            "type" -> location.type = reader.nextString()
            "geometry" -> location.geometry = geometryDeserializer.read(reader)
            "properties" -> properties = readProperties(reader, location)
            "userId" -> userId = reader.nextString()
            else -> reader.skipValue()
         }
      }

      // don't set the user at this time, only the id.  Set it later.
      properties.add(LocationProperty("userId", userId))
      location.properties = properties
      val propertiesMap = location.propertiesMap

      // timestamp is special pull it out of properties and set it at the top level
      propertiesMap["timestamp"]?.value?.toString()?.let {
         try {
            location.timestamp = ISO8601DateFormatFactory.ISO8601().parse(it)
         } catch (e: ParseException) {
            Log.w(LOG_NAME, "Unable to parse date: " + it + " for location: " + location.remoteId, e)
         }
      }

      reader.endObject()

      return location
   }

   @Throws(IOException::class)
   private fun readProperties(reader: JsonReader, location: Location): MutableList<LocationProperty> {
      val properties = mutableListOf<LocationProperty>()

      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
         reader.skipValue()
         return properties
      }

      reader.beginObject()
      while(reader.hasNext()) {
         val key = reader.nextName()
         if (reader.peek() == JsonToken.BEGIN_OBJECT || reader.peek() == JsonToken.BEGIN_ARRAY) {
            reader.skipValue()
         } else {
            val value: Serializable? = when(reader.peek()) {
               JsonToken.NUMBER -> reader.nextNumberOrNull()
               JsonToken.BOOLEAN -> reader.nextBooleanOrNull()
               else -> reader.nextStringOrNull()
            }

            if (value != null) {
               val property =
                  LocationProperty(
                     key,
                     value
                  )
               property.location = location
               properties.add(property)
            }
         }
      }

      reader.endObject()

      return properties
   }

   override fun write(out: JsonWriter, value: List<Location>) {
      out.beginArray()

      value.forEach { location ->
         out.beginObject()
         out.name("eventId").value(location.event.remoteId.toInt())
         out.name("geometry").jsonValue(GeometrySerializer.getGsonBuilder().toJson(location.geometry))

         out.name("properties").beginObject()
         out.name("timestamp").value(ISO8601DateFormatFactory.ISO8601().format(location.timestamp))
         location.properties.filter { it.value != null }.forEach { property ->
            out.name(property.key)
            when (val propertyValue = property.value) {
               is Double -> out.value(propertyValue)
               is Float -> out.value(propertyValue)
               is Boolean -> out.value(propertyValue)
               else -> out.value(property.value.toString())
            }
         }
         out.endObject()
         out.endObject()
      }

      out.endArray()
   }

   companion object {
      private val LOG_NAME = LocationsTypeAdapter::class.java.name
   }
}