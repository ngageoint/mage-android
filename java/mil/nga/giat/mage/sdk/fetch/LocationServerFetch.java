package mil.nga.giat.mage.sdk.fetch;

import android.content.Context;
import android.util.Log;

import java.util.Collection;
import java.util.Date;

import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.location.LocationProperty;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.http.resource.LocationResource;

public class LocationServerFetch extends AbstractServerFetch {

	private static final String LOG_NAME = LocationServerFetch.class.getName();

	private UserHelper userHelper;
	private UserServerFetch userFetch;
	private LocationHelper locationHelper;
	private LocationResource locationResource;

	public LocationServerFetch(Context context) {
		super(context);

		userHelper = UserHelper.getInstance(context);
		locationHelper = LocationHelper.getInstance(context);
		locationResource = new LocationResource(context);
		userFetch = new UserServerFetch(mContext);
	}

	public void fetch() {
		User currentUser = null;
		try {
			currentUser = userHelper.readCurrentUser();
		} catch (UserException e) {
			Log.e(LOG_NAME, "Error rading current user.", e);
		}

		Event event = EventHelper.getInstance(mContext).getCurrentEvent();
		Collection<Location> locations = locationResource.getLocations(event);
		for (Location location : locations) {

			// make sure that the user exists and is persisted in the local data-store
			String userId = null;
			LocationProperty userIdProperty = location.getPropertiesMap().get("userId");
			if (userIdProperty != null) {
				userId = userIdProperty.getValue().toString();
			}
			try {
				if (userId != null) {
					User user = userHelper.read(userId);
					// TODO : test the timer to make sure users are updated as needed!
					final long sixHoursInMilliseconds = 6 * 60 * 60 * 1000;
					if (user == null || (new Date()).after(new Date(user.getFetchedDate().getTime() + sixHoursInMilliseconds))) {
						// get any users that were not recognized or expired
						userFetch.fetch(userId);
						user = userHelper.read(userId);
					}
					location.setUser(user);

					// if there is no existing location, create one
					Location l = locationHelper.read(location.getRemoteId());
					if (l == null) {
						// delete old location and create new one
						if (user != null) {
							// don't pull your own locations
							if (!user.equals(currentUser)) {
								userId = String.valueOf(user.getId());
								location = locationHelper.create(location);
								locationHelper.deleteUserLocations(userId, true, location.getEvent());
							}
						} else {
							Log.w(LOG_NAME, "A location with no user was found and discarded.  User id: " + userId);
						}
					}
				}
			} catch (Exception e) {
				Log.e(LOG_NAME, "There was a failure while performing an Location Fetch operation.", e);
			}
		}
	}
}