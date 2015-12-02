package mil.nga.giat.mage.sdk.http.resource;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.user.Team;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.gson.deserializer.TeamsDeserializer;
import mil.nga.giat.mage.sdk.http.HttpClientManager;
import retrofit.Call;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.GET;

/***
 * RESTful communication for teams
 *
 * @author newmanw
 */

public class TeamResource {

    public interface TeamService {
        @GET("/api/teams")
        Call<Map<Team, Collection<User>>> getTeams();
    }

    private static final String LOG_NAME = TeamResource.class.getName();

    private Context context;

    public TeamResource(Context context) {
        this.context = context;
    }

    public Map<Team, Collection<User>> getTeams() throws IOException {
        Map<Team, Collection<User>> teams = new HashMap<>();

        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(TeamsDeserializer.getGsonBuilder(context)))
                .client(HttpClientManager.getInstance(context).httpClient())
                .build();

        TeamService service = retrofit.create(TeamService.class);
        Response<Map<Team, Collection<User>>> response = service.getTeams().execute();

        if (response.isSuccess()) {
            teams = response.body();
        } else {
            Log.e(LOG_NAME, "Bad request.");
            if (response.errorBody() != null) {
                Log.e(LOG_NAME, response.errorBody().string());
            }
        }

        return teams;
    }
}