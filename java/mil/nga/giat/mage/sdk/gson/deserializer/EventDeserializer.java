package mil.nga.giat.mage.sdk.gson.deserializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import mil.nga.giat.mage.sdk.datastore.user.Permission;
import mil.nga.giat.mage.sdk.datastore.user.Permissions;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * JSON to {@link Event}
 *
 * @author wiedemanns
 *
 */
public class EventDeserializer implements JsonDeserializer<Event> {

    private static final String LOG_NAME = EventDeserializer.class.getName();

    /**
     * Convenience method for returning a Gson object with a registered GSon
     * TypeAdaptor i.e. custom deserializer.
     *
     * @return A Gson object that can be used to convert Json into a {@link Event}.
     */
    public static Gson getGsonBuilder() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Event.class, new EventDeserializer());
        return gsonBuilder.create();
    }

    @Override
    public Event deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        JsonObject feature = json.getAsJsonObject();

        String remoteId = feature.get("id").getAsString();
        String name = feature.get("name").getAsString();
        String description = "";
        if(feature.has("description")) {
            description = feature.get("description").getAsString();
        }
        String form = feature.get("form").getAsString();

        Event event = null; //new Event(remoteId, name, description);
        return event;
    }
}
