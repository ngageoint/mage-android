package mil.nga.giat.mage.sdk.http.converter;

import java.io.IOException;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.jackson.deserializer.ObservationDeserializer;
import okhttp3.ResponseBody;
import retrofit2.Converter;

/**
 * Retrofit observation response body converter
 *
 * Handles Jackson deserialization for an observation
 *
 * @author newmanw
 *
 */
public class ObservationResponseBodyConverter implements Converter<ResponseBody, Observation> {

    private final Event event;

    public ObservationResponseBodyConverter(Event event) {
        this.event = event;
    }

    @Override
    public Observation convert(ResponseBody body) throws IOException {
        return new ObservationDeserializer(event).parseObservation(body.byteStream());
    }

}
