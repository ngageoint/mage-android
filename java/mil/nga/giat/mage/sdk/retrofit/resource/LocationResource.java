package mil.nga.giat.mage.sdk.retrofit.resource;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.retrofit.HttpClient;
import mil.nga.giat.mage.sdk.retrofit.converter.LocationConverterFactory;
import retrofit.Call;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

/***
 * RESTful communication for locations
 *
 * @author newmanw
 */

public class LocationResource {

    public interface LocationService {
        @GET("/api/events/{eventId}/locations/users")
        Call<List<Location>> getLocations(@Path("eventId") String eventId);

        @POST("/api/events/{eventId}/locations")
        Call<List<Location>> createLocations(@Path("eventId") String eventId, @Body List<Location> locations);
    }

    private static final String LOG_NAME = LocationResource.class.getName();

    private Context context;

    public LocationResource(Context context) {
        this.context = context;
    }

    public Collection<Location> getLocations(Event event) {
        Collection<Location> locations = new ArrayList<Location>();

        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(LocationConverterFactory.create(event, true))
                .client(HttpClient.httpClient(context))
                .build();

        try {
            LocationService service = retrofit.create(LocationService.class);
            Call<List<Location>> call = service.getLocations(event.getRemoteId());
            Response<List<Location>> response = call.execute();

            if (response.isSuccess()) {
                locations = response.body();
            } else {
                Log.e(LOG_NAME, "Bad request.");
                if (response.errorBody() != null) {
                    Log.e(LOG_NAME, response.errorBody().string());
                }
            }
        } catch (Exception e) {
            Log.e(LOG_NAME, "There was a failure while performing an Location Fetch operation.", e);
        }

        return locations;
    }

    /**
     * All these locations provided should be from the provided event.
     *
     * @param locations locations to post to the server
     * @param event event in which to post locations for
     * @return create status
     */
    public boolean createLocations(Event event, List<Location> locations) {
        LocationHelper locationHelper = LocationHelper.getInstance(context);

        try {
            String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(LocationConverterFactory.create(event, false))
                    .client(HttpClient.httpClient(context))
                    .build();

            LocationService service = retrofit.create(LocationService.class);
            Call<List<Location>> call = service.createLocations(event.getRemoteId(), locations);
            Response<List<Location>> response = call.execute();

            if (response.isSuccess()) {
                // locations that are posted are only from the current user
                User user = UserHelper.getInstance(context).readCurrentUser();

                // it is imperative that the order of the returnedLocations match the order that was posted!!!
                // if the order changes from the server, all of this will break!
                List<Location> returnedLocations = response.body();
                for (int i = 0; i < returnedLocations.size(); i++) {
                    Location returnedLocation = returnedLocations.get(i);
                    returnedLocation.setId(locations.get(i).getId());
                    returnedLocation.setUser(user);
                    locationHelper.update(returnedLocation);
                }

                return true;
            } else {
                Log.e(LOG_NAME, "Bad request.");
                if (response.errorBody() != null) {
                    Log.e(LOG_NAME, response.errorBody().string());
                }
            }

        } catch (Exception e) {
            Log.e(LOG_NAME, "Failure posting location.", e);
        }

        return false;
    }

}