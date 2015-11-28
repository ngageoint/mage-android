package mil.nga.giat.mage.sdk.retrofit.resource;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.gson.deserializer.UsersDeserializer;
import mil.nga.giat.mage.sdk.retrofit.HttpClient;
import retrofit.Call;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

/***
 * RESTful communication for users
 *
 * @author newmanw
 */

public class UserResource {

    public interface UserService {
        @GET("/api/users")
        Call<Collection<User>> getUsers();

        @POST("/api/users/{userId}/events{eventId}/recent")
        Call<User> addRecentEvent(@Path("userId") String userId, @Path("eventId") String eventId);
    }

    private static final String LOG_NAME = UserResource.class.getName();

    private Context context;

    public UserResource(Context context) {
        this.context = context;
    }

    public Collection<User> getUsers() throws IOException {
        Collection<User> users = new ArrayList<>();

        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(UsersDeserializer.getGsonBuilder(context)))
                .client(HttpClient.httpClient(context))
                .build();

        UserService service = retrofit.create(UserService.class);
        Response<Collection<User>> response = service.getUsers().execute();

        if (response.isSuccess()) {
            users = response.body();
        } else {
            Log.e(LOG_NAME, "Bad request.");
            if (response.errorBody() != null) {
                Log.e(LOG_NAME, response.errorBody().string());
            }
        }

        return users;
    }

    public User addRecentEvent(User user, Event event) throws IOException {
        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(UsersDeserializer.getGsonBuilder(context)))
                .client(HttpClient.httpClient(context))
                .build();

        UserService service = retrofit.create(UserService.class);
        Response<User> response = service.addRecentEvent(user.getRemoteId(), event.getRemoteId()).execute();

        if (response.isSuccess()) {
            return response.body();
        } else {
            Log.e(LOG_NAME, "Bad request.");
            if (response.errorBody() != null) {
                Log.e(LOG_NAME, response.errorBody().string());
            }

            return null;
        }

    }
}