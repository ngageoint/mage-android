package mil.nga.giat.mage.network.location

import LocationsTypeAdapter
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import mil.nga.giat.mage.database.model.location.Location
import mil.nga.giat.mage.network.gson.nextStringOrNull
import java.io.IOException
import java.lang.UnsupportedOperationException

data class UserLocations(
   val userId: String,
   val locations: List<Location>
)

class UserLocationsTypeAdapter: TypeAdapter<List<UserLocations>>() {
   private val locationsTypeAdapter = LocationsTypeAdapter()

   override fun read(reader: JsonReader): List<UserLocations> {
      val locations = mutableListOf<UserLocations>()

      if (reader.peek() != JsonToken.BEGIN_ARRAY) {
         reader.skipValue()
         return locations
      }

      reader.beginArray()

      while (reader.hasNext()) {
         readUserLocations(reader)?.let {
            locations.add(it)
         }
      }

      reader.endArray()

      return locations
   }

   @Throws(IOException::class)
   private fun readUserLocations(reader: JsonReader): UserLocations? {
      var userId: String? = null
      val locations = mutableListOf<Location>()

      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
         reader.skipValue()
         return null
      }

      reader.beginObject()

      while(reader.hasNext()) {
         when(reader.nextName()) {
            "userId" -> userId = reader.nextStringOrNull()
            "locations" -> locations.addAll(locationsTypeAdapter.read(reader))
            else -> reader.skipValue()
         }
      }

      reader.endObject()

      return userId?.let {
         UserLocations(
            userId = it,
            locations = locations
         )
      }
   }

   override fun write(out: JsonWriter?, value: List<UserLocations>?) {
      throw UnsupportedOperationException()
   }
}