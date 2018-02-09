package mil.nga.giat.mage.sdk.fetch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import mil.nga.giat.mage.sdk.ConnectivityAwareIntentService;
import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.event.IEventEventListener;
import mil.nga.giat.mage.sdk.event.IScreenEventListener;
import mil.nga.giat.mage.sdk.login.LoginTaskFactory;
import mil.nga.giat.mage.sdk.screen.ScreenChangeReceiver;

public class LocationFetchIntentService extends ConnectivityAwareIntentService implements OnSharedPreferenceChangeListener, IScreenEventListener, IEventEventListener {

	private static final String LOG_NAME = LocationFetchIntentService.class.getName();

	protected final AtomicBoolean fetchSemaphore = new AtomicBoolean(false);

	public LocationFetchIntentService() {
		super(LOG_NAME);
	}

	protected final synchronized long getLocationFetchFrequency() {
		return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(getString(R.string.userFetchFrequencyKey), getResources().getInteger(R.integer.userFetchFrequencyDefaultValue));
	}

	protected final synchronized Event getCurrentEvent() {
		return EventHelper.getInstance(getApplicationContext()).getCurrentEvent();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		super.onHandleIntent(intent);

		// Register listeners
		ScreenChangeReceiver.getInstance().addListener(this);
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
		UserHelper.getInstance(getApplicationContext()).addListener(this);

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		while (!isCanceled) {
			Boolean isDataFetchEnabled = sharedPreferences.getBoolean(getString(R.string.dataFetchEnabledKey), getResources().getBoolean(R.bool.dataFetchEnabledDefaultValue));
			Long eventId = getCurrentEvent().getId();

			if (isConnected && isDataFetchEnabled && !LoginTaskFactory.getInstance(getApplicationContext()).isLocalLogin()) {
				new LocationServerFetch(getApplicationContext()).fetch();
			} else {
				Log.d(LOG_NAME, "The device is currently disconnected, or data fetch is disabled. Not performing fetch.");
			}

			long frequency = getLocationFetchFrequency();
			long lastFetchTime = new Date().getTime();
			long currentTime;
			Event currentEvent;
			try {
				while ((currentEvent = getCurrentEvent()) != null &&
						eventId.equals(currentEvent.getId()) &&
						lastFetchTime + (frequency = getLocationFetchFrequency()) > (currentTime = new Date().getTime())) {
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

	@Override
	public void onEventChanged() {
		synchronized (fetchSemaphore) {
			fetchSemaphore.set(true);
			fetchSemaphore.notifyAll();
		}
	}
}
