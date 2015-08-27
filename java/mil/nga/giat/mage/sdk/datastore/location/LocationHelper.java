package mil.nga.giat.mage.sdk.datastore.location;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import mil.nga.giat.mage.sdk.datastore.DaoHelper;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.event.IEventDispatcher;
import mil.nga.giat.mage.sdk.event.ILocationEventListener;
import mil.nga.giat.mage.sdk.exceptions.LocationException;
import mil.nga.giat.mage.sdk.exceptions.UserException;

/**
 * A utility class for accessing {@link Location} data from the physical data
 * model. The details of ORM DAOs and Lazy Loading should not be exposed past
 * this class.
 * 
 * @author wiedemanns
 * 
 */
public class LocationHelper extends DaoHelper<Location> implements IEventDispatcher<ILocationEventListener> {

	private static final String LOG_NAME = LocationHelper.class.getName();

	private final Dao<Location, Long> locationDao;
	private final Dao<LocationProperty, Long> locationPropertyDao;
	
	private Collection<ILocationEventListener> listeners = new CopyOnWriteArrayList<ILocationEventListener>();

	private Context context;
	
	/**
	 * Singleton.
	 */
	private static LocationHelper mLocationHelper;

	/**
	 * Use of a Singleton here ensures that an excessive amount of DAOs are not
	 * created.
	 * 
	 * @param context
	 *            Application Context
	 * @return A fully constructed and operational LocationHelper.
	 */
	public static LocationHelper getInstance(Context context) {
		if (mLocationHelper == null) {
			mLocationHelper = new LocationHelper(context);
		}
		return mLocationHelper;
	}

	/**
	 * Only one-per JVM. Singleton.
	 * 
	 * @param context
	 */
	private LocationHelper(Context context) {
		super(context);
		this.context = context;
				
		try {
			locationDao = daoStore.getLocationDao();
			locationPropertyDao = daoStore.getLocationPropertyDao();
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to communicate with Location database.", sqle);

			throw new IllegalStateException("Unable to communicate with Location database.", sqle);
		}

	}

	@Override
	public Location create(final Location pLocation) throws LocationException {
		Location createdLocation;

		try {
			createdLocation = TransactionManager.callInTransaction(DaoStore.getInstance(context).getConnectionSource(), new Callable<Location>() {
				@Override
				public Location call() throws Exception {
					// create Location geometry.
					Location createdLocation = locationDao.createIfNotExists(pLocation);
					// create Location properties.
					Collection<LocationProperty> locationProperties = pLocation.getProperties();
					if (locationProperties != null) {
						for (LocationProperty locationProperty : locationProperties) {
							locationProperty.setLocation(createdLocation);
							locationPropertyDao.create(locationProperty);
						}
					}
					for (ILocationEventListener listener : listeners) {
						listener.onLocationCreated(Collections.singletonList(createdLocation));
					}
					return createdLocation;
				}
			});
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "There was a problem creating the location: " + pLocation + ".", sqle);
			throw new LocationException("There was a problem creating the location: " + pLocation + ".", sqle);
		}

