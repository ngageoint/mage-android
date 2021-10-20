package mil.nga.giat.mage.sdk.event;

import java.util.Collection;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;

public interface IObservationEventListener extends IEventListener {

	void onObservationCreated(final Collection<Observation> observations, Boolean sendUserNotifcations);
	
	void onObservationUpdated(final Observation observation);
	
	void onObservationDeleted(final Observation observation);
}
