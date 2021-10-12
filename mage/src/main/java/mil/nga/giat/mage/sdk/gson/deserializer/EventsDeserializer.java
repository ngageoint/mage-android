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

import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.Team;
import mil.nga.giat.mage.sdk.datastore.user.TeamHelper;
import mil.nga.giat.mage.sdk.exceptions.TeamException;

/**
 * JSON to {@link Event}
 *
 * @author newmanw
 *
 */
public class EventsDeserializer implements JsonDeserializer<Map<Event, Collection<Team>>> {

    private static final String LOG_NAME = EventsDeserializer.class.getName();

    private TeamHelper teamHelper;
    private Gson teamDeserializer;
    private Gson eventDeserializer;

    public EventsDeserializer(Context context) {
        teamHelper = TeamHelper.getInstance(context);
        teamDeserializer = TeamDeserializer.getGsonBuilder();
        eventDeserializer = EventDeserializer.getGsonBuilder();
    }

    /**
     * Convenience method for returning a Gson object with a registered GSon
     * TypeAdaptor i.e. custom deserializer.
     *
     * @return A Gson object that can be used to convert Json into a {@link Event}.
     */
    public static Gson getGsonBuilder(Context context) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(new TypeToken<Map<Event, Collection<Team>>>(){}.getType(), new EventsDeserializer(context));
        return gsonBuilder.create();
    }

    @Override
    public Map<Event, Collection<Team>> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Map<Event, Collection<Team>> events = new HashMap<>();

        for (JsonElement element : json.getAsJsonArray()) {
            JsonObject jsonEvent = element.getAsJsonObject();
            Event event = eventDeserializer.fromJson(jsonEvent, Event.class);
            Collection<Team> teams = deserializeTeams(jsonEvent.getAsJsonArray("teams"));
            events.put(event, teams);
        }

        return events;
    }

    private Collection<Team> deserializeTeams(JsonArray jsonTeams) {
        Collection<Team> teams = new ArrayList<>();
        for (JsonElement element : jsonTeams) {
            JsonObject jsonTeam = element.getAsJsonObject();

            Team team = null;
            try {
                team = teamHelper.read(jsonTeam.get("id").getAsString());
            } catch (TeamException e) {
                Log.e(LOG_NAME, "Error reading user from database", e);
            }

            if (team == null) {
                team = teamDeserializer.fromJson(jsonTeam.toString(), Team.class);
            }

            if (team != null) {
                teams.add(team);
            }
        }

        return teams;
    }
}
