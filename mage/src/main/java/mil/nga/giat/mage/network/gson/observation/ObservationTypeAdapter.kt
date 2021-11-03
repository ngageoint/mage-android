package mil.nga.giat.mage.network.gson.observation

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import mil.nga.giat.mage.sdk.datastore.observation.Observation

class ObservationTypeAdapter(val context: Context): TypeAdapter<Observation>() {
   private val observationSerializer = GsonBuilder()
      .registerTypeAdapter(Observation::class.java, ObservationSerializer())
      .create()

   private val observationDeserializer = ObservationDeserializer(context)

   override fun write(writer: JsonWriter, observation: Observation) {
      writer.jsonValue(observationSerializer.toJson(observation))
   }

   override fun read(reader: JsonReader): Observation {
      return observationDeserializer.read(reader)
   }
}