package mil.nga.giat.mage.sdk.http.resource;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.JsonObject;

import java.io.IOException;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.http.HttpClientManager;
import retrofit.Call;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.Body;
import retrofit.http.POST;
import retrofit.http.Path;

/***
 * RESTful communication for devices
 *
 * @author newmanw
 */

public class DeviceResource {

    public interface DeviceService {

        @POST("/api/devices")
        Call<JsonObject> createDevice(@Body JsonObject device);

        @POST("/auth/{strategy}/devices")
        Call<JsonObject> createOAuthDevice(@Path("strategy") String strategy, @Body JsonObject device);
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
                .client(HttpClientManager.getInstance(context).httpClient())
                .build();

        DeviceService service = retrofit.create(DeviceService.class);

        JsonObject json = new JsonObject();
        json.addProperty("username", username);
        json.addProperty("uid", uid);
        json.addProperty("password", password);

        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            json.addProperty("appVersion", String.format("%s-%s", packageInfo.versionName, packageInfo.versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_NAME , "Problem retrieving package info.", e);
        }

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

    public JsonObject createOAuthDevice(String strategy, String accessToken, String uid) throws IOException {
        JsonObject device = null;

        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(HttpClientManager.getInstance(context).httpClient())
                .build();

        DeviceService service = retrofit.create(DeviceService.class);

        JsonObject json = new JsonObject();
        json.addProperty("access_token", accessToken);
        json.addProperty("uid", uid);

        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            json.addProperty("appVersion", String.format("%s-%s", packageInfo.versionName, packageInfo.versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_NAME , "Problem retrieving package info.", e);
        }

        Response<JsonObject> response = service.createOAuthDevice(strategy, json).execute();

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