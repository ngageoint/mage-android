package mil.nga.giat.mage.network.gson.observation

import android.content.Context
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import mil.nga.giat.mage.sdk.datastore.observation.Observation

class ObservationsTypeAdapter(val context: Context): TypeAdapter<List<Observation>>() {
   private val observationDeserializer = ObservationDeserializer(context)

   override fun write(out: JsonWriter?, value: List<Observation>?) {
      throw UnsupportedOperationException()
   }

   override fun read(reader: JsonReader): List<Observation> {
      val observations = mutableListOf<Observation>()

      if (reader.peek() != JsonToken.BEGIN_ARRAY) {
         reader.skipValue()
         return observations
      }

      reader.beginArray()

      while (reader.hasNext()) {
         observations.add(observationDeserializer.read(reader))
      }

      reader.endArray()

      return observations
   }
}