package mil.nga.giat.mage.sdk.http.resource;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.gson.deserializer.LayerDeserializer;
import mil.nga.giat.mage.sdk.http.HttpClientManager;
import mil.nga.giat.mage.sdk.http.converter.FeatureConverterFactory;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

/***
 * RESTful communication for events
 *
 * @author newmanw
 */

public class LayerResource {

    public interface LayerService {

        @GET("/api/events/{eventId}/layers")
        Call<Collection<Layer>> getLayers(@Path("eventId") String eventId, @Query("type") String type);

        @GET("/api/events/{eventId}/layers/{layerId}/features")
        Call<Collection<StaticFeature>> getFeatures(@Path("eventId") String eventId, @Path("layerId") String layerId);

        @GET
        Call<ResponseBody> getFeatureIcon(@Url String url);

        @GET("/api/layers/{layerId}")
        @Headers("Accept: application/octet-stream")
        @Streaming
        Call<ResponseBody> getGeopackage(@Path("layerId") String layerId);
    }

    private static final String LOG_NAME = LayerResource.class.getName();

    private Context context;

    public LayerResource(Context context) {
        this.context = context;
    }

    public Collection<Layer> getLayers(Event event) throws IOException {
        Collection<Layer> layers = new ArrayList<>();

        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(LayerDeserializer.getGsonBuilder(event)))
                .client(HttpClientManager.getInstance(context).httpClient())
                .build();

        LayerService service = retrofit.create(LayerService.class);
        Response<Collection<Layer>> response = service.getLayers(event.getRemoteId(), "Feature").execute();

        if (response.isSuccessful()) {
            layers = response.body();
        } else {
            Log.e(LOG_NAME, "Bad request.");
            if (response.errorBody() != null) {
                Log.e(LOG_NAME, response.errorBody().string());
            }
        }

        return layers;
    }

    public Collection<StaticFeature> getFeatures(Layer layer) throws IOException {
        Collection<StaticFeature> features = new ArrayList<>();

        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(FeatureConverterFactory.create(layer))
                .client(HttpClientManager.getInstance(context).httpClient())
                .build();

        LayerService service = retrofit.create(LayerService.class);
        Response<Collection<StaticFeature>> response = service.getFeatures(layer.getEvent().getRemoteId(), layer.getRemoteId()).execute();

        if (response.isSuccessful()) {
            features = response.body();
        } else {
            Log.e(LOG_NAME, "Bad request.");
            if (response.errorBody() != null) {
                Log.e(LOG_NAME, response.errorBody().string());
            }
        }

        return features;
    }

    public InputStream getFeatureIcon(String url) throws IOException {
        InputStream inputStream = null;

        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(HttpClientManager.getInstance(context).httpClient())
                .build();

        LayerService service = retrofit.create(LayerService.class);
        Response<ResponseBody> response = service.getFeatureIcon(url).execute();

        if (response.isSuccessful()) {
            inputStream = response.body().byteStream();
        } else {
            Log.e(LOG_NAME, "Bad request.");
            if (response.errorBody() != null) {
                Log.e(LOG_NAME, response.errorBody().string());
            }
        }

        return inputStream;
    }

    public InputStream getGeopackage(String layerId) throws IOException {
        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(HttpClientManager.getInstance(context).httpClient())
                .build();

        LayerService service = retrofit.create(LayerService.class);
        Response<ResponseBody> response = service.getGeopackage(layerId).execute();

        InputStream inputStream = null;
        if (response.isSuccessful()) {
            inputStream = response.body().byteStream();
        } else {
            Log.e(LOG_NAME, "Bad request.");
            if (response.errorBody() != null) {
                Log.e(LOG_NAME, response.errorBody().string());
            }
        }

        return inputStream;
    }
}