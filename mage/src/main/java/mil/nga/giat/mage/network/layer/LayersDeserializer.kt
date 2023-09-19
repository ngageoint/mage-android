package mil.nga.giat.mage.network.layer

import com.google.gson.*
import mil.nga.giat.mage.database.model.layer.Layer
import java.lang.reflect.Type

class LayersDeserializer : JsonDeserializer<List<Layer>> {
   private val layerDeserializer: Gson = LayerDeserializer.getGsonBuilder()

   @Throws(JsonParseException::class)
   override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): List<Layer> {
      val layers = mutableListOf<Layer>()
      for (element in json.asJsonArray) {
         layers.add(layerDeserializer.fromJson(element.asJsonObject, Layer::class.java))
      }

      return layers
   }
}