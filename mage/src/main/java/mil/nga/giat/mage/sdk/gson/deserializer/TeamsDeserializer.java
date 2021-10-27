package mil.nga.giat.mage.sdk.gson.deserializer;

import android.content.Context;

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
import mil.nga.giat.mage.sdk.jackson.deserializer.UserDeserializer;

/**
 * JSON to {@link Team}
 */
public class TeamsDeserializer implements JsonDeserializer<Map<Team, Collection<User>>> {

    private final UserHelper userHelper;
    private final UserDeserializer userDeserializer;
    private final Gson teamDeserializer;

    public TeamsDeserializer(Context context) {
        userHelper = UserHelper.getInstance(context);
        userDeserializer = new UserDeserializer(context);
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

            try {
                User user = userDeserializer.parseUser(jsonUser.toString());
                User existingUser = userHelper.read(user.getRemoteId());
                if (existingUser != null) {
                    user.setId(existingUser.getId());
                }
                users.add(user);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return users;
    }
}
