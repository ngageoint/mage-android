package mil.nga.giat.mage.sdk.fetch;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import mil.nga.giat.mage.sdk.ConnectivityAwareIntentService;
import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.datastore.common.State;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.event.IScreenEventListener;
import mil.nga.giat.mage.sdk.http.get.MageServerGetRequests;
import mil.nga.giat.mage.sdk.login.LoginTaskFactory;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import mil.nga.giat.mage.sdk.screen.ScreenChangeReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

public class ObservationFetchIntentService extends ConnectivityAwareIntentService implements OnSharedPreferenceChangeListener, IScreenEventListener {

	private static final String LOG_NAME = ObservationFetchIntentService.class.getName();

	private boolean firstTimeToRun = true;
	
	public ObservationFetchIntentService() {
		super(LOG_NAME);
	}

	protected AtomicBoolean fetchSemaphore = new AtomicBoolean(false);

	protected final synchronized long getobservationFetchFrequency() {
		return PreferenceHelper.getInstance(getApplicationContext()).getValue(R.string.observationFetchFrequencyKey, Long.class, R.string.observationFetchFrequencyDefaultValue);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		super.onHandleIntent(intent);
		ScreenChangeReceiver.getInstance().addListener(this);
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
		ObservationHelper observationHelper = ObservationHelper.getInstance(getApplicationContext());
		UserHelper userHelper = UserHelper.getInstance(getApplicationContext());
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
		Boolean isDataFetchEnabled = sharedPreferences.getBoolean(getApplicationContext().getString(R.string.dataFetchEnabledKey), true);

		// Pull the icons here
		if (!isCanceled && isConnected && isDataFetchEnabled && !LoginTaskFactory.getInstance(getApplicationContext()).isLocalLogin()) {
			new ObservationBitmapFetch(getApplicationContext()).fetch();
		}

		while (!isCanceled) {
			isDataFetchEnabled = sharedPreferences.getBoolean(getApplicationContext().getString(R.string.dataFetchEnabledKey), true);

			if (isConnected && isDataFetchEnabled && !LoginTaskFactory.getInstance(getApplicationContext()).isLocalLogin()) {

				Log.d(LOG_NAME, "The device is currently connected. Attempting to fetch Observations...");
				List<Observation> observations = MageServerGetRequests.getObservations(getApplicationContext());
				Log.d(LOG_NAME, "Fetched " + observations.size() + " new observations");
				for (Observation observation : observations) {
					try {
						if (isCanceled) {
							break;
						}

						String userId = observation.getUserId();
						if (userId != null) {
							User user = userHelper.read(userId);
							// TODO : test the timer to make sure users are updated as needed!
							final long sixHoursInMillseconds = 6 * 60 * 60 * 1000;
							if (user == null || (new Date()).after(new Date(user.getFetchedDate().getTime() + sixHoursInMillseconds))) {
								// get any users that were not recognized or expired
								new UserServerFetch(getApplicationContext()).fetch(new String[] { userId });
							}
						}

						Observation oldObservation = observationHelper.read(observation.getRemoteId());
						if (observation.getState().equals(State.ARCHIVE) && oldObservation != null) {
							observationHelper.delete(oldObservation.getId());
							Log.d(LOG_NAME, "Deleted observation with remote_id " + observation.getRemoteId());
						} else if (!observation.getState().equals(State.ARCHIVE) && oldObservation == null) {
							observation = observationHelper.create(observation);
							Log.d(LOG_NAME, "Created observation with remote_id " + observation.getRemoteId());
						} else if (!observation.getState().equals(State.ARCHIVE) && oldObservation != null && !oldObservation.isDirty()) {
							observation.setId(oldObservation.getId());
							observation = observationHelper.update(observation);
							Log.d(LOG_NAME, "Updated observation with remote_id " + observation.getRemoteId());
						}
					} catch (Exception e) {
						Log.e(LOG_NAME, "There was a failure while performing an Observation Fetch opperation.", e);
						continue;
					}
				}
			} else {
				Log.d(LOG_NAME, "The device is currently disconnected, or data fetch is disabled. Not performing fetch.");
			}

			long frequency = getobservationFetchFrequency();
			long lastFetchTime = new Date().getTime();
			long currentTime = new Date().getTime();
			try {
				while (lastFetchTime + (frequency = getobservationFetchFrequency()) > (currentTime = new Date().getTime())) {
					synchronized (fetchSemaphore) {
						Log.d(LOG_NAME, "Observation fetch sleeping for " + (lastFetchTime + frequency - currentTime) + "ms.");
						fetchSemaphore.wait(lastFetchTime + frequency - currentTime);
						if (fetchSemaphore.get() == true) {
							break;
						}
					}
				}
				synchronized (fetchSemaphore) {
					fetchSemaphore.set(false);
				}
			} catch (InterruptedException ie) {
				Log.e(LOG_NAME, "Interupted.  Unable to sleep " + frequency, ie);
			} finally {
				isConnected = ConnectivityUtility.isOnline(getApplicationContext());
			}
			firstTimeToRun = false;
		}
	}

	/**
	 * Will alert the fetching thread that changes have been made
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equalsIgnoreCase(getApplicationContext().getString(R.string.observationFetchFrequencyKey)) || key.equalsIgnoreCase(getApplicationContext().getString(R.string.dataFetchEnabledKey))) {
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
