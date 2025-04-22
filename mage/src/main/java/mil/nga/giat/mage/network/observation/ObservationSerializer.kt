package mil.nga.giat.mage.network.observation

import android.util.Log
import com.google.gson.*
import com.mapbox.geojson.GeometryAdapterFactory
import com.mapbox.geojson.gson.GeoJsonAdapterFactory
import mil.nga.giat.mage.database.model.observation.Observation
import mil.nga.giat.mage.network.geojson.GeometryTypeAdapterFactory
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory
import mil.nga.giat.mage.sdk.utils.toGeometry
import java.lang.reflect.Type
import java.util.*

class ObservationSerializer : JsonSerializer<Observation> {
   val gson: Gson = GsonBuilder()
      .registerTypeAdapterFactory(GeometryTypeAdapterFactory())
      .registerTypeAdapterFactory(GeoJsonAdapterFactory.create())
      .registerTypeAdapterFactory(GeometryAdapterFactory.create())
      .create()


   override fun serialize(observation: Observation, type: Type, context: JsonSerializationContext): JsonElement {
      val event = observation.event

      val feature = JsonObject()
      feature.addProperty("id", observation.remoteId)
      feature.addProperty("eventId", observation.event.remoteId.toInt())
      feature.addProperty("type", "Feature")
      feature.add("geometry", gson.toJsonTree(observation.geometry))

      val properties = JsonObject()
      properties.addProperty("timestamp", ISO8601DateFormatFactory.ISO8601().format(observation.timestamp))
      if (observation.accuracy != null) {
         properties.addProperty("accuracy", observation.accuracy)
      }
      if (observation.provider != null) {
         properties.addProperty("provider", observation.provider)
      }
      if (observation.locationDelta != null) {
         properties.addProperty("delta", observation.locationDelta)
      }

      // serialize the observation's forms
      val forms = JsonArray()
      for (form in observation.forms) {
         val formDefinition = event.forms.find { it.formId ==  form.formId }?.json?.let {
            JsonParser.parseString(it).asJsonObject
         }

         val fields = formDefinition?.get("fields")?.asJsonArray ?: JsonArray()
         val jsonForm = JsonObject()
         jsonForm.addProperty("id", form.remoteId)
         jsonForm.addProperty("formId", form.formId)

         for (property in form.properties) {
            val fieldDefinition = fields.find { field ->
               property.key == field.asJsonObject["name"].asString
            }?.asJsonObject

            serializeValue(property.value, fieldDefinition)?.let {
               jsonForm.add(property.key, it)
            }
         }

         forms.add(jsonForm)
      }

      properties.add("forms", forms)
      feature.add("properties", properties)

      // serialize the observation's state
      val jsonState = JsonObject()
      jsonState.add("name", JsonPrimitive(observation.state.toString()))
      feature.add("state", jsonState)
      return feature
   }

   /**
    * Serialize observation property value, skip null property values.
    */
   private fun serializeValue(value: Any?, fieldDefinition: JsonObject?): JsonElement? {
      if (value == null) return null

      return when(fieldDefinition?.get("type")?.asString) {
         "attachment" -> {
            val attachments = JsonParser.parseString(Gson().toJson(value)).asJsonArray
            for (attachment in attachments) {
               val id = attachment.asJsonObject.remove("remoteId")
               if (id != null && !id.isJsonNull) {
                  attachment.asJsonObject.add("id", JsonPrimitive(id.asString))
               }
            }
            attachments
         }
         "geometry" -> {
            try {
               val bytes = value as ByteArray
               val geometry = bytes.toGeometry()
               gson.toJsonTree(geometry)
            } catch (e: Exception) {
               Log.w(LOG_NAME, "Error converting byte array to geometry", e)
               null
            }
         }
         "date" -> {
            val timestamp = value as? Date ?: return null
            JsonPrimitive(ISO8601DateFormatFactory.ISO8601().format(timestamp))
         }
         "multiselectdropdown" -> {
            JsonParser.parseString(Gson().toJson(value)).asJsonArray
         }
         "textarea", "textfield", "password", "email", "radio", "dropdown" -> {
            JsonPrimitive(value as? String)
         }
         "numberfield" -> JsonPrimitive(value as? Number)
         "checkbox" ->JsonPrimitive(value as? Boolean)
         else -> null
      }
   }

   companion object {
      private val LOG_NAME = ObservationSerializer::class.java.name
   }
}