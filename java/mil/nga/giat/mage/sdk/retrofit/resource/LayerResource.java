package mil.nga.giat.mage.sdk.retrofit.resource;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.gson.deserializer.LayerDeserializer;
import mil.nga.giat.mage.sdk.retrofit.HttpClientManager;
import mil.nga.giat.mage.sdk.retrofit.converter.FeatureConverterFactory;
import retrofit.Call;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.http.Url;

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

        if (response.isSuccess()) {
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

        if (response.isSuccess()) {
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

        if (response.isSuccess()) {
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