package mil.nga.giat.mage.sdk.http.converter;

import com.google.gson.Gson;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.gson.serializer.LocationSerializer;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Retrofit converter factory for locations
 *
 * @author newmanw
 *
 */
public final class LocationConverterFactory extends Converter.Factory {

    private Gson gson;
    private Event event;
    private boolean groupByUser;

    public static LocationConverterFactory create(Event event, boolean groupByUser) {
        return new LocationConverterFactory(LocationSerializer.getGsonBuilder(), event, groupByUser);
    }

    private LocationConverterFactory(Gson gson, Event event, boolean groupByUser) {
        this.gson = gson;
        this.event = event;
        this.groupByUser = groupByUser;
    }


    @Override
    public Converter<ResponseBody, List<Location>> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        return new LocationResponseBodyConverter(event, groupByUser);
    }

    @Override
    public Converter<List<Location>, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        return new LocationRequestBodyConverter(gson, type);
    }

}
