package mil.nga.giat.mage.location;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.location.LocationProperty;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.LocationException;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.wkb.geom.Point;

public class LocationSaveTask extends AsyncTask<Location, Void, Location> {
    private static final String LOG_NAME = LocationSaveTask.class.getName();

    public interface LocationDatabaseListener {
        void onSaveComplete(Location location);
    }

    private Context context;
    private LocationDatabaseListener listener;
    private Intent batteryStatus;

    LocationSaveTask(Context context, LocationDatabaseListener listener) {
        this.context = context;
        this.listener = listener;
        batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    protected Location doInBackground(Location... locations) {
        Location location = locations[0];
        saveLocation(location);
        return location;
    }

    private void saveLocation(Location location) {
        Log.v(LOG_NAME, "Saving MAGE location to database.");

        if (location != null && location.getTime() > 0) {
            Collection<LocationProperty> locationProperties = new ArrayList<>();

            LocationHelper locationHelper = LocationHelper.getInstance(context);

            // build properties
            locationProperties.add(new LocationProperty("accuracy", location.getAccuracy()));
            locationProperties.add(new LocationProperty("bearing", location.getBearing()));
            locationProperties.add(new LocationProperty("speed", location.getSpeed()));
            locationProperties.add(new LocationProperty("provider", location.getProvider()));
            locationProperties.add(new LocationProperty("altitude", location.getAltitude()));

            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            if (level != -1) {
                locationProperties.add(new LocationProperty("battery_level", level));
            }

            User currentUser = null;
            try {
                currentUser = UserHelper.getInstance(context).readCurrentUser();
            } catch (UserException e) {
                Log.e(LOG_NAME, "Could not get current User!");
            }

            // build location
            mil.nga.giat.mage.sdk.datastore.location.Location loc = new mil.nga.giat.mage.sdk.datastore.location.Location(
                    "Feature",
                    currentUser,
                    locationProperties,
                    new Point(location.getLongitude(), location.getLatitude()),
                    new Date(location.getTime()),
                    currentUser.getCurrentEvent());

            // save the location
            try {
                locationHelper.create(loc);
            } catch (LocationException le) {
                Log.e(LOG_NAME, "Unable to save current location locally!", le);
            }
        }
    }


    @Override
    protected void onPostExecute(Location location) {
        listener.onSaveComplete(location);
    }
}
