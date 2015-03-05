package mil.nga.giat.mage.sdk.location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.location.LocationGeometry;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.location.LocationProperty;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.LocationException;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Query the device for the device's location. If userReportingFrequency is set
 * to never, the Service will listen for changes to userReportingFrequency.
 * 
 * @author wiedemanns
 * 
 */
public class LocationService extends Service implements LocationListener, OnSharedPreferenceChangeListener {

	private static final String LOG_NAME = LocationService.class.getName();

	private final Context mContext;
	
	private final UserHelper userHelper;

	protected final LocationManager locationManager;

	private Intent batteryStatus;

	protected boolean pollingRunning = false;
	
	protected Collection<LocationListener> locationListeners = new CopyOnWriteArrayList<LocationListener>();
	
	protected synchronized boolean isPolling() {
		return pollingRunning;
	}
	
	// False means don't re-read gps settings.  True means re-read gps settings.  Gets triggered from preference change
	protected final AtomicBoolean preferenceSemaphore = new AtomicBoolean(false);

	protected long priorOnLocationChangedTime = 0;
	
	// the last time a location was pulled form the phone.
	protected long lastLocationPullTime = 0;
	
	protected synchronized long getLastLocationPullTime() {
		return lastLocationPullTime;
	}

	protected synchronized void setLastLocationPullTime(long lastLocationPullTime) {
		this.lastLocationPullTime = lastLocationPullTime;
	}
	
	private final GeometryFactory geometryFactory = new GeometryFactory();
	
	/**
	 * GPS Sensitivity Setting
	 * 
	 * @return
	 */
	private synchronized long getMinimumDistanceChangeForUpdates() {
		return PreferenceHelper.getInstance(mContext).getValue(R.string.gpsSensitivityKey, Long.class, R.string.gpsSensitivityDefaultValue);
	}
	
	/**
	 * User Reporting Frequency Setting
	 * 
	 * @return
	 */
	protected final synchronized long getUserReportingFrequency() {
		return PreferenceHelper.getInstance(mContext).getValue(R.string.userReportingFrequencyKey, Long.class, R.string.userReportingFrequencyDefaultValue);
	}
	
	protected final synchronized boolean getLocationServiceEnabled() {
	    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        return preferences.getBoolean(mContext.getString(R.string.locationServiceEnabledKey), false);
	}
	
