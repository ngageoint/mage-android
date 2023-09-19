package mil.nga.giat.mage.network.event;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.database.model.event.Event;

public class EventsDeserializer implements JsonDeserializer<List<Event>> {

    private final Gson eventDeserializer;

    public EventsDeserializer() {
        eventDeserializer = EventDeserializer.getGsonBuilder();
    }

    @Override
    public List<Event> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<Event> events = new ArrayList<>();

        for (JsonElement element : json.getAsJsonArray()) {
            JsonObject jsonEvent = element.getAsJsonObject();
            Event event = eventDeserializer.fromJson(jsonEvent, Event.class);
            events.add(event);
        }

        return events;
    }
}
