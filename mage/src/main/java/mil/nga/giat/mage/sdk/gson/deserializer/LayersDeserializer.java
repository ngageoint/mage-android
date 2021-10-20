package mil.nga.giat.mage.sdk.gson.deserializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.user.Team;

/**
 * JSON to {@link Layer}
 */
public class LayersDeserializer implements JsonDeserializer<Collection<Layer>> {

    private final Gson layerDeserializer;

    public LayersDeserializer() {
        layerDeserializer = LayerDeserializer.getGsonBuilder();
    }

    /**
     * Convenience method for returning a Gson object with a registered GSon
     * TypeAdaptor i.e. custom deserializer.
     *
     * @return A Gson object that can be used to convert Json into a {@link Team}.
     */
    public static Gson getGsonBuilder() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(new TypeToken<Collection<Layer>>(){}.getType(), new LayersDeserializer());
        return gsonBuilder.create();
    }

    @Override
    public Collection<Layer> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Collection<Layer> layers = new ArrayList<>();

        for (JsonElement element : json.getAsJsonArray()) {
            JsonObject jsonTeam = element.getAsJsonObject();
            Layer layer = layerDeserializer.fromJson(jsonTeam, Layer.class);
            layers.add(layer);
        }

        return layers;
    }

}
