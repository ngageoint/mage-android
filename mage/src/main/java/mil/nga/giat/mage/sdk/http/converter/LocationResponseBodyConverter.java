package mil.nga.giat.mage.sdk.http.converter;

import java.io.IOException;
import java.util.List;

import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.jackson.deserializer.LocationDeserializer;
import okhttp3.ResponseBody;
import retrofit2.Converter;

/**
 * Retrofit location response body converter
 *
 * Handles Jackson deserialization for locations
 *
 * @author newmanw
 *
 */
public class LocationResponseBodyConverter implements Converter<ResponseBody, List<Location>> {

    private final Event event;
    private final boolean groupByUser;

    public LocationResponseBodyConverter(Event event, boolean groupByUser) {
        this.event = event;
        this.groupByUser = groupByUser;
    }

    @Override
    public List<Location> convert(ResponseBody body) throws IOException {
        LocationDeserializer deserializer = new LocationDeserializer(event);
        return groupByUser ?
                deserializer.parseUserLocations(body.byteStream()) :
                deserializer.parseLocations(body.byteStream());
    }
}
