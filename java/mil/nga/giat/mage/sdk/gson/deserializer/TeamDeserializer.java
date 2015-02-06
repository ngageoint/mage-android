package mil.nga.giat.mage.sdk.gson.deserializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import mil.nga.giat.mage.sdk.datastore.user.Permission;
import mil.nga.giat.mage.sdk.datastore.user.Permissions;
import mil.nga.giat.mage.sdk.datastore.user.Team;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * JSON to {@link Team}
 *
 * @author wiedemanns
 *
 */
public class TeamDeserializer implements JsonDeserializer<Team> {

    private static final String LOG_NAME = TeamDeserializer.class.getName();

    /**
     * Convenience method for returning a Gson object with a registered GSon
     * TypeAdaptor i.e. custom deserializer.
     *
     * @return A Gson object that can be used to convert Json into a {@link Team}.
     */
    public static Gson getGsonBuilder() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Team.class, new TeamDeserializer());
        return gsonBuilder.create();
    }

    @Override
    public Team deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        JsonObject feature = json.getAsJsonObject();

        String remoteId = feature.get("id").getAsString();
        String name = feature.get("name").getAsString();
        String description = "";
        if(feature.has("description")) {
            description = feature.get("description").getAsString();
        }

        Team team = new Team(remoteId, name, description);
        return team;
    }
}
