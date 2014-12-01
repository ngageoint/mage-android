package mil.nga.giat.mage.sdk.push;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import mil.nga.giat.mage.sdk.ConnectivityAwareIntentService;
import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.event.IObservationEventListener;
import mil.nga.giat.mage.sdk.http.post.MageServerPostRequests;
import mil.nga.giat.mage.sdk.login.LoginTaskFactory;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import android.content.Intent;
import android.util.Log;

public class ObservationPushIntentService extends ConnectivityAwareIntentService implements IObservationEventListener {

	private static final String LOG_NAME = ObservationPushIntentService.class.getName();

	// in milliseconds
	private long pushFrequency;

	protected AtomicBoolean pushSemaphore = new AtomicBoolean(false);

	public ObservationPushIntentService() {
		super(LOG_NAME);
	}

	protected final long getObservationPushFrequency() {
		return PreferenceHelper.getInstance(getApplicationContext()).getValue(R.string.observationPushFrequencyKey, Long.class, R.string.observationPushFrequencyDefaultValue);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		super.onHandleIntent(intent);
		ObservationHelper.getInstance(getApplicationContext()).addListener(this);
		pushFrequency = getObservationPushFrequency();
		while (!isCanceled) {
			if (isConnected && !LoginTaskFactory.getInstance(getApplicationContext()).isLocalLogin()) {
				pushFrequency = getObservationPushFrequency();

				// push dirty observations
				ObservationHelper observationHelper = ObservationHelper.getInstance(getApplicationContext());
				List<Observation> observations = observationHelper.getDirty();
				for (Observation observation : observations) {
					if (isCanceled) {
						break;
					}
					Log.d(LOG_NAME, "Pushing observation with id: " + observation.getId());
					observation = MageServerPostRequests.postObservation(observation, getApplicationContext());
					if(observation != null) {
						Log.d(LOG_NAME, "Pushed observation with remote_id: " + observation.getRemoteId());
					}
				}
			} else {
				Log.d(LOG_NAME, "The device is currently disconnected. Can't push observations.");
				pushFrequency = Math.min(pushFrequency * 2, 30 * 60 * 1000);
			}
			long lastFetchTime = new Date().getTime();
			long currentTime = new Date().getTime();

			try {
				while (lastFetchTime + pushFrequency > (currentTime = new Date().getTime())) {
					synchronized (pushSemaphore) {
						Log.d(LOG_NAME, "Observation push sleeping for " + (lastFetchTime + pushFrequency - currentTime) + "ms.");
						pushSemaphore.wait(lastFetchTime + pushFrequency - currentTime);
						if (pushSemaphore.get() == true) {
							break;
						}
					}
				}
				synchronized (pushSemaphore) {
					pushSemaphore.set(false);
				}
			} catch (InterruptedException ie) {
				Log.e(LOG_NAME, "Interupted.  Unable to sleep " + pushFrequency, ie);
			} finally {
				isConnected = ConnectivityUtility.isOnline(getApplicationContext());
			}
		}
	}

	@Override
	public void onObservationCreated(Collection<Observation> observations) {
		for (Observation observation : observations) {
			if (observation.isDirty()) {
				synchronized (pushSemaphore) {
					pushSemaphore.set(true);
					pushSemaphore.notifyAll();
				}
				break;
			}
		}
	}

	@Override
	public void onObservationUpdated(Observation observation) {
		if (observation.isDirty()) {
			synchronized (pushSemaphore) {
				pushSemaphore.set(true);
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

	@Override
	public void onObservationDeleted(Observation observation) {
		// TODO Auto-generated method stub
	}
}
