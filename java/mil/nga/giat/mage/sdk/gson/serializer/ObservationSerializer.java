package mil.nga.giat.mage.sdk.gson.serializer;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationForm;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.utils.GeometryUtility;
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory;
import mil.nga.wkb.geom.Geometry;


public class ObservationSerializer implements JsonSerializer<Observation> {
	private static final String LOG_NAME = ObservationSerializer.class.getName();

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
	 * @param key
	 *            Property key to add.
	 * @param value
	 *            Property value to add.
	 * @param json
	 *            Object to conditionally add to.
	 * @return A reference to json object.
	 */
	private void conditionalAdd(String key, Object value, final JsonObject json) {
		if (value != null) {
			if (value instanceof Number) {
				json.add(key, new JsonPrimitive((Number) value));
			} else if (value instanceof Boolean) {
				json.add(key, new JsonPrimitive((Boolean) value));
			} else if (value instanceof Date) {
				json.add(key, new JsonPrimitive(iso8601Format.format((Date) value)));
			} else if (value instanceof ArrayList) {
				JsonParser jsonParser = new JsonParser();
				JsonArray choicesArray = jsonParser.parse(new Gson().toJson(value)).getAsJsonArray();
				json.add(key, choicesArray);
			} else if (value instanceof byte[]) {
				byte[] bytes = (byte[]) value;
				try {
					Geometry geometry = GeometryUtility.toGeometry(bytes);
					json.add(key, new JsonParser().parse(GeometrySerializer.getGsonBuilder().toJson(geometry)));
				} catch (Exception e) {
					Log.w(LOG_NAME, "Error converting byte array to geometry", e);
				}
			} else {
				json.add(key, new JsonPrimitive(value.toString()));
			}
		}
	}
}
