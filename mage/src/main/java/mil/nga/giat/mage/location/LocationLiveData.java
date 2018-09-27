package mil.nga.giat.mage.location;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import mil.nga.giat.mage.R;

public class LocationLiveData extends LiveData<Location> implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String LOG_NAME = LocationLiveData.class.getName();

    private Application application;
    private SharedPreferences preferences;
    private LocationManager locationManager;
    private LiveDataLocationListener locationListener;

    public LocationLiveData(Application application) {
        this.application = application;
    }

    @Override
    protected void onActive() {
        super.onActive();

        preferences = PreferenceManager.getDefaultSharedPreferences(application);
        preferences.registerOnSharedPreferenceChangeListener(this);
        locationManager = (LocationManager) application.getSystemService(Context.LOCATION_SERVICE);

        requestLocationUpdates();

        try {
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (location != null) {
                setValue(location);
            }

        } catch (SecurityException e) {
            Log.e(LOG_NAME, "Could not get users location", e);
        }
    }

    @Override
    protected void onInactive() {
        super.onInactive();

        removeLocationUpdates();
        preferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equalsIgnoreCase(application.getString(R.string.gpsSensitivityKey))) {
            Log.d(LOG_NAME, "GPS sensitivity changed, distance in meters for change: " + getMinimumDistanceChangeForUpdates());

            // bounce location updates so new distance sensitivity takes effect
            removeLocationUpdates();
            requestLocationUpdates();
        }
    }

    private void requestLocationUpdates() {
        Log.v(LOG_NAME, "request location updates");

        locationListener = new LiveDataLocationListener();

        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, getMinimumDistanceChangeForUpdates(), locationListener);
        } catch (java.lang.SecurityException ex) {
            Log.i(LOG_NAME, "Error requesting location updates", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(LOG_NAME, "LocationManager.NETWORK_PROVIDER does not exist, " + ex.getMessage());
        }

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, getMinimumDistanceChangeForUpdates(), locationListener);
        } catch (java.lang.SecurityException ex) {
            Log.i(LOG_NAME, "Error requesting location updates", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(LOG_NAME, "LocationManager.GPS_PROVIDER does not exist, " + ex.getMessage());
        }
    }

    private void removeLocationUpdates() {
        Log.v(LOG_NAME, "Removing location updates.");

        if (locationListener != null) {
            locationManager.removeUpdates(locationListener);
            locationListener = null;
        }
    }

    private long getMinimumDistanceChangeForUpdates() {
        return preferences.getInt(application.getString(R.string.gpsSensitivityKey), application.getResources().getInteger(R.integer.gpsSensitivityDefaultValue));
    }

    private class LiveDataLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(final Location location) {
            setValue(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }
}
