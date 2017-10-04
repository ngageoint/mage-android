package mil.nga.giat.mage.sdk.gson.serializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.ArrayList;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationForm;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory;


public class ObservationSerializer implements JsonSerializer<Observation> {

	private DateFormat iso8601Format = ISO8601DateFormatFactory.ISO8601();


	public ObservationSerializer() {
		super();
	}

	/**
	 * Convenience method for returning a Gson object with a registered GSon
	 * TypeAdaptor i.e. custom serializer.
	 * 
	 * @return A Gson object that can be used to convert {@link Observation} object
	 * into a JSON string.
	 */
	public static Gson getGsonBuilder() {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Observation.class, new ObservationSerializer());
		return gsonBuilder.create();
	}

	@Override
	public JsonElement serialize(Observation observation, Type type, JsonSerializationContext context) {

		JsonObject feature = new JsonObject();
        feature.add("eventId", new JsonPrimitive(observation.getEvent().getRemoteId()));
		feature.add("type", new JsonPrimitive("Feature"));
		conditionalAdd("id", observation.getRemoteId(), feature);
		feature.add("geometry", new JsonParser().parse(GeometrySerializer.getGsonBuilder().toJson(observation.getGeometry())));

		JsonObject properties = new JsonObject();
		properties.addProperty("timestamp", iso8601Format.format(observation.getTimestamp()));

		if (observation.getAccuracy() != null) {
			properties.addProperty("accuracy", observation.getAccuracy());
		}

		if (observation.getProvider() != null) {
			properties.addProperty("provider", observation.getProvider());
		}

		if (observation.getLocationDelta() != null) {
			properties.addProperty("delta", observation.getLocationDelta());
		}

		// serialize the observation's forms
		JsonArray forms = new JsonArray();
		for (ObservationForm form : observation.getForms()) {
			JsonObject jsonForm = new JsonObject();
			jsonForm.addProperty("formId", form.getFormId());

			for (ObservationProperty property : form.getProperties()) {
				conditionalAdd(property.getKey(), property.getValue(), jsonForm);
			}

			forms.add(jsonForm);
		}

		properties.add("forms", forms);

		feature.add("properties", properties);

		// serialize the observation's state
		JsonObject jsonState = new JsonObject();
		jsonState.add("name", new JsonPrimitive(observation.getState().toString()));
		feature.add("state", jsonState);

		return feature;
	}

	/**
	 * Utility used to ensure we don't add junk to the json string. For now, we skip null property values.
	 * 
	 * @param property
	 *            Property to add.
	 * @param toAdd
	 *            Property value to add.
	 * @param pJsonObject
	 *            Object to conditionally add to.
	 * @return A reference to json object.
	 */
	private JsonObject conditionalAdd(String property, Serializable toAdd, final JsonObject pJsonObject) {
		if (toAdd != null) {
			if (toAdd instanceof Double) {
				pJsonObject.add(property, new JsonPrimitive((Double) toAdd));
			} else if (toAdd instanceof Float) {
				pJsonObject.add(property, new JsonPrimitive((Float) toAdd));
			} else if (toAdd instanceof Boolean) {
				pJsonObject.add(property, new JsonPrimitive((Boolean) toAdd));
			} else if (toAdd instanceof ArrayList){
				JsonParser jsonParser = new JsonParser();
				JsonArray choicesArray = jsonParser.parse(toAdd.toString()).getAsJsonArray();
				pJsonObject.add(property, choicesArray);
			} else {
				pJsonObject.add(property, new JsonPrimitive(toAdd.toString()));
			}
		}
		return pJsonObject;
	}
}
