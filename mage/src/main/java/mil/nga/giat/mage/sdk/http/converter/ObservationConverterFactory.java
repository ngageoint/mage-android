package mil.nga.giat.mage.sdk.http.converter;

import com.google.gson.Gson;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.gson.serializer.ObservationSerializer;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Retrofit converter factory for an observation
 *
 * @author newmanw
 *
 */
public final class ObservationConverterFactory extends Converter.Factory {

    private final Gson gson;
    private final Event event;

    public static ObservationConverterFactory create(Event event) {
        return new ObservationConverterFactory(ObservationSerializer.getGsonBuilder(), event);
    }

    private ObservationConverterFactory(Gson gson, Event event) {
        this.gson = gson;
        this.event = event;
    }

    @Override
    public Converter<ResponseBody, Observation> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        return new ObservationResponseBodyConverter(event);
    }

    @Override
    public Converter<Observation, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        return new ObservationRequestBodyConverter(gson, type);
    }
}
