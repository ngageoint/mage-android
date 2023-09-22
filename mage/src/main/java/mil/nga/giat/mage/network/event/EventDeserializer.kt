package mil.nga.giat.mage.network.event

import com.google.gson.*
import mil.nga.giat.mage.network.gson.asJsonObjectOrNull
import mil.nga.giat.mage.network.gson.asStringOrNull
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.database.model.event.Form
import java.lang.reflect.Type

class EventDeserializer : JsonDeserializer<Event> {

   @Throws(JsonParseException::class)
   override fun deserialize(
      json: JsonElement,
      typeOfT: Type,
      context: JsonDeserializationContext
   ): Event {
      val eventJson = json.asJsonObject

      val remoteId = eventJson.get("id").asString
      val name = eventJson.get("name").asString
      val description = eventJson.get("description")?.asJsonPrimitive?.asString
      val acl = eventJson["acl"].toString()
      val event = Event(
         remoteId,
         name,
         description,
         acl
      )
      eventJson.get("style")?.asJsonObjectOrNull()?.let { style ->
         event.style = style.toString()
      }

      val formsJson = eventJson.get("forms")
      if (formsJson?.isJsonArray == true) {
         event.forms = deserializeForms(formsJson.asJsonArray)
      }

      var minObservationForms: Int? = null
      val minObservationFormsElement = eventJson["minObservationForms"]
      if (minObservationFormsElement != null && minObservationFormsElement.isJsonPrimitive) {
         minObservationForms = minObservationFormsElement.asInt
      }
      event.minObservationForms = minObservationForms

      var maxObservationForms: Int? = null
      val maxObservationFormsElement = eventJson["maxObservationForms"]
      if (maxObservationFormsElement != null && maxObservationFormsElement.isJsonPrimitive) {
         maxObservationForms = maxObservationFormsElement.asInt
      }
      event.maxObservationForms = maxObservationForms

      return event
   }

   private fun deserializeForms(formsJson: JsonArray): List<Form> {
      val forms = mutableListOf<Form>()
      for (formJson in formsJson.filter { it.isJsonObject }) {
         forms.add(deserializeForm(formJson.asJsonObject))
      }

      return forms
   }

   private fun deserializeForm(formJson: JsonObject): Form {
      val form = Form()
      form.formId = formJson.get("id").asJsonPrimitive.asLong
      form.json = formJson.toString()

      formJson.get("primaryField")?.asStringOrNull()?.let {
         form.primaryMapField = it
      }

      formJson.get("variantField")?.asStringOrNull()?.let {
         form.secondaryMapField = it
      }

      formJson.get("primaryFeedField")?.asStringOrNull()?.let {
         form.primaryFeedField = it
      }

      formJson.get("secondaryFeedField")?.asStringOrNull()?.let {
         form.secondaryFeedField = it
      }

      formJson.get("style")?.asJsonObjectOrNull()?.let { style ->
         form.style = style.toString()
      }

      return form
   }

   companion object {
      /**
       * Convenience method for returning a Gson object with a registered GSon
       * TypeAdaptor i.e. custom deserializer.
       *
       * @return A Gson object that can be used to convert Json into a [Event].
       */
      @JvmStatic
      val gsonBuilder: Gson
         get() {
            val gsonBuilder = GsonBuilder()
            gsonBuilder.registerTypeAdapter(Event::class.java, EventDeserializer())
            return gsonBuilder.create()
         }
   }
}