package mil.nga.giat.mage.sdk.fetch;

import android.content.Context;
import android.util.Log;

import java.util.Collection;
import java.util.Date;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.observation.State;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.http.resource.ObservationResource;

public class ObservationServerFetch extends AbstractServerFetch {

	private static final String LOG_NAME = ObservationServerFetch.class.getName();

	private UserHelper userHelper;
	private ObservationHelper observationHelper;
	private ObservationResource observationResource;

	public ObservationServerFetch(Context context) {
		super(context);

		userHelper = UserHelper.getInstance(context);
		observationHelper = ObservationHelper.getInstance(context);
		observationResource = new ObservationResource(context);
	}

	public void fetch(boolean sendNotifications) {
		Event event = EventHelper.getInstance(mContext).getCurrentEvent();
		Log.d(LOG_NAME, "The device is currently connected. Attempting to fetch Observations for event " + event.getName());

		Collection<Observation> observations = observationResource.getObservations(event);
		Log.d(LOG_NAME, "Fetched " + observations.size() + " new observations");
		for (Observation observation : observations) {
			try {
				String userId = observation.getUserId();
				if (userId != null) {
					User user = userHelper.read(userId);
					// TODO : test the timer to make sure users are updated as needed!
					final long sixHoursInMilliseconds = 6 * 60 * 60 * 1000;
					if (user == null || (new Date()).after(new Date(user.getFetchedDate().getTime() + sixHoursInMilliseconds))) {
						// get any users that were not recognized or expired
						new UserServerFetch(mContext).fetch(userId);
					}
				}

				Observation oldObservation = observationHelper.read(observation.getRemoteId());
				if (observation.getState().equals(State.ARCHIVE) && oldObservation != null) {
					observationHelper.delete(oldObservation);
					Log.d(LOG_NAME, "Deleted observation with remote_id " + observation.getRemoteId());
				} else if (!observation.getState().equals(State.ARCHIVE) && oldObservation == null) {
					observation = observationHelper.create(observation, sendNotifications);
					Log.d(LOG_NAME, "Created observation with remote_id " + observation.getRemoteId());
				} else if (!observation.getState().equals(State.ARCHIVE) && oldObservation != null && !oldObservation.isDirty()) { // TODO : conflict resolution
					observation.setId(oldObservation.getId());
					observation = observationHelper.update(observation);
					Log.d(LOG_NAME, "Updated observation with remote_id " + observation.getRemoteId());
				}
			} catch (Exception e) {
				Log.e(LOG_NAME, "There was a failure while performing an Observation Fetch operation.", e);
			}
		}
	}
}