		return createdLocation;
	}

	@Override
	public Location read(Long id) throws LocationException {
		try {
			return locationDao.queryForId(id);
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to query for existence for id = '" + id + "'", sqle);
			throw new LocationException("Unable to query for existence for id = '" + id + "'", sqle);
		}
	}
	
    @Override
    public Location read(String pRemoteId) throws LocationException {
        Location location = null;
        try {
            List<Location> results = locationDao.queryBuilder().where().eq("remote_id", pRemoteId).query();
            if (results != null && results.size() > 0) {
                location = results.get(0);
            }
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "Unable to query for existence for remote_id = '" + pRemoteId + "'", sqle);
            throw new LocationException("Unable to query for existence for remote_id = '" + pRemoteId + "'", sqle);
        }

        return location;
    }
    
    /**
	 * We have to realign all the foreign ids so the update works correctly
	 * 
	 * @param location
	 * @throws LocationException
	 */
	public Location update(final Location location) throws LocationException {
		// set all the ids as needed
		final Location pOldLocation = read(location.getId());

		// do the update
		try {
			TransactionManager.callInTransaction(DaoStore.getInstance(context).getConnectionSource(), new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					
					location.setId(pOldLocation.getId());

					// FIXME : make this run faster?
					for (LocationProperty lp : location.getProperties()) {
						for (LocationProperty olp : pOldLocation.getProperties()) {
							if (lp.getKey().equalsIgnoreCase(olp.getKey())) {
								lp.setId(olp.getId());
								break;
							}
						}
					}
					
					locationDao.update(location);
		
					Collection<LocationProperty> properties = location.getProperties();
					if (properties != null) {
						for (LocationProperty property : properties) {
							property.setLocation(location);
							locationPropertyDao.createOrUpdate(property);
						}
					}
					return null;
				}
			});
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "There was a problem updating the location: " + location + ".", sqle);
			throw new LocationException("There was a problem updating the location: " + location + ".", sqle);
		}
		
		// fire the event
		for (ILocationEventListener listener : listeners) {
			listener.onLocationUpdated(location);
		}
		
		return location;
	}

	/**
	 * Light-weight query for testing the existence of a location in the local data-store.
	 * @param location The primary key of the passed in Location object is used for the query.
	 * @return
	 */
	public Boolean exists(Location location) {
		
		Boolean exists = Boolean.FALSE;		
		try {
			List<Location> locations = 
					locationDao.queryBuilder().selectColumns("_id").limit(1L).where().eq("_id", location.getId()).query();
			if(locations != null && locations.size() > 0) {
				exists = Boolean.TRUE;
			}
		} 
		catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to query for existence for location = '" + location.getId() + "'", sqle);
		}
		
		return exists;
	}

	public List<Location> getCurrentUserLocations(Context context, long limit, boolean includeRemote) {
		List<Location> locations = new ArrayList<Location>();
		User currentUser = null;
		try {
			currentUser = UserHelper.getInstance(context.getApplicationContext()).readCurrentUser();
		} catch (UserException e) {
			e.printStackTrace();
		}
		if (currentUser != null) {
			locations = getUserLocations(currentUser.getId(), context, limit, includeRemote);
		}
		return locations;
	}
	
	public List<Location> getUserLocations(Long userId, Context context, long limit, boolean includeRemote) {
		List<Location> locations = new ArrayList<Location>();
		QueryBuilder<Location, Long> queryBuilder = locationDao.queryBuilder();
		try {
			if (limit > 0) {
				queryBuilder.limit(limit);
				// most recent first!
				queryBuilder.orderBy("timestamp", false);
			}
			Where<Location, Long> where = queryBuilder.where().eq("user_id", userId);
			if(!includeRemote) {
				where.and().isNull("remote_id");
			}
			locations = locationDao.query(queryBuilder.prepare());
		} catch (SQLException e) {
			Log.e(LOG_NAME, "Could not get current users Locations.");
		}
		return locations;
	}
	
	/**
	 * This will delete the user's location(s) that have remote_ids. Locations
	 * that do NOT have remote_ids have not been sync'ed w/ the server.
	 *
	 * @param userLocalId
	 *            The user's local id
	 * @throws LocationException
	 */
	public int deleteUserLocations(String userLocalId, Boolean keepMostRecent, Event event) throws LocationException {

		int numberLocationsDeleted = 0;

		try {
			// newest first
			QueryBuilder<Location, Long> qb = locationDao.queryBuilder().orderBy("timestamp", false);
			qb.where().eq("user_id", userLocalId).and().eq("event_id", event.getId());

			List<Location> locations = qb.query();

			// if we should keep the most recent record, then skip one record.
			if(keepMostRecent) {
				locations.remove(0);
			}

			delete(locations);
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to delete user's locations", sqle);
			throw new LocationException("Unable to delete user's locations", sqle);
		}
		return numberLocationsDeleted;
	}

	/**
	 * This will delete all locations for an event.
	 *
	 * @param event
	 *            The event to remove locations for
	 * @throws LocationException
	 */
	public void deleteLocations(Event event) throws LocationException {
		Log.e(LOG_NAME, "Deleting locations for event "  + event.getName());

		try {
			QueryBuilder<Location, Long> qb = locationDao.queryBuilder();
			qb.where().eq("event_id", event.getId());
			List<Location> locations = qb.query();
			delete(locations);
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to delete locations for an event", sqle);
			throw new LocationException("Unable to delete locations for an event", sqle);
		}
	}

	/**
	 * Deletes locations. This will also delete a Location's child
	 * Properties and Geometry data.
	 * 
	 * @param locations
	 * @throws LocationException
	 */
	public void delete(final Collection<Location> locations) throws LocationException {
		List<Location> deletedLocations = new ArrayList<Location>();
		try {
			deletedLocations = TransactionManager.callInTransaction(DaoStore.getInstance(context).getConnectionSource(), new Callable<List<Location>>() {
				@Override
				public List<Location> call() throws Exception {
					// read the full Location in
					List<Location> deletedLocations = new ArrayList<Location>();
					for(Location location : locations) {
						// delete Location properties.
						Collection<LocationProperty> properties = location.getProperties();
						if (properties != null) {
							for (LocationProperty property : properties) {
								locationPropertyDao.deleteById(property.getId());
							}
						}

						// finally, delete the Location.
						locationDao.deleteById(location.getId());
						deletedLocations.add(location);
					}
					return deletedLocations;
				}
			});
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to delete Location: " + Arrays.toString(locations.toArray()), sqle);
			throw new LocationException("Unable to delete Location: " + Arrays.toString(locations.toArray()), sqle);
		} finally {
			for (ILocationEventListener listener : listeners) {
				listener.onLocationDeleted(deletedLocations);
			}
		}
	}
	
	public void deleteAll() throws UserException {

		try {
			DeleteBuilder<Location, Long> db = locationDao.deleteBuilder();
			db.delete();
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "There was a problem deleting locations.", sqle);
			throw new UserException("There was a problem deleting locations.", sqle);
		}
	}
	
	@Override
	public boolean addListener(final ILocationEventListener listener) {
		return listeners.add(listener);
	}

	@Override
	public boolean removeListener(ILocationEventListener listener) {
		return listeners.remove(listener);
	}
}
