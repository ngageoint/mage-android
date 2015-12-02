package mil.nga.giat.mage.sdk.retrofit.resource;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.user.Role;
import mil.nga.giat.mage.sdk.gson.deserializer.RolesDeserializer;
import mil.nga.giat.mage.sdk.retrofit.HttpClientManager;
import retrofit.Call;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.GET;

/***
 * RESTful communication for roles
 *
 * @author newmanw
 */

public class RoleResource {

    public interface RoleService {
        @GET("/api/roles")
        Call<Collection<Role>> getRoles();
    }

    private static final String LOG_NAME = RoleResource.class.getName();

    private Context context;

    public RoleResource(Context context) {
        this.context = context;
    }

    public Collection<Role> getRoles() throws IOException {
        Collection<Role> roles = new ArrayList<Role>();

        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(RolesDeserializer.getGsonBuilder()))
                .client(HttpClientManager.getInstance(context).httpClient())
                .build();

        RoleService service = retrofit.create(RoleService.class);
        Call<Collection<Role>> call = service.getRoles();
        Response<Collection<Role>> response = call.execute();

        if (response.isSuccess()) {
            roles = response.body();
        } else {
            Log.e(LOG_NAME, "Bad request.");
            if (response.errorBody() != null) {
                Log.e(LOG_NAME, response.errorBody().string());
            }
        }

        return roles;
    }
}