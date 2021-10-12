package mil.nga.giat.mage.sdk.http.converter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Retrofit converter factory for observations
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
    public Converter<ResponseBody, Collection<Observation>> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        return new ObservationsResponseBodyConverter(event);
    }
}
