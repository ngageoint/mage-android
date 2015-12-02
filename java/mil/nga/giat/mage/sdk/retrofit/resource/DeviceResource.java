package mil.nga.giat.mage.sdk.retrofit.resource;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.JsonObject;

import java.io.IOException;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.retrofit.HttpClient;
import retrofit.Call;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.Body;
import retrofit.http.GET;

/***
 * RESTful communication for devices
 *
 * @author newmanw
 */

public class DeviceResource {

    public interface DeviceService {

        @GET("/api/devices")
        Call<JsonObject> createDevice(@Body JsonObject device);
    }

    private static final String LOG_NAME = DeviceResource.class.getName();

    private Context context;

    public DeviceResource(Context context) {
        this.context = context;
    }

    public JsonObject createDevice(String username, String uid, String password) throws IOException {
        JsonObject device = null;

        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(HttpClient.httpClient(context))
                .build();

        DeviceService service = retrofit.create(DeviceService.class);

        JsonObject json = new JsonObject();
        json.addProperty("username", username);
        json.addProperty("uid", uid);
        json.addProperty("password", password);

        Response<JsonObject> response = service.createDevice(json).execute();

        if (response.isSuccess()) {
            device = response.body();
        } else {
            Log.e(LOG_NAME, "Bad request.");
            if (response.errorBody() != null) {
                Log.e(LOG_NAME, response.errorBody().string());
            }
        }

        return device;
    }
}