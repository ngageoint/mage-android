package mil.nga.giat.mage.sdk.datastore.observation;

import android.content.Context;
import android.util.Log;

import com.google.common.collect.Sets;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

import mil.nga.giat.mage.sdk.datastore.DaoHelper;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.event.IEventDispatcher;
import mil.nga.giat.mage.sdk.event.IObservationEventListener;
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
	private final Dao<ObservationForm, Long> observationFormDao;
	private final Dao<ObservationProperty, Long> observationPropertyDao;
	private final Dao<ObservationImportant, Long> observationImportantDao;
	private final Dao<ObservationFavorite, Long> observationFavoriteDao;

	private Collection<IObservationEventListener> listeners = new CopyOnWriteArrayList<>();
	
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
			observationFormDao = daoStore.getObservationFormDao();
			observationPropertyDao = daoStore.getObservationPropertyDao();
			observationImportantDao = daoStore.getObservationImportantDao();
			observationFavoriteDao = daoStore.getObservationFavoriteDao();
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to communicate with Observation database.", sqle);

			throw new IllegalStateException("Unable to communicate with Observation database.", sqle);
		}

	}

	@Override
	public Observation create(Observation observation) throws ObservationException {
		return create(observation, true);
	}

	public Observation create(final Observation observation, final Boolean sendUserNotifcations) throws ObservationException {
		Observation savedObservation = null;
		try {
			 savedObservation =  observationDao.callBatchTasks(new Callable<Observation>() {
                @Override
                public Observation call() throws Exception {
					Observation createdObservation;

					// Now we try and create the Observation structure.
					try {
						// set last Modified
						if (observation.getLastModified() == null) {
							observation.setLastModified(new Date());
						}

						// create the Observation.
						observationDao.create(observation);

						Collection<ObservationForm> forms = observation.getForms();
						if (forms != null) {
							for (ObservationForm form : forms) {
								form.setObservation(observation);
								observationFormDao.create(form);

								// create Observation properties.
								Collection<ObservationProperty> properties = form.getProperties();
								if (properties != null) {
									for (ObservationProperty property : properties) {
										property.setObservationForm(form);
										observationPropertyDao.create(property);
									}
								}
							}
						}

						// create Observation favorites.
						Collection<ObservationFavorite> favorites = observation.getFavorites();
						if (favorites != null) {
							for (ObservationFavorite favorite : favorites) {
								favorite.setObservation(observation);
								observationFavoriteDao.create(favorite);
							}
						}

						// create Observation attachments.
						Collection<Attachment> attachments = observation.getAttachments();
						for (Attachment attachment : attachments) {
							try {
								attachment.setObservation(observation);
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
						listener.onObservationCreated(Collections.singletonList(observation), sendUserNotifcations);
					}

					return observation;
                }
            });

		} catch (Exception e) {
			e.printStackTrace();
		}

		return savedObservation;
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

	public ObservationFavorite readFavorite(Long id) throws ObservationException {
		try {
			return observationFavoriteDao.queryForId(id);
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to query for existence for id = '" + id + "'", sqle);
			throw new ObservationException("Unable to query for existence for id = '" + id + "'", sqle);
		}
	}

	/**
	 * We have to realign all the foreign ids so the update works correctly
	 * 
	 * @param observation
	 * @throws ObservationException
	 */
	@Override
	public Observation update(final Observation observation) throws ObservationException {
		Log.i(LOG_NAME, "Updating observation w/ id: " + observation.getId());
		Observation updatedObservation = null;
		try {
			updatedObservation = observationDao.callBatchTasks(new Callable<Observation>() {
                @Override
                public Observation call() throws Exception {
					// set all the ids as needed
					Observation oldObservation = read(observation.getId());

					Log.i(LOG_NAME, "Old Observation attachments " + oldObservation.getAttachments().size());

					// if the observation is dirty, set the last_modified date!
					// FIXME this is a server property and should not be set by the client,
					// investigate why we are setting this
					if (observation.isDirty()) {
						observation.setLastModified(new Date());
					}

					ObservationImportant important = observation.getImportant();
					ObservationImportant oldImportant = oldObservation.getImportant();
					if (oldImportant != null && oldImportant.isDirty()) {
						observation.setImportant(oldImportant);
					} else {
						if (important != null) {
							if (oldImportant != null) {
								important.setId(oldImportant.getId());
							}
							observationImportantDao.createOrUpdate(important);
						} else {
							if (oldImportant != null) {
								observationImportantDao.deleteById(oldImportant.getId());
							}
						}
					}

					observationDao.update(observation);

					Map<Long, ObservationForm> forms = observation.getFormsMap();
					Map<Long, ObservationForm> oldForms = oldObservation.getFormsMap();
					Collection<Long> commonForms = Sets.intersection(forms.keySet(), oldForms.keySet());

					// Map database ids from old forms to new forms
					for (Long formId : commonForms) {
						forms.get(formId).setId(oldForms.get(formId).getId());
					}

					for (ObservationForm form : forms.values()) {
						form.setObservation(observation);
						observationFormDao.createOrUpdate(form);

						Map<String, ObservationProperty> properties = form.getPropertiesMap();
						ObservationForm oldForm = oldForms.get(form.getFormId());
						if (oldForm != null) {
							Map<String, ObservationProperty> oldProperties = oldForm.getPropertiesMap();
							Collection<String> commonProperties = Sets.intersection(properties.keySet(), oldProperties.keySet());

							// Map database ids from old properties to new properties
							for (String propertyKey : commonProperties) {
								properties.get(propertyKey).setId(oldProperties.get(propertyKey).getId());
							}

							// Remove any properties that existed in the old form but do not exist
							// in the new form.
							for (String property : Sets.difference(oldProperties.keySet(), properties.keySet())) {
								observationPropertyDao.deleteById(oldProperties.get(property).getId());
							}
						}

						for (ObservationProperty property : properties.values()) {
							property.setObservationForm(form);
							observationPropertyDao.createOrUpdate(property);
						}
					}

					// Remove any forms that existed in the old observation but do not exist
					// in the new observation.
					for (Long formId : Sets.difference(oldForms.keySet(), forms.keySet())) {
						observationFormDao.deleteById(oldForms.get(formId).getId());
					}

					Map<String, ObservationFavorite> favorites = observation.getFavoritesMap();
					Map<String, ObservationFavorite> oldFavorites = oldObservation.getFavoritesMap();
					Collection<String> commonFavorites = Sets.intersection(favorites.keySet(), oldFavorites.keySet());

					// Map database ids from old properties to new properties
					for (String favoriteKey : commonFavorites) {
						favorites.get(favoriteKey).setId(oldFavorites.get(favoriteKey).getId());
					}

					for (ObservationFavorite favorite : favorites.values()) {
						ObservationFavorite oldFavorite = oldFavorites.get(favorite.getUserId());
						// only update favorite if local is not dirty
						if (oldFavorite == null || !oldFavorite.isDirty()) {
							favorite.setObservation(observation);
							observationFavoriteDao.createOrUpdate(favorite);
						}
					}

					// Remove any favorites that existed in the old observation but do not exist
					// in the new observation.
					for (String favorite : Sets.difference(oldFavorites.keySet(), favorites.keySet())) {
						// Only delete favorites that are not dirty
						if (!oldFavorites.get(favorite).isDirty()) {
							observationFavoriteDao.deleteById(oldFavorites.get(favorite).getId());
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

					return observation;
				}
            });
		} catch (Exception e) {
			Log.e(LOG_NAME, "There was a problem updating the observation: " + observation + ".", e);
			throw new ObservationException("There was a problem updating the observation: " + observation + ".", e);
		}

		// fire the event
		for (IObservationEventListener listener : listeners) {
			listener.onObservationUpdated(updatedObservation);
		}

		return updatedObservation;
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
		List<Observation> observations = new ArrayList<>();

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
	public void delete(final Observation observation) throws ObservationException {
		try {
			observationDao.callBatchTasks(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					// delete Observation forms.
					Collection<ObservationForm> forms = observation.getForms();
					if (forms != null) {
						for (ObservationForm form : forms) {
							// delete Observation properties.
							Collection<ObservationProperty> properties = form.getProperties();
							if (properties != null) {
								for (ObservationProperty property : properties) {
									observationPropertyDao.deleteById(property.getId());
								}
							}

							observationFormDao.deleteById(form.getId());
						}
					}

					// delete Observation favorites.
					Collection<ObservationFavorite> favorites = observation.getFavorites();
					if (favorites != null) {
						for (ObservationFavorite favorite : favorites) {
							observationFavoriteDao.deleteById(favorite.getId());
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

					// delete important
					ObservationImportant important = observation.getImportant();
					if (important != null) {
						observationImportantDao.deleteById(important.getId());
					}

					// finally, delete the Observation.
					observationDao.deleteById(observation.getId());

					for (IObservationEventListener listener : listeners) {
						listener.onObservationDeleted(observation);
					}

					return null;
				}
			});
		} catch (Exception e) {
			Log.e(LOG_NAME, "Unable to delete Observation: " + observation.getId(), e);
			throw new ObservationException("Unable to delete Observation: " + observation.getId(), e);
		}
	}

	/**
	 * This will delete all observations for an event.
	 *
	 * @param event
	 *            The event to remove locations for
	 * @throws ObservationException
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
			Log.e(LOG_NAME, "Unable to delete observations for an event", sqle);
			throw new ObservationException("Unable to delete observations for an event", sqle);
		}
	}

	/**
	 * This will mark the  observation as important
	 *
	 * @param observation The observation to mark as important
	 *
	 * @throws ObservationException
	 */
	public void addImportant(Observation observation) throws ObservationException {
		ObservationImportant important = observation.getImportant();
		important.setImportant(true);
		important.setDirty(true);
		try {
			observationImportantDao.createOrUpdate(important);
			observationDao.update(observation);

			// fire the event
			for (IObservationEventListener listener : listeners) {
				listener.onObservationUpdated(observation);
			}
		} catch (SQLException e) {
			Log.e(LOG_NAME, "Unable to favorite observation", e);
			throw new ObservationException("Unable to favorite observation", e);
		}
	}

	/**
	 * This will remove the important mark from an observation.
	 *
	 * @param observation The observation to unfavorite
	 *
	 * @throws ObservationException
	 */
	public void removeImportant(Observation observation) throws ObservationException {
		try {
			Collection<ObservationImportant> importants = observationImportantDao.queryForAll();
			Log.i(LOG_NAME, "foo");
		} catch (SQLException e) {
			e.printStackTrace();
		}

		ObservationImportant important = observation.getImportant();
		if (important != null) {
			important.setImportant(false);
			important.setDirty(true);
			try {
				observationImportantDao.update(important);
				observationDao.refresh(observation);

				// fire the event
				for (IObservationEventListener listener : listeners) {
					listener.onObservationUpdated(observation);
				}
			} catch (SQLException e) {
				Log.e(LOG_NAME, "Unable to unfavorite observation", e);
				throw new ObservationException("Unable to unfavorite observation", e);
			}
		}
	}

	public void updateImportant(ObservationImportant important, Observation observation) throws ObservationException {
		try {
			if (important.isImportant()) {
				important.setDirty(Boolean.FALSE);
				observation.setImportant(important);
				observationImportantDao.update(important);
			} else {
				observationImportantDao.delete(important);
			}

			// Update the observation so that the lastModified time is updated
			observationDao.update(observation);
			observationDao.refresh(observation);

			for (IObservationEventListener listener : listeners) {
				listener.onObservationUpdated(observation);
			}
		} catch (SQLException e) {
			Log.e(LOG_NAME, "Unable to update observation favorite", e);
			throw new ObservationException("Unable to update observation favorite", e);
		}
	}

	/**
	 * This will favorite and observation for the user.
	 *
	 * @param observation The observation to favorite
	 * @param user The user that is favoriting the observation
	 *
	 * @throws ObservationException
	 */
	public void favoriteObservation(Observation observation, User user) throws ObservationException {
		Map<String, ObservationFavorite> favoritesMap = observation.getFavoritesMap();
		ObservationFavorite favorite = favoritesMap.get(user.getRemoteId());
		if (favorite == null) {
			favorite = new ObservationFavorite(user.getRemoteId(), true);
		}

		favorite.setObservation(observation);
		favorite.setFavorite(true);
		favorite.setDirty(true);
		try {
			observationFavoriteDao.createOrUpdate(favorite);
			observationDao.refresh(observation);

			// fire the event
			for (IObservationEventListener listener : listeners) {
				listener.onObservationUpdated(favorite.getObservation());
			}
		} catch (SQLException e) {
			Log.e(LOG_NAME, "Unable to favorite observation", e);
			throw new ObservationException("Unable to favorite observation", e);
		}
	}

	/**
	 * This will unfavorite and observation for the user.
	 *
	 * @param observation The observation to unfavorite
	 * @param user The user that is unfavoriting the observation
	 *
	 * @throws ObservationException
	 */
	public void unfavoriteObservation(Observation observation, User user) throws ObservationException {
		Map<String, ObservationFavorite> favoritesMap = observation.getFavoritesMap();
		ObservationFavorite favorite = favoritesMap.get(user.getRemoteId());
		if (favorite != null) {
			favorite.setFavorite(false);
			favorite.setDirty(true);
			try {
				observationFavoriteDao.update(favorite);
				observationDao.refresh(observation);

				// fire the event
				for (IObservationEventListener listener : listeners) {
					listener.onObservationUpdated(favorite.getObservation());
				}
			} catch (SQLException e) {
				Log.e(LOG_NAME, "Unable to unfavorite observation", e);
				throw new ObservationException("Unable to unfavorite observation", e);
			}
		}
	}

	public void updateFavorite(ObservationFavorite favorite) throws ObservationException {
		try {
			Observation observation = favorite.getObservation();

			if (favorite.isFavorite()) {
				favorite.setDirty(Boolean.FALSE);
				observationFavoriteDao.update(favorite);
			} else {
				observationFavoriteDao.delete(favorite);
			}

			// Update the observation so that the lastModified time is updated
			observationDao.update(observation);
			observationDao.refresh(observation);

			for (IObservationEventListener listener : listeners) {
				listener.onObservationUpdated(observation);
			}
		} catch (SQLException e) {
			Log.e(LOG_NAME, "Unable to update observation favorite", e);
			throw new ObservationException("Unable to update observation favorite", e);
		}
	}

	/**
	 * A List of {@link ObservationImportant} from the datastore that are dirty (i.e.
	 * should be synced with the server).
	 *
	 * @return
	 */
	public List<Observation> getDirtyImportant() throws ObservationException {
		try {
			QueryBuilder<ObservationImportant, Long> importantQb = observationImportantDao.queryBuilder();
			importantQb.where().eq("dirty", true);

			QueryBuilder<Observation, Long> observationQb = observationDao.queryBuilder();
			return observationQb.join(importantQb).query();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			Log.e(LOG_NAME, "Unable to get dirty observation favorites", e);
			throw new ObservationException("Unable to get dirty observation favorites", e);
		}
	}

	/**
	 * A List of {@link ObservationProperty} from the datastore that are dirty (i.e.
	 * should be synced with the server).
	 *
	 * @return
	 */
	public List<ObservationFavorite> getDirtyFavorites() throws ObservationException {
		try {
			QueryBuilder<ObservationFavorite, Long> queryBuilder = observationFavoriteDao.queryBuilder();
			queryBuilder.where().eq("dirty", true);

			return observationFavoriteDao.query(queryBuilder.prepare());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			Log.e(LOG_NAME, "Unable to get dirty observation favorites", e);
			throw new ObservationException("Unable to get dirty observation favorites", e);
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
