package mil.nga.giat.mage.sdk.event;

import java.util.Collection;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;

public interface IObservationEventListener extends IEventListener {

	public void onObservationCreated(final Collection<Observation> observations);
	
	public void onObservationUpdated(final Observation observation);
	
	public void onObservationDeleted(final Observation observation);
}
