package mil.nga.giat.mage.sdk.gson.deserializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

import mil.nga.giat.mage.sdk.datastore.user.Event;

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
        JsonObject event = json.getAsJsonObject();

        String remoteId = event.get("id").getAsString();

        String name = event.get("name").getAsString();

        String description = "";
        JsonElement descriptionElement = event.get("description");
        if (descriptionElement != null && !descriptionElement.isJsonNull()) {
            description = descriptionElement.getAsString();
        }

        String form = event.get("forms").toString();

        String acl = event.get("acl").toString();

        return new Event(remoteId, name, description, form, acl);
    }
}
