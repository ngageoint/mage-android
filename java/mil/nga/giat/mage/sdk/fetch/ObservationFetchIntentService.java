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

public class ObservationFetchIntentService extends ConnectivityAwareIntentService implements OnSharedPreferenceChangeListener, IScreenEventListener, IEventEventListener {

	private static final String LOG_NAME = ObservationFetchIntentService.class.getName();

	private boolean firstTimeToRun = true;
	private ObservationServerFetch observationServerFetch;
	
	public ObservationFetchIntentService() {
		super(LOG_NAME);
	}

	protected final AtomicBoolean fetchSemaphore = new AtomicBoolean(false);

	protected final AtomicBoolean needToFetchIcons = new AtomicBoolean(true);

	protected final synchronized long getObservationFetchFrequency() {
		return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(getString(R.string.observationFetchFrequencyKey), getResources().getInteger(R.integer.observationFetchFrequencyDefaultValue));
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		super.onHandleIntent(intent);

		// Register listeners
		ScreenChangeReceiver.getInstance().addListener(this);
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
		UserHelper.getInstance(getApplicationContext()).addListener(this);

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		needToFetchIcons.set(true);

		observationServerFetch = new ObservationServerFetch(getApplicationContext());

		while (!isCanceled) {
			Boolean isDataFetchEnabled = sharedPreferences.getBoolean(getString(R.string.dataFetchEnabledKey), getResources().getBoolean(R.bool.dataFetchEnabledDefaultValue));

			if (isConnected && isDataFetchEnabled && !LoginTaskFactory.getInstance(getApplicationContext()).isLocalLogin()) {

				Event event = EventHelper.getInstance(getApplicationContext()).getCurrentEvent();

				// Pull the icons here
				if (needToFetchIcons.get()) {
					new ObservationBitmapFetch(getApplicationContext()).fetch(event);
					needToFetchIcons.set(false);
				}

				observationServerFetch.fetch(!firstTimeToRun);
			} else {
				Log.d(LOG_NAME, "The device is currently disconnected, or data fetch is disabled. Not performing fetch.");
			}

			long frequency = getObservationFetchFrequency();
			long lastFetchTime = new Date().getTime();
			long currentTime;
			try {
				while (lastFetchTime + (frequency = getObservationFetchFrequency()) > (currentTime = new Date().getTime())) {
					synchronized (fetchSemaphore) {
						Log.d(LOG_NAME, "Observation fetch sleeping for " + (lastFetchTime + frequency - currentTime) + "ms.");
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

	@Override
	public void onEventChanged() {
		synchronized (fetchSemaphore) {
			needToFetchIcons.set(true);
			fetchSemaphore.set(true);
			fetchSemaphore.notifyAll();
		}
	}
}
