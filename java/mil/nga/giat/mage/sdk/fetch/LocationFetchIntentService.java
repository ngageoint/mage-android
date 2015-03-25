package mil.nga.giat.mage.sdk.fetch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import mil.nga.giat.mage.sdk.ConnectivityAwareIntentService;
import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.location.LocationProperty;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.event.IScreenEventListener;
import mil.nga.giat.mage.sdk.http.get.MageServerGetRequests;
import mil.nga.giat.mage.sdk.login.LoginTaskFactory;
import mil.nga.giat.mage.sdk.screen.ScreenChangeReceiver;

public class LocationFetchIntentService extends ConnectivityAwareIntentService implements OnSharedPreferenceChangeListener, IScreenEventListener {

	private static final String LOG_NAME = LocationFetchIntentService.class.getName();

	public LocationFetchIntentService() {
		super(LOG_NAME);
	}

	protected final AtomicBoolean fetchSemaphore = new AtomicBoolean(false);

	protected final synchronized long getLocationFetchFrequency() {
		return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(getString(R.string.userFetchFrequencyKey), getResources().getInteger(R.integer.userFetchFrequencyDefaultValue));
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		super.onHandleIntent(intent);
		ScreenChangeReceiver.getInstance().addListener(this);
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
		LocationHelper locationHelper = LocationHelper.getInstance(getApplicationContext());
		UserHelper userHelper = UserHelper.getInstance(getApplicationContext());
		UserServerFetch userFetch = new UserServerFetch(getApplicationContext());
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		while (!isCanceled) {
			Boolean isDataFetchEnabled = sharedPreferences.getBoolean(getString(R.string.dataFetchEnabledKey), getResources().getBoolean(R.bool.dataFetchEnabledDefaultValue));

			if (isConnected && isDataFetchEnabled && !LoginTaskFactory.getInstance(getApplicationContext()).isLocalLogin()) {

				Log.d(LOG_NAME, "The device is currently connected. Attempting to fetch Locations...");
				try {
					Collection<Location> locations = MageServerGetRequests.getLocations(getApplicationContext());
					for (Location location : locations) {
						if (isCanceled) {
							break;
						}

						// make sure that the user exists and is persisted in the local data-store
						String userId = null;
						LocationProperty userIdProperty = location.getPropertiesMap().get("userId");
						if (userIdProperty != null) {
							userId = userIdProperty.getValue().toString();
						}

						if (userId != null) {
							User user = userHelper.read(userId);
							// TODO : test the timer to make sure users are updated as needed!
							final long sixHoursInMilliseconds = 6 * 60 * 60 * 1000;
							if (user == null || (new Date()).after(new Date(user.getFetchedDate().getTime() + sixHoursInMilliseconds))) {
								// get any users that were not recognized or expired
								userFetch.fetch(userId);
								user = userHelper.read(userId);
							}
							location.setUser(user);

							// if there is no existing location, create one
							Location l = locationHelper.read(location.getRemoteId());
							if (l == null) {
								// delete old location and create new one
								if (user != null) {
									// don't pull your own locations for now!
									if (!user.isCurrentUser()) {
										userId = String.valueOf(user.getId());
										locationHelper.create(location);
										int numberOfLocationsDeleted = locationHelper.deleteUserLocations(userId, true);
									}
								} else {
									Log.w(LOG_NAME, "A location with no user was found and discarded.  User id: " + userId);
								}
							}
						}
					}
				} catch (Exception e) {
					Log.e(LOG_NAME, "There was a failure while performing an Location Fetch operation.", e);
				}
			} else {
				Log.d(LOG_NAME, "The device is currently disconnected, or data fetch is disabled. Not performing fetch.");
			}

			long frequency = getLocationFetchFrequency();
			long lastFetchTime = new Date().getTime();
			long currentTime = new Date().getTime();
			try {
				while (lastFetchTime + (frequency = getLocationFetchFrequency()) > (currentTime = new Date().getTime())) {
					synchronized (fetchSemaphore) {
						Log.d(LOG_NAME, "Location fetch sleeping for " + (lastFetchTime + frequency - currentTime) + "ms.");
						fetchSemaphore.wait(lastFetchTime + frequency - currentTime);
						if (fetchSemaphore.get()) {
							break;
						}
					}
				}
				synchronized (fetchSemaphore) {
					fetchSemaphore.set(false);
				}
			} catch (InterruptedException ie) {
				Log.e(LOG_NAME, "Interrupted.  Unable to sleep " + frequency, ie);
			} finally {
				isConnected = ConnectivityUtility.isOnline(getApplicationContext());
			}
		}
	}

	/**
	 * Will alert the fetching thread that changes have been made
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equalsIgnoreCase(getApplicationContext().getString(R.string.userFetchFrequencyKey)) || key.equalsIgnoreCase(getApplicationContext().getString(R.string.dataFetchEnabledKey))) {
			synchronized (fetchSemaphore) {
				fetchSemaphore.notifyAll();
			}
		}
	}

	@Override
	public void onAnyConnected() {
		super.onAnyConnected();
		synchronized (fetchSemaphore) {
			fetchSemaphore.set(true);
			fetchSemaphore.notifyAll();
		}
	}

	@Override
	public void onScreenOn() {
		synchronized (fetchSemaphore) {
			fetchSemaphore.set(true);
			fetchSemaphore.notifyAll();
		}
	}
}
