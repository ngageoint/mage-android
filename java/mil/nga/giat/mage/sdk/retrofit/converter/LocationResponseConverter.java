package mil.nga.giat.mage.sdk.retrofit.converter;

import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.util.List;

import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.jackson.deserializer.LocationDeserializer;
import retrofit.Converter;

/**
 * Retrofit location response body converter
 *
 * Handles Jackson deserialization for locations
 *
 * @author newmanw
 *
 */
public class LocationResponseConverter implements Converter<ResponseBody, List<Location>> {

    private Event event;

    public LocationResponseConverter(Event event) {
        this.event = event;
    }

    @Override
    public List<Location> convert(ResponseBody body) throws IOException {
        return new LocationDeserializer(event).parseUserLocations(body.byteStream());
    }

}
