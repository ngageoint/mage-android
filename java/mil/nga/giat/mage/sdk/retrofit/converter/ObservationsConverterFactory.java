package mil.nga.giat.mage.sdk.retrofit.converter;

import com.squareup.okhttp.ResponseBody;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import retrofit.Converter;

/**
 * Retrofit converter factory for locations
 *
 * @author newmanw
 *
 */
public final class ObservationsConverterFactory extends Converter.Factory {

    private Event event;

    public static ObservationsConverterFactory create(Event event) {
        return new ObservationsConverterFactory(event);
    }

    private ObservationsConverterFactory(Event event) {
        this.event = event;
    }

    @Override
    public Converter<ResponseBody, Collection<Observation>> fromResponseBody(Type type, Annotation[] annotations) {
        return new ObservationsResponseBodyConverter(event);
    }
}
