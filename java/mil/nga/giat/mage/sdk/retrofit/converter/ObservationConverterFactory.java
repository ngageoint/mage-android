package mil.nga.giat.mage.sdk.retrofit.converter;

import com.google.gson.Gson;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.gson.serializer.ObservationSerializer;
import retrofit.Converter;

/**
 * Retrofit converter factory for an observation
 *
 * @author newmanw
 *
 */
public final class ObservationConverterFactory extends Converter.Factory {

    private Gson gson;
    private Event event;

    public static ObservationConverterFactory create(Event event) {
        return new ObservationConverterFactory(ObservationSerializer.getGsonBuilder(), event);
    }

    private ObservationConverterFactory(Gson gson, Event event) {
        this.gson = gson;
        this.event = event;
    }

    @Override
    public Converter<ResponseBody, Observation> fromResponseBody(Type type, Annotation[] annotations) {
        return new ObservationResponseBodyConverter(event);
    }

    @Override
    public Converter<Observation, RequestBody> toRequestBody(Type type, Annotation[] annotations) {
        return new ObservationRequestBodyConverter(gson, type);
    }
}
