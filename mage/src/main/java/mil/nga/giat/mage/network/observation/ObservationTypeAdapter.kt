package mil.nga.giat.mage.network.observation

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import mil.nga.giat.mage.database.model.observation.Observation

class ObservationTypeAdapter: TypeAdapter<Observation>() {
   private val observationSerializer = GsonBuilder()
      .registerTypeAdapter(Observation::class.java, ObservationSerializer())
      .create()

   private val observationDeserializer = ObservationDeserializer()

   override fun write(writer: JsonWriter, observation: Observation) {
      writer.jsonValue(observationSerializer.toJson(observation))
   }

   override fun read(reader: JsonReader): Observation {
      return observationDeserializer.read(reader)
   }
}