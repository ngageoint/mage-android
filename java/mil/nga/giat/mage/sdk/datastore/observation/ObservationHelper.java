package mil.nga.giat.mage.sdk.datastore.observation;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

import mil.nga.giat.mage.sdk.datastore.DaoHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.event.IEventDispatcher;
import mil.nga.giat.mage.sdk.event.IObservationEventListener;
import mil.nga.giat.mage.sdk.exceptions.ObservationException;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;

/**
 * A utility class for accessing {@link Observation} data from the physical data
 * model. The details of ORM DAOs and Lazy Loading should not be exposed past
 * this class.
 * 
 * @author travis
 * 
 */
public class ObservationHelper extends DaoHelper<Observation> implements IEventDispatcher<IObservationEventListener> {

	private static final String LOG_NAME = ObservationHelper.class.getName();

	private final Dao<Observation, Long> observationDao;
	private final Dao<ObservationGeometry, Long> observationGeometryDao;
	private final Dao<ObservationProperty, Long> observationPropertyDao;
	private final Dao<Attachment, Long> attachmentDao;

	private Collection<IObservationEventListener> listeners = new CopyOnWriteArrayList<IObservationEventListener>();
	
	/**
	 * Singleton.
	 */
	private static ObservationHelper mObservationHelper;

	/**
	 * Use of a Singleton here ensures that an excessive amount of DAOs are not
	 * created.
	 * 
	 * @param context
	 *            Application Context
	 * @return A fully constructed and operational ObservationHelper.
	 */
	public static ObservationHelper getInstance(Context context) {
		if (mObservationHelper == null) {
			mObservationHelper = new ObservationHelper(context);
		}
		return mObservationHelper;
	}

	/**
	 * Only one-per JVM. Singleton.
	 * 
	 * @param pContext
	 */
	private ObservationHelper(Context pContext) {
		super(pContext);
		try {
			// Set up DAOs
			observationDao = daoStore.getObservationDao();
			observationGeometryDao = daoStore.getObservationGeometryDao();
			observationPropertyDao = daoStore.getObservationPropertyDao();
			attachmentDao = daoStore.getAttachmentDao();
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to communicate with Observation database.", sqle);

			throw new IllegalStateException("Unable to communicate with Observation database.", sqle);
		}

	}

	@Override
	public Observation create(Observation observation) throws ObservationException {

		Observation createdObservation;

		// Now we try and create the Observation structure.
		try {

			// create Observation geometry.
			observationGeometryDao.create(observation.getObservationGeometry());

			// set last Modified
			if (observation.getLastModified() == null) {
				observation.setLastModified(new Date());
			}
			// create the Observation.
			createdObservation = observationDao.createIfNotExists(observation);

			// create Observation properties.
			Collection<ObservationProperty> properties = observation.getProperties();
			if (properties != null) {
				for (ObservationProperty property : properties) {
					property.setObservation(createdObservation);
					observationPropertyDao.create(property);
				}
			}

			// create Observation attachments.
			Collection<Attachment> attachments = observation.getAttachments();
			if (attachments != null) {
				for (Attachment attachment : attachments) {
					attachment.setObservation(createdObservation);
					attachmentDao.create(attachment);
				}
			}

		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "There was a problem creating the observation: " + observation + ".", sqle);
			throw new ObservationException("There was a problem creating the observation: " + observation + ".", sqle);
		}
		
		// fire the event
		for (IObservationEventListener listener : listeners) {
			listener.onObservationCreated(Collections.singletonList(createdObservation));
		}
		
