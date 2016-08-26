package mil.nga.giat.mage.sdk.http.converter;

import com.google.gson.Gson;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.gson.serializer.ObservationSerializer;
import retrofit.Converter;
import retrofit.GsonConverterFactory;

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
    public Converter<ResponseBody, Observation> fromResponseBody(Type type, Annotation[] annotations) {
        return new ObservationResponseBodyConverter(event);
    }

    @Override
    public Converter<?, RequestBody> toRequestBody(Type type, Annotation[] annotations) {
        return GsonConverterFactory.create().toRequestBody(type, annotations);
    }
}
