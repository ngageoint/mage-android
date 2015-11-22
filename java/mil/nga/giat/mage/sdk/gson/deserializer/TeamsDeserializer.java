package mil.nga.giat.mage.sdk.gson.deserializer;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import mil.nga.giat.mage.sdk.datastore.user.Team;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;

/**
 * JSON to {@link Team}
 *
 * @author newmanw
 *
 */
public class TeamsDeserializer implements JsonDeserializer<Map<Team, Collection<User>>> {

    private static final String LOG_NAME = TeamsDeserializer.class.getName();

    private UserHelper userHelper;
    private Gson userDeserializer;
    private Gson teamDeserializer;

    public TeamsDeserializer(Context context) {
        userHelper = UserHelper.getInstance(context);
        userDeserializer = UserDeserializer.getGsonBuilder(context);
        teamDeserializer = TeamDeserializer.getGsonBuilder();
    }

    /**
     * Convenience method for returning a Gson object with a registered GSon
     * TypeAdaptor i.e. custom deserializer.
     *
     * @return A Gson object that can be used to convert Json into a {@link Team}.
     */
    public static Gson getGsonBuilder(Context context) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(new TypeToken<Map<Team, Collection<User>>>(){}.getType(), new TeamsDeserializer(context));
        return gsonBuilder.create();
    }

    @Override
    public Map<Team, Collection<User>> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Map<Team, Collection<User>> teams = new HashMap<>();

        for (JsonElement element : json.getAsJsonArray()) {
            JsonObject jsonTeam = element.getAsJsonObject();
            Team team = teamDeserializer.fromJson(jsonTeam, Team.class);
            Collection<User> users = deserializeUsers(jsonTeam.getAsJsonArray("users"));
            teams.put(team, users);
        }

        return teams;
    }

    private Collection<User> deserializeUsers(JsonArray jsonUsers) {
        Collection<User> users = new ArrayList<>();
        for (JsonElement userElement : jsonUsers) {
            JsonObject jsonUser = userElement.getAsJsonObject();

            User user = null;
            try {
                user = userHelper.read(jsonUser.get("id").getAsString());
            } catch (UserException e) {
                Log.e(LOG_NAME, "Error reading user from database", e);
            }

            if (user == null) {
                user = userDeserializer.fromJson(jsonUser.toString(), User.class);
            }

            if (user != null) {
                users.add(user);
            }
        }

        return users;
    }
}