		return createdObservation;
	}

	@Override
	public Observation read(Long id) throws ObservationException {
		try {
		    return observationDao.queryForId(id);
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to query for existance for id = '" + id + "'", sqle);
			throw new ObservationException("Unable to query for existance for id = '" + id + "'", sqle);
		}
	}
	
    @Override
    public Observation read(String pRemoteId) throws ObservationException {
        Observation observation = null;
        try {
            List<Observation> results = observationDao.queryBuilder().where().eq("remote_id", pRemoteId).query();
            if (results != null && results.size() > 0) {
                observation = results.get(0);
            }
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "Unable to query for existance for remote_id = '" + pRemoteId + "'", sqle);
            throw new ObservationException("Unable to query for existance for remote_id = '" + pRemoteId + "'", sqle);
        }

        return observation;
    }

	/**
	 * We have to realign all the foreign ids so the update works correctly
	 * 
	 * @param observation
	 * @throws ObservationException
	 */
	public Observation update(Observation observation) throws ObservationException {
		// set all the ids as needed
	    Observation pOldObservation = read(observation.getId());
	    
		observation.setId(pOldObservation.getId());

		if (observation.getObservationGeometry() != null && pOldObservation.getObservationGeometry() != null) {
			observation.getObservationGeometry().setPk_id(pOldObservation.getObservationGeometry().getPk_id());
		}

		// FIXME : make this run faster?
		for (ObservationProperty op : observation.getProperties()) {
			for (ObservationProperty oop : pOldObservation.getProperties()) {
				if (op.getKey().equalsIgnoreCase(oop.getKey())) {
					op.setPk_id(oop.getPk_id());
					break;
				}
			}
		}

		// FIXME : make this run faster?
		for (Attachment a : observation.getAttachments()) {
			for (Attachment oa : pOldObservation.getAttachments()) {
				if (a.getRemoteId() != null && a.getRemoteId().equalsIgnoreCase(oa.getRemoteId())) {
					a.setId(oa.getId());
					break;
				}
			}
		}

		// do the update
		try {
			observationGeometryDao.update(observation.getObservationGeometry());
			
			// if the observation is dirty, set the last_modified date!
			if(observation.isDirty()) {
				observation.setLastModified(new Date());
			}
			observationDao.update(observation);

			Collection<ObservationProperty> properties = observation.getProperties();
			if (properties != null) {
				for (ObservationProperty property : properties) {
					property.setObservation(observation);
					observationPropertyDao.createOrUpdate(property);
				}
			}

			Collection<Attachment> attachments = observation.getAttachments();
			if (attachments != null) {
				for (Attachment attachment : attachments) {
					attachment.setObservation(observation);
					attachmentDao.createOrUpdate(attachment);
				}
			}

		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "There was a problem updating the observation: " + observation + ".", sqle);
			throw new ObservationException("There was a problem updating the observation: " + observation + ".", sqle);
		}
		
		// fire the event
		for (IObservationEventListener listener : listeners) {
			listener.onObservationUpdated(observation);
		}
		
		return observation;
	}


	public Collection<Observation> readAll() throws ObservationException {
		ConcurrentSkipListSet<Observation> observations = new ConcurrentSkipListSet<Observation>();
		try {
			observations.addAll(observationDao.queryForAll());
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to read Observations", sqle);
			throw new ObservationException("Unable to read Observations.", sqle);
		}
		return observations;
	}

	/**
	 * Gets the latest last modified date.  Used when fetching.
	 * 
	 * @return
	 */
	public Date getLatestCleanLastModified(Context context) {
		Date lastModifiedDate = new Date(0);
		QueryBuilder<Observation, Long> queryBuilder = observationDao.queryBuilder();

		try {
			User currentUser = UserHelper.getInstance(context.getApplicationContext()).readCurrentUser();
			if (currentUser != null) {
				queryBuilder.where().eq("dirty", Boolean.FALSE).and().ne("user_id", String.valueOf(currentUser.getRemoteId()));
				queryBuilder.orderBy("last_modified", false);
				queryBuilder.limit(1L);
				Observation o = observationDao.queryForFirst(queryBuilder.prepare());
				if (o != null) {
					lastModifiedDate = o.getLastModified();
				}
			}
		} catch (SQLException se) {
			Log.e(LOG_NAME, "Could not get last_modified date.");
		} catch (UserException e) {
			Log.e(LOG_NAME, "Could not get current user.");
		}

		return lastModifiedDate;
	}

	/**
	 * Gets a List of Observations from the datastore that are dirty (i.e.
	 * should be synced with the server).
	 * 
	 * @return
	 */
	public List<Observation> getDirty() {
		QueryBuilder<Observation, Long> queryBuilder = observationDao.queryBuilder();
		List<Observation> observations = new ArrayList<Observation>();

		try {
			queryBuilder.where().eq("dirty", true);
			observations = observationDao.query(queryBuilder.prepare());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			Log.e(LOG_NAME, "Could not get dirty Observations.");
		}
		return observations;
	}
	
	/**
	 * A List of {@link Attachment} from the datastore that are dirty (i.e.
	 * should be synced with the server).
	 * 
	 * @return
	 */
	public List<Attachment> getDirtyAttachments() {
		QueryBuilder<Attachment, Long> queryBuilder = attachmentDao.queryBuilder();
		List<Attachment> attachments = new ArrayList<Attachment>();

		try {
			queryBuilder.where().eq("dirty", true);
			attachments = attachmentDao.query(queryBuilder.prepare());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			Log.e(LOG_NAME, "Could not get dirty Observations.");
		}
		return attachments;
	}
	
	/**
	 * Read an Attachment from the data-store
	 * 
	 * @param primaryKey
	 *            The primary key of the Attachment to read.
	 * @return A fully constructed Observation.
	 * @throws OrmException
	 *             If there was an error reading the Observation from the
	 *             database.
	 */
	public Attachment readAttachmentByPrimaryKey(Long primaryKey) throws ObservationException {
		Attachment a;
		try {
			a = attachmentDao.queryForId(primaryKey);
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to read Attachment: " + primaryKey, sqle);
			throw new ObservationException("Unable to read Attachment: " + primaryKey, sqle);
		}
		return a;
	}
	
	/**
	 * Deletes an Observation. This will also delete an Observation's child
	 * Attachments, child Properties and Geometry data.
	 * 
	 * @param pPrimaryKey
	 * @throws OrmException
	 */
	public void delete(Long pPrimaryKey) throws ObservationException {
		try {
			// read the full Observation in
			Observation observation = observationDao.queryForId(pPrimaryKey);

			// delete Observation properties.
			Collection<ObservationProperty> properties = observation.getProperties();
			if (properties != null) {
				for (ObservationProperty property : properties) {
					observationPropertyDao.deleteById(property.getPk_id());
				}
			}

			// delete Observation attachments.
			Collection<Attachment> attachments = observation.getAttachments();
			if (attachments != null) {
				for (Attachment attachment : attachments) {
					attachmentDao.deleteById(attachment.getId());
				}
			}

			// delete Geometry (but not corresponding GeometryType).
			observationGeometryDao.deleteById(observation.getObservationGeometry().getPk_id());

			// finally, delete the Observation.
			observationDao.deleteById(pPrimaryKey);
			
			for (IObservationEventListener listener : listeners) {
				listener.onObservationDeleted(observation);
			}
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to delete Observation: " + pPrimaryKey, sqle);
			throw new ObservationException("Unable to delete Observation: " + pPrimaryKey, sqle);
		}
	}

	@Override
	public boolean addListener(final IObservationEventListener listener) {
		return listeners.add(listener);
	}

	@Override
	public boolean removeListener(IObservationEventListener listener) {
		return listeners.remove(listener);
	}
}
