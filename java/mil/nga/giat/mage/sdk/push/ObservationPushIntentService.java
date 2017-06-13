package mil.nga.giat.mage.sdk.push;

import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import mil.nga.giat.mage.sdk.ConnectivityAwareIntentService;
import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationFavorite;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationImportant;
import mil.nga.giat.mage.sdk.event.IObservationEventListener;
import mil.nga.giat.mage.sdk.exceptions.ObservationException;
import mil.nga.giat.mage.sdk.http.resource.ObservationResource;

public class ObservationPushIntentService extends ConnectivityAwareIntentService implements IObservationEventListener {

	private static final String LOG_NAME = ObservationPushIntentService.class.getName();

	// in milliseconds
	private long pushFrequency;

	protected final AtomicBoolean pushSemaphore = new AtomicBoolean(false);

	public ObservationPushIntentService() {
		super(LOG_NAME);
	}

	protected final long getObservationPushFrequency() {
		return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(getString(R.string.observationPushFrequencyKey), getResources().getInteger(R.integer.observationPushFrequencyDefaultValue));
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		super.onHandleIntent(intent);
		ObservationHelper.getInstance(getApplicationContext()).addListener(this);
		pushFrequency = getObservationPushFrequency();

		ObservationResource observationResource = new ObservationResource(getApplicationContext());
		while (!isCanceled) {
			pushFrequency = getObservationPushFrequency();

			// push dirty observations
			pushObservations(observationResource);

			// push dirty observation favorites
			pushImportant(observationResource);

			// push dirty observation important
			pushFavorite(observationResource);

			long lastFetchTime = new Date().getTime();
			long currentTime = new Date().getTime();

			try {
				while (lastFetchTime + pushFrequency > (currentTime = new Date().getTime())) {
					synchronized (pushSemaphore) {
						Log.d(LOG_NAME, "Observation push sleeping for " + (lastFetchTime + pushFrequency - currentTime) + "ms.");
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
				Log.e(LOG_NAME, "Unable to sleep " + pushFrequency, ie);
			} finally {
				isConnected = ConnectivityUtility.isOnline(getApplicationContext());
			}
		}
	}

	@Override
	public void onObservationCreated(Collection<Observation> observations, Boolean sendUserNotifcations) {
		for (Observation observation : observations) {
			if (isObservationDirty(observation)) {
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
		if (isObservationDirty(observation)) {
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

	private boolean isObservationDirty(Observation observation) {

		if (observation.isDirty()) return true;

		ObservationImportant important = observation.getImportant();
		if (important != null && important.isDirty()) {
			return true;
		}

		for (ObservationFavorite favorite : observation.getFavorites()) {
			if (favorite.isDirty()) {
				return true;
			}
		}

		return false;
	}

	private void pushObservations(ObservationResource observationResource) {
		ObservationHelper observationHelper = ObservationHelper.getInstance(getApplicationContext());
		for (Observation observation : observationHelper.getDirty()) {
			if (isCanceled) {
				break;
			}
			Log.d(LOG_NAME, "Pushing observation with id: " + observation.getId());
			observation = observationResource.saveObservation(observation);
			if (observation != null) {
				Log.d(LOG_NAME, "Pushed observation with remote_id: " + observation.getRemoteId());
			}
		}
	}

	private void pushImportant(ObservationResource observationResource) {
		ObservationHelper observationHelper = ObservationHelper.getInstance(getApplicationContext());
		try {
			for (Observation observation : observationHelper.getDirtyImportant()) {
				observationResource.toogleImporant(observation);
				if (observation != null) {
					Log.d(LOG_NAME, "Pushed observation important with remote_id: " + observation.getRemoteId());
				}
			}
		} catch (ObservationException e) {
			Log.e(LOG_NAME, "Error pushing observation important", e);
		}
	}

	private void pushFavorite(ObservationResource observationResource) {
		ObservationHelper observationHelper = ObservationHelper.getInstance(getApplicationContext());
		try {
			for (ObservationFavorite favorite : observationHelper.getDirtyFavorites()) {
				Observation observation = observationResource.toogleFavorite(favorite);
				if (observation != null) {
					Log.d(LOG_NAME, "Pushed observation favorite with remote_id: " + observation.getRemoteId());
				}
			}
		} catch (ObservationException e) {
			Log.e(LOG_NAME, "Error pushing observation favorite", e);
		}
	}
}
