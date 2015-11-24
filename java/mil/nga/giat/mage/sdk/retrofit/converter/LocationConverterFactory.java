package mil.nga.giat.mage.sdk.retrofit.converter;

import com.google.gson.Gson;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.gson.serializer.LocationSerializer;
import retrofit.Converter;

/**
 * Retrofit converter factory for locations
 *
 * @author newmanw
 *
 */
public final class LocationConverterFactory extends Converter.Factory {

    private final Gson gson;
    private Event event;

    public static LocationConverterFactory create(Event event) {
        return new LocationConverterFactory(LocationSerializer.getGsonBuilder(), event);
    }

    private LocationConverterFactory(Gson gson, Event event) {
        this.gson = gson;
        this.event = event;
    }

    @Override
    public Converter<ResponseBody, List<Location>> fromResponseBody(Type type, Annotation[] annotations) {
        return new LocationResponseBodyConverter(event);
    }

    @Override
    public Converter<List<Location>, RequestBody> toRequestBody(Type type, Annotation[] annotations) {
        return new LocationRequestBodyConverter(gson, type);
    }
}
