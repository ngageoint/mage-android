package mil.nga.giat.mage.location;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.LocationException;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.http.resource.LocationResource;

public class LocationPushTask extends AsyncTask<Void, Void, Boolean> {
    private static final String LOG_NAME = LocationPushTask.class.getName();

    public interface LocationSyncListener {
        void onSyncComplete(Boolean status);
    }

    private static long LOCATION_PUSH_BATCH_SIZE = 100;
    private static final int minNumberOfLocationsToKeep = 40;

    private Context context;
    private LocationSyncListener listener;

    public LocationPushTask(Context context, LocationSyncListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        Boolean status = true;

        LocationResource locationResource = new LocationResource(context);
        LocationHelper locationHelper = LocationHelper.getInstance(context);

        User currentUser = null;
        try {
            currentUser = UserHelper.getInstance(context).readCurrentUser();
        } catch (UserException e) {
            e.printStackTrace();
        }

        List<Location> locations = locationHelper.getCurrentUserLocations(LOCATION_PUSH_BATCH_SIZE, false);
        while (!locations.isEmpty()) {

            // Send locations for the current event
            Event event = locations.get(0).getEvent();

            List<mil.nga.giat.mage.sdk.datastore.location.Location> eventLocations = new ArrayList<>();
            for (mil.nga.giat.mage.sdk.datastore.location.Location l : locations) {
                if (event.equals(l.getEvent())) {
                    eventLocations.add(l);
                }
            }

            // We've sync-ed locations to the server, lets remove the locations we sync'eds from the database
            if (locationResource.createLocations(event, eventLocations)) {
                Log.d(LOG_NAME, "Pushed " + eventLocations.size() + " locations.");

                // Delete location where:
                // the user is current user
                // the remote id is set. (have been sent to server)
                // past the lower n amount!
                try {
                    if (currentUser != null) {
                        Dao<Location, Long> locationDao = DaoStore.getInstance(context).getLocationDao();
                        QueryBuilder<Location, Long> queryBuilder = locationDao.queryBuilder();
                        Where<Location, Long> where = queryBuilder.where().eq("user_id", currentUser.getId());
                        where.and().isNotNull("remote_id").and().eq("event_id", event.getId());
                        queryBuilder.orderBy("timestamp", false);
                        List<mil.nga.giat.mage.sdk.datastore.location.Location> pushedLocations = queryBuilder.query();

                        if (pushedLocations.size() > minNumberOfLocationsToKeep) {
                            Collection<Location> locationsToDelete = pushedLocations.subList(minNumberOfLocationsToKeep, pushedLocations.size());

                            try {
                                LocationHelper.getInstance(context).delete(locationsToDelete);
                            } catch (LocationException e) {
                                Log.e(LOG_NAME, "Could not delete locations.", e);
                            }
                        }
                    }
                } catch (SQLException e) {
                    Log.e(LOG_NAME, "Problem deleting locations.", e);
                }
            } else {
                Log.e(LOG_NAME, "Failed to push locations.");
                status = false;
            }

            locations = locationHelper.getCurrentUserLocations(LOCATION_PUSH_BATCH_SIZE, false);
        }

        return status;
    }

    @Override
    protected void onPostExecute(Boolean status) {
        listener.onSyncComplete(status);
    }
}
