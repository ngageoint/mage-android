package mil.nga.giat.mage.sdk.gson.serializer;

import java.io.Serializable;
import java.lang.reflect.Type;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


public class ObservationSerializer implements JsonSerializer<Observation> {

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
	public JsonElement serialize(Observation pObs, Type pType, JsonSerializationContext pContext) {

		JsonObject feature = new JsonObject();
        feature.add("eventId", new JsonPrimitive(pObs.getEvent().getRemoteId()));
		feature.add("type", new JsonPrimitive("Feature"));
		conditionalAdd("id", pObs.getRemoteId(), feature);
		feature.add("geometry", new JsonParser().parse(GeometrySerializer.getGsonBuilder().toJson(pObs.getObservationGeometry().getGeometry())));

		// serialize the observation's properties.
		JsonObject properties = new JsonObject();
		for (ObservationProperty property : pObs.getProperties()) {

			String key = property.getKey();
			Serializable value = property.getValue();

			conditionalAdd(key, value, properties);
		}
		feature.add("properties", properties);

		// serialize the observation's state
		JsonObject jsonState = new JsonObject();
		jsonState.add("name", new JsonPrimitive(pObs.getState().toString()));
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
			} else {
				pJsonObject.add(property, new JsonPrimitive(toAdd.toString()));
			}
		}
		return pJsonObject;
	}
}
