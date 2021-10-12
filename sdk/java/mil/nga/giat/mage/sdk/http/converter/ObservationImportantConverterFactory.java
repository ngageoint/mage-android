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
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit converter factory for an observation
 *
 * @author newmanw
 *
 */
public final class ObservationImportantConverterFactory extends Converter.Factory {

    private Gson gson;
    private Event event;

    public static ObservationImportantConverterFactory create(Event event) {
        return new ObservationImportantConverterFactory(ObservationSerializer.getGsonBuilder(), event);
    }

    private ObservationImportantConverterFactory(Gson gson, Event event) {
        this.gson = gson;
        this.event = event;
    }

    @Override
    public Converter<ResponseBody, Observation> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        return new ObservationResponseBodyConverter(event);
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        return GsonConverterFactory.create().requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit);
    }
}
