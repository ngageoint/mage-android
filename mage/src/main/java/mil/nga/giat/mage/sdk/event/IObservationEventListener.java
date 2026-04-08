package mil.nga.giat.mage.sdk.event;

import java.util.Collection;

import mil.nga.giat.mage.database.model.observation.Observation;

public interface IObservationEventListener extends IEventListener {

	void onObservationsCreated(final Collection<Observation> observations, Boolean sendUserNotifcations);
	
	void onObservationsUpdated(final Collection<Observation> observations);
	
	void onObservationsDeleted();
}
