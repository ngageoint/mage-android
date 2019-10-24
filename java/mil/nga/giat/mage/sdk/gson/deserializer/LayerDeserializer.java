package mil.nga.giat.mage.sdk.gson.deserializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.user.Event;

/**
 * JSON to {@link Layer}
 * 
 * @author wiedemanns
 * 
 */
public class LayerDeserializer implements JsonDeserializer<Layer> {

	private Event event = null;

	public LayerDeserializer(Event event) {
		this.event = event;
	}

	/**
	 * Convenience method for returning a Gson object with a registered GSon
	 * TypeAdaptor i.e. custom deserializer.
	 * 
	 * @return A Gson object that can be used to convert Json into a {@link Layer}.
	 */
	public static Gson getGsonBuilder(Event event) {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Layer.class, new LayerDeserializer(event));
		return gsonBuilder.create();
	}

	@Override
	public Layer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		Layer layer = new Layer();

		JsonObject feature = json.getAsJsonObject();

		layer.setEvent(event);
		layer.setRemoteId(feature.get("id").getAsString());
		layer.setType(feature.get("type").getAsString());
		layer.setName(feature.get("name").getAsString());
		if(feature.get("url") != null) {
			layer.setUrl(feature.get("url").getAsString());
		}
		if(feature.get("format") != null){
			layer.setFormat(feature.get("format").getAsString());
		}

		JsonObject file = feature.getAsJsonObject("file");
		if (file != null) {
			if (file.has("name")) {
				layer.setFileName(file.get("name").getAsString());
			}

			if (file.has("size")) {
				layer.setFileSize(file.get("size").getAsLong());
			}
		}

		return layer;
	}
}
