package mil.nga.giat.mage.sdk.datastore.observation;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

import mil.nga.giat.mage.sdk.datastore.DaoHelper;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.event.IEventDispatcher;
import mil.nga.giat.mage.sdk.event.IObservationEventListener;
import mil.nga.giat.mage.sdk.exceptions.LocationException;
import mil.nga.giat.mage.sdk.exceptions.ObservationException;
import mil.nga.giat.mage.sdk.exceptions.UserException;

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
	private final Dao<ObservationProperty, Long> observationPropertyDao;

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
			observationPropertyDao = daoStore.getObservationPropertyDao();
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to communicate with Observation database.", sqle);

			throw new IllegalStateException("Unable to communicate with Observation database.", sqle);
		}

	}

	@Override
	public Observation create(Observation observation) throws ObservationException {
		return create(observation, true);
	}

	public Observation create(Observation observation, Boolean sendUserNotifcations) throws ObservationException {

		Observation createdObservation;

		// Now we try and create the Observation structure.
		try {
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
			for (Attachment attachment : attachments) {
				try {
					attachment.setObservation(createdObservation);
					AttachmentHelper.getInstance(mApplicationContext).create(attachment);
				} catch (Exception e) {
					throw new ObservationException("There was a problem creating the observations attachment: " + attachment + ".", e);
				}
			}

		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "There was a problem creating the observation: " + observation + ".", sqle);
			throw new ObservationException("There was a problem creating the observation: " + observation + ".", sqle);
		}

		// fire the event
		for (IObservationEventListener listener : listeners) {
			listener.onObservationCreated(Collections.singletonList(createdObservation), sendUserNotifcations);
		}

		return createdObservation;
	}

	@Override
	public Observation read(Long id) throws ObservationException {
		try {
		    return observationDao.queryForId(id);
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to query for existence for id = '" + id + "'", sqle);
			throw new ObservationException("Unable to query for existence for id = '" + id + "'", sqle);
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
            Log.e(LOG_NAME, "Unable to query for existence for remote_id = '" + pRemoteId + "'", sqle);
            throw new ObservationException("Unable to query for existence for remote_id = '" + pRemoteId + "'", sqle);
        }

        return observation;
    }

	/**
	 * We have to realign all the foreign ids so the update works correctly
	 * 
	 * @param observation
	 * @throws ObservationException
	 */
	@Override
	public Observation update(Observation observation) throws ObservationException {
		Log.i(LOG_NAME, "Updating observation w/ id: " + observation.getId());

		// set all the ids as needed
	    Observation oldObservation = read(observation.getId());

		Log.i(LOG_NAME, "Old Observation attachments " + oldObservation.getAttachments().size());

		// if the observation is dirty, set the last_modified date!
		if (observation.isDirty()) {
			observation.setLastModified(new Date());
		}

//		for (Attachment oa : oldObservation.getAttachments()) {
//			if (oa.getRemoteId() == null) {
//				observation.getAttachments().add(oa);
//			} else {
//				boolean contains = false;
//				for (Attachment a : observation.getAttachments()) {
//					if (oa.getRemoteId().equals(a.getRemoteId())) {
//						contains = true;
//						break;
//					}
//				}
//
//				if (!contains) {
//					observation.getAttachments().remove(oa);
//					try {
//						AttachmentHelper.getInstance(mApplicationContext).delete(oa);
//					} catch (SQLException e) {
//						throw new ObservationException("There was a problem deleting the observations attachment: " + oa + ".", e);
//					}
//				}
//			}
//		}

		// do the update
		try {
			observationDao.update(observation);

			// FIXME : make this run faster?
			for (ObservationProperty op : observation.getProperties()) {
				for (ObservationProperty oop : oldObservation.getProperties()) {
					if (op.getKey().equalsIgnoreCase(oop.getKey())) {
						op.setId(oop.getId());
						break;
					}
				}
			}

			Collection<ObservationProperty> properties = observation.getProperties();
			if (properties != null) {
				for (ObservationProperty property : properties) {
					property.setObservation(observation);
					observationPropertyDao.createOrUpdate(property);
				}
			}

			Log.i(LOG_NAME, "Observation attachments " + observation.getAttachments().size());


			// FIXME : make this run faster?
			for (Attachment a : observation.getAttachments()) {
				for (Attachment oa : oldObservation.getAttachments()) {
					if (a.getRemoteId() != null && a.getRemoteId().equalsIgnoreCase(oa.getRemoteId())) {
						a.setId(oa.getId());
						break;
					}
				}
			}

			for (Attachment attachment : observation.getAttachments()) {
				try {
					attachment.setObservation(observation);
					AttachmentHelper.getInstance(mApplicationContext).create(attachment);
				} catch (Exception e) {
					throw new ObservationException("There was a problem creating/updating the observations attachment: " + attachment + ".", e);
				}
			}

			observationDao.refresh(observation);

			if (observation.getRemoteId() != null) {
				for (Attachment attachment : observation.getAttachments()) {
					if (attachment.isDirty()) {
						AttachmentHelper.getInstance(mApplicationContext).uploadableAttachment(attachment);
					}
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
	public Date getLatestCleanLastModified(Context context, Event currentEvent) {
		Date lastModifiedDate = new Date(0);
		QueryBuilder<Observation, Long> queryBuilder = observationDao.queryBuilder();

		try {
			User currentUser = UserHelper.getInstance(context.getApplicationContext()).readCurrentUser();
			if (currentUser != null) {
				queryBuilder.where().eq("dirty", Boolean.FALSE).and().ne("user_id", String.valueOf(currentUser.getRemoteId())).and().eq("event_id", currentEvent.getId());
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
	 * Deletes an Observation. This will also delete an Observation's child
	 * Attachments, child Properties and Geometry data.
	 * 
	 * @param observation
	 * @throws ObservationException
	 */
	public void delete(Observation observation) throws ObservationException {
		try {
			// delete Observation properties.
			Collection<ObservationProperty> properties = observation.getProperties();
			if (properties != null) {
				for (ObservationProperty property : properties) {
					observationPropertyDao.deleteById(property.getId());
				}
			}

			// delete Observation attachments.
			Collection<Attachment> attachments = observation.getAttachments();
			if (attachments != null) {
				AttachmentHelper attachmentHelper = AttachmentHelper.getInstance(mApplicationContext);
				for (Attachment attachment : attachments) {
					attachmentHelper.delete(attachment);
				}
			}

			// finally, delete the Observation.
			observationDao.deleteById(observation.getId());
			
			for (IObservationEventListener listener : listeners) {
				listener.onObservationDeleted(observation);
			}
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to delete Observation: " + observation.getId(), sqle);
			throw new ObservationException("Unable to delete Observation: " + observation.getId(), sqle);
		}
	}

	/**
	 * This will delete all observations for an event.
	 *
	 * @param event
	 *            The event to remove locations for
	 * @throws LocationException
	 */
	public void deleteObservations(Event event) throws ObservationException {
		Log.e(LOG_NAME, "Deleting observations for event "  + event.getName());


		try {
			QueryBuilder<Observation, Long> qb = observationDao.queryBuilder();
			qb.where().eq("event_id", event.getId());
			for (Observation observation : qb.query()) {
				delete(observation);
			}
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to delete locations for an event", sqle);
			throw new ObservationException("Unable to delete observations for an event", sqle);
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
