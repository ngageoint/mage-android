package mil.nga.giat.mage.sdk.push;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import mil.nga.giat.mage.sdk.ConnectivityAwareIntentService;
import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.LocationException;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.http.post.MageServerPostRequests;
import mil.nga.giat.mage.sdk.login.LoginTaskFactory;

public class LocationPushIntentService extends ConnectivityAwareIntentService implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String LOG_NAME = LocationPushIntentService.class.getName();

	public static final int minNumberOfLocationsToKeep = 40;

	protected final AtomicBoolean pushSemaphore = new AtomicBoolean(false);

	public LocationPushIntentService() {
		super(LOG_NAME);
	}

	protected final int getLocationPushFrequency() {
		return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(getString(R.string.locationPushFrequencyKey), getResources().getInteger(R.integer.locationPushFrequencyDefaultValue));
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		super.onHandleIntent(intent);
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
		long pushFrequency = getLocationPushFrequency();
		while (!isCanceled) {
			if (isConnected && !LoginTaskFactory.getInstance(getApplicationContext()).isLocalLogin()) {
				pushFrequency = getLocationPushFrequency();
				LocationHelper locationHelper = LocationHelper.getInstance(getApplicationContext());

				long batchSize = 100;
				int failedAttemptCount = 0;

				User currentUser = null;
				try {
					currentUser = UserHelper.getInstance(getApplicationContext()).readCurrentUser();
				} catch (UserException e) {
					e.printStackTrace();
				}

				List<Location> locations = locationHelper.getCurrentUserLocations(getApplicationContext(), batchSize, false);
				while (!locations.isEmpty() && failedAttemptCount < 3) {

					// post locations by event
					Event event = locations.get(0).getEvent();

					List<Location> eventLocations = new ArrayList<Location>();
					for(Location l : locations) {
						if(event.equals(l.getEvent())) {
							eventLocations.add(l);
						}
					}

					Boolean status = MageServerPostRequests.postLocations(eventLocations, event, getApplicationContext());
					// we've sync-ed. Don't need the locations anymore.
					if (status) {
						Log.d(LOG_NAME, "Pushed " + eventLocations.size() + " locations.");

						// Delete location where:
						// the user is current user
						// the remote id is set. (have been sent to server)
						// past the lower n amount!
						try {
							if (currentUser != null) {
								Dao<Location, Long> locationDao = DaoStore.getInstance(getApplicationContext()).getLocationDao();
								QueryBuilder<Location, Long> queryBuilder = locationDao.queryBuilder();
								Where<Location, Long> where = queryBuilder.where().eq("user_id", currentUser.getId());
								where.and().isNotNull("remote_id").and().eq("event_id", event.getId());
								queryBuilder.orderBy("timestamp", false);
								List<Location> locationsToDelete = queryBuilder.query();
								Stack<Long> locationIDsToDelete = new Stack<Long>(); 

								for (int i = minNumberOfLocationsToKeep; i < locationsToDelete.size(); i++) {
									locationIDsToDelete.push(locationsToDelete.get(i).getId());
								}
								try {
									LocationHelper.getInstance(getApplicationContext()).delete(locationIDsToDelete.toArray(new Long[locationIDsToDelete.size()]));
								} catch (LocationException e) {
									Log.e(LOG_NAME, "Could not delete locations.", e);
									for (int i = 0; i < locationIDsToDelete.size(); i++) {
										try {
											LocationHelper.getInstance(getApplicationContext()).delete(locationIDsToDelete.pop());
										} catch (LocationException e1) {
											Log.e(LOG_NAME, "Could not delete the location.", e);
											continue;
										}
									}
								}
							}
						} catch (SQLException e) {
							Log.e(LOG_NAME, "Problem deleting locations.", e);
						}
					} else {
						Log.e(LOG_NAME, "Failed to push locations.");
						failedAttemptCount++;
					}
					locations = locationHelper.getCurrentUserLocations(getApplicationContext(), batchSize, false);
				}
			} else {
				Log.d(LOG_NAME, "The device is currently disconnected. Can't push locations.");
				pushFrequency = Math.min(pushFrequency * 2, 30 * 60 * 1000);
			}
			long lastFetchTime = new Date().getTime();
			long currentTime = new Date().getTime();

			try {
				while (lastFetchTime + (pushFrequency = getLocationPushFrequency()) > (currentTime = new Date().getTime())) {
					synchronized (pushSemaphore) {
						Log.d(LOG_NAME, "Location push sleeping for " + (lastFetchTime + pushFrequency - currentTime) + "ms.");
						pushSemaphore.wait(lastFetchTime + pushFrequency - currentTime);
						if (pushSemaphore.get()) {
							break;
						}
					}
				}
				synchronized (pushSemaphore) {
					pushSemaphore.set(false);
				}
			} catch (InterruptedException ie) {
				Log.e(LOG_NAME, "Interrupted.  Unable to sleep " + pushFrequency, ie);
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
		if (key.equalsIgnoreCase(getApplicationContext().getString(R.string.locationPushFrequencyKey))) {
			synchronized (pushSemaphore) {
				pushSemaphore.notifyAll();
			}
		}
	}

	@Override
	public void onAnyConnected() {
		super.onAnyConnected();
		synchronized (pushSemaphore) {
			pushSemaphore.set(true);
			pushSemaphore.notifyAll();
		}
	}
}
