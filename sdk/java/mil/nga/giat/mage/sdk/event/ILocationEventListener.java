package mil.nga.giat.mage.sdk.event;

import java.util.Collection;

import mil.nga.giat.mage.sdk.datastore.location.Location;

public interface ILocationEventListener extends IEventListener {

	public void onLocationCreated(final Collection<Location> location);
	
	public void onLocationUpdated(final Location location);
	
	public void onLocationDeleted(final Collection<Location> location);
}
