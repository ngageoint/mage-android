package mil.nga.giat.mage.sdk.gson.deserializer

import com.google.gson.*
import mil.nga.giat.mage.sdk.datastore.layer.Layer
import java.lang.reflect.Type

class LayersDeserializer : JsonDeserializer<Collection<Layer>> {
   private val layerDeserializer: Gson = LayerDeserializer.getGsonBuilder()

   @Throws(JsonParseException::class)
   override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Collection<Layer> {
      val layers = mutableListOf<Layer>()
      for (element in json.asJsonArray) {
         layers.add(layerDeserializer.fromJson(element.asJsonObject, Layer::class.java))
      }

      return layers
   }
}