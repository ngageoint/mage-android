package mil.nga.giat.mage.sdk.retrofit.resource;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.gson.deserializer.UsersDeserializer;
import mil.nga.giat.mage.sdk.retrofit.HttpClient;
import retrofit.Call;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.GET;

/***
 * RESTful communication for users
 *
 * @author newmanw
 */

public class UserResource {

    public interface UserService {
        @GET("/api/users")
        Call<Map<User, Collection<String>>> getUsers();
    }

    private static final String LOG_NAME = UserResource.class.getName();

    private Context context;

    public UserResource(Context context) {
        this.context = context;
    }

    public Map<User, Collection<String>> getUsers() throws IOException {
        Map<User, Collection<String>> users = new HashMap<>();

        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(UsersDeserializer.getGsonBuilder(context)))
                .client(HttpClient.httpClient(context))
                .build();

        UserService service = retrofit.create(UserService.class);
        Response<Map<User, Collection<String>>> response = service.getUsers().execute();

        if (response.isSuccess()) {
            users = response.body();
        } else {
            Log.e(LOG_NAME, "Bad request.");
            if (response.errorBody() != null) {
                Log.e(LOG_NAME, response.errorBody().toString());
            }
        }

        return users;
    }
}