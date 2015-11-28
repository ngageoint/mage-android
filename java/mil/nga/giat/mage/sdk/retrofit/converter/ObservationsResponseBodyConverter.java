package mil.nga.giat.mage.sdk.retrofit.converter;

import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.util.Collection;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.jackson.deserializer.ObservationDeserializer;
import retrofit.Converter;

/**
 * Retrofit location response body converter
 *
 * Handles Jackson deserialization for observations
 *
 * @author newmanw
 *
 */
public class ObservationsResponseBodyConverter implements Converter<ResponseBody, Collection<Observation>> {

    private Event event;

    public ObservationsResponseBodyConverter(Event event) {
        this.event = event;
    }

    @Override
    public Collection<Observation> convert(ResponseBody body) throws IOException {
        return new ObservationDeserializer(event).parseObservations(body.byteStream());
    }

}