    protected final synchronized boolean shouldReportUserLocation() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        return preferences.getBoolean("reportLocation", false);
    }
	
	protected boolean locationUpdatesEnabled = false;

	public synchronized boolean getLocationUpdatesEnabled() {
		return locationUpdatesEnabled;
	}
	
	private final Handler mHandler = new Handler();

	public LocationService(Context context) {
		this.mContext = context;
		this.userHelper = UserHelper.getInstance(mContext);
		this.locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
		batteryStatus = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		PreferenceManager.getDefaultSharedPreferences(mContext).registerOnSharedPreferenceChangeListener(this);
		preferenceSemaphore.set(false);
	}
	
	public void registerOnLocationListener(LocationListener listener) {
	    locationListeners.add(listener);
	    
		Location lastLocation = getLocation();
	    if (lastLocation != null) {        
	        Log.d(LOG_NAME, "location service added listener, pushing last known location to listener");
	        listener.onLocationChanged(new Location(lastLocation));
	    }
	}
	
	public void unregisterOnLocationListener(LocationListener listener) {
	    locationListeners.remove(listener);
	}
	
	private void requestLocationUpdates() {
		if (locationManager != null) {
			final List<String> providers = locationManager.getAllProviders();
			if (providers != null) {
				
				if (providers.contains(LocationManager.GPS_PROVIDER)) {
					locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, getMinimumDistanceChangeForUpdates(), this);
					locationUpdatesEnabled = true;
				}
				
				if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
					locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, getMinimumDistanceChangeForUpdates(), this);
					locationUpdatesEnabled = true;
				}
			}
		}
	}

	private void removeLocationUpdates() {
		locationManager.removeUpdates(this);
		locationUpdatesEnabled = false;
	}

	/**
	 * Return a location or <code>null</code> is no location is available.
	 * 
	 * @return A {@link Location}.
	 */
	public Location getLocation() {
		Location location = null;

		// if GPS Enabled get Location using GPS Services
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		}
		if(location == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}
		return location;
	}


	/**
	 * Method to show settings alert dialog On pressing Settings button will
	 * launch Settings Options
	 */
	public void showSettingsAlert() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
		alertDialog.setTitle("GPS settings");
		alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");
		alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				mContext.startActivity(intent);
			}
		});
		alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		alertDialog.show();
	}

	@Override
	public void onLocationChanged(Location location) {
		if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
			setLastLocationPullTime(System.currentTimeMillis());

			if (shouldReportUserLocation()) {
				saveLocation(location);
			}
		}

		if (location.getProvider().equals(LocationManager.GPS_PROVIDER) || priorOnLocationChangedTime == 0 || (new Date().getTime() - priorOnLocationChangedTime > getUserReportingFrequency())) {
			for (LocationListener listener : locationListeners) {
				listener.onLocationChanged(location);
			}
		}
		priorOnLocationChangedTime = new Date().getTime();
	}

	@Override
	public void onProviderDisabled(String provider) {
       for (LocationListener listener : locationListeners) {
            listener.onProviderDisabled(provider);
        }
	}

	@Override
	public void onProviderEnabled(String provider) {
       for (LocationListener listener : locationListeners) {
            listener.onProviderEnabled(provider);
        }
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
       for (LocationListener listener : locationListeners) {
            listener.onStatusChanged(provider, status, extras);
        }
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	public void init() {
	    if (getLocationServiceEnabled()) start();
	}

	/**
	 * Call this to start the location service
	 */
	private void start() {
		 if(!isPolling()) {
			pollingRunning = Boolean.TRUE;
			createLocationPollingThread().start();
		}
	}
	
    private void stop() {
    	pollingRunning = Boolean.FALSE;
		if (locationManager != null) {
			synchronized (preferenceSemaphore) {
				preferenceSemaphore.notifyAll();
			}
			removeLocationUpdates();
		}
    }
	
	/**
	 * Call this to stop the location service
	 */
	public void destroy() {
		stop();
		PreferenceManager.getDefaultSharedPreferences(mContext).unregisterOnSharedPreferenceChangeListener(this);
	}
	
	/**
	 * Polls for locations at time specified by the settings.
	 * 
	 * @return
	 */
	private Thread createLocationPollingThread() {
		return new Thread(new Runnable() {
			public void run() {
				Looper.prepare();
				
				while (isPolling()) {
					try {
						long userReportingFrequency = getUserReportingFrequency();
						// if we should pull, then do it.
						if(userReportingFrequency > 0) {
							
							// make sure the service is configured to report locations
							if(!getLocationUpdatesEnabled()) {
								mHandler.post(new Runnable() {
									public void run() {
										removeLocationUpdates();
										requestLocationUpdates();
									}
								});
							}
							
							final Location location = getLocation();
							setLastLocationPullTime(System.currentTimeMillis());
							
							if (location != null && shouldReportUserLocation()) {
								mHandler.post(new Runnable() {
									public void run() {
										saveLocation(location, new Date().getTime());
									}
								});
							}
							long currentTime = new Date().getTime();
							long lLPullTime = getLastLocationPullTime();
							// we only need to pull if a location has not been saved in the last 'pollingInterval' seconds.
							// the location could have been saved from a motion event, or from the last time the parent loop ran
							// use local variables in order to maintain data integrity across instructions. 
							while (((lLPullTime = getLastLocationPullTime()) + (userReportingFrequency = getUserReportingFrequency()) > (currentTime = new Date().getTime())) && isPolling()) {
								synchronized (preferenceSemaphore) {
									preferenceSemaphore.wait(lLPullTime + userReportingFrequency - currentTime);
									// this means we need to re-read the gps sensitivity
									if(preferenceSemaphore.get() == true) {
										break;
									}
								}
							}
							synchronized (preferenceSemaphore) {
								preferenceSemaphore.set(false);
							}
						} else {
							// disable location updates
							mHandler.post(new Runnable() {
								public void run() {
									removeLocationUpdates();
								}
							});
							
							synchronized (preferenceSemaphore) {
								preferenceSemaphore.wait();
							}
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
						pollingRunning = Boolean.FALSE;
					}
				}
			}
		});

	}

	private void saveLocation(Location location) {
		saveLocation(location, null);
	}

	@SuppressLint("NewApi") 
	private void saveLocation(Location location, Long echoTime) {
		if (location != null && location.getTime() > 0) {
			Collection<LocationProperty> locationProperties = new ArrayList<LocationProperty>();
			long currentTimeMillis = System.currentTimeMillis();
			locationProperties.add(new LocationProperty("currentTimeMillis", currentTimeMillis));
			
			if (location.getTime() > currentTimeMillis) {
				locationProperties.add(new LocationProperty("timeInFuture", true));
				Log.w(LOG_NAME, "Location was in future.  Setting location time to system current time.");
				location.setTime(currentTimeMillis);
			}

			// INTEGRATION WITH LOCATION DATASTORE
			LocationHelper locationHelper = LocationHelper.getInstance(mContext);

			// build properties
			// locationProperties.add(new LocationProperty("timestamp", DateFormatFactory.getISO8601().format(new Date(location.getTime()))));
			if (echoTime != null) {
				locationProperties.add(new LocationProperty("echoTime", echoTime));
				locationProperties.add(new LocationProperty("accuracy", Float.valueOf(Math.max(Long.valueOf(getMinimumDistanceChangeForUpdates()).floatValue(), location.getAccuracy()))));
			} else {
				locationProperties.add(new LocationProperty("accuracy", Float.valueOf(location.getAccuracy())));
			}
			locationProperties.add(new LocationProperty("bearing", location.getBearing()));
			locationProperties.add(new LocationProperty("speed", location.getSpeed()));
			locationProperties.add(new LocationProperty("provider", location.getProvider()));
			locationProperties.add(new LocationProperty("altitude", location.getAltitude()));

			int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			if(level != -1) {
				locationProperties.add(new LocationProperty("battery_level", level));
			}

			// build geometry
			LocationGeometry locationGeometry = new LocationGeometry(geometryFactory.createPoint(new Coordinate(location.getLongitude(), location.getLatitude())));

			User currentUser = null;
			try {
				currentUser = userHelper.readCurrentUser();
			} catch (UserException e) {
				Log.e(LOG_NAME, "Could not get current User!");
			}
			
			// build location
			mil.nga.giat.mage.sdk.datastore.location.Location loc = new mil.nga.giat.mage.sdk.datastore.location.Location("Feature", currentUser, locationProperties, locationGeometry, new Date(location.getTime()), currentUser.getCurrentEvent());
			locationProperties.add(new LocationProperty("locationObjectTime", loc.getTimestamp()));

			loc.setLocationGeometry(locationGeometry);
			loc.setProperties(locationProperties);

			// save the location
			try {
				loc = locationHelper.create(loc);
				Log.d(LOG_NAME, "Save location: " + loc.getLocationGeometry().getGeometry());
			} catch (LocationException le) {
				Log.e(LOG_NAME, "Unable to save current location locally!", le);
			}
		}
	}
	
	/**
	 * Will alert the polling thread that changes have been made
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
	    if (key.equalsIgnoreCase(mContext.getString(R.string.locationServiceEnabledKey))) {
	        boolean locationServiceEnabled = sharedPreferences.getBoolean(mContext.getString(R.string.locationServiceEnabledKey), false);
	        if (locationServiceEnabled) {
	            start();
	        } else {
	            stop();
	        }
	    }
	    else if (key.equalsIgnoreCase(mContext.getString(R.string.gpsSensitivityKey))) {
			synchronized (preferenceSemaphore) {
				Log.d(LOG_NAME, "GPS sensitivity changed, distance in meters for change: " + getMinimumDistanceChangeForUpdates());
				// this will cause the polling-thread to reset the gps sensitivity
				locationUpdatesEnabled = false;
				preferenceSemaphore.set(true);
				preferenceSemaphore.notifyAll();
			}
		} else if (key.equalsIgnoreCase(mContext.getString(R.string.userReportingFrequencyKey))) {
			synchronized (preferenceSemaphore) {
				Log.d(LOG_NAME, "Location service frequency changed: " + getUserReportingFrequency());
				preferenceSemaphore.notifyAll();
			}
		}
	}

}