package mil.nga.giat.mage.sdk.event;

import java.util.Collection;

import mil.nga.giat.mage.database.model.location.Location;

public interface ILocationEventListener extends IEventListener {

	void onLocationCreated(final Collection<Location> location);
	
	void onLocationUpdated(final Location location);
	
	void onLocationDeleted(final Collection<Location> location);
}
