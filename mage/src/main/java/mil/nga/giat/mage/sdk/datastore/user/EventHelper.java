package mil.nga.giat.mage.sdk.datastore.user;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.DeleteBuilder;

import org.apache.commons.lang3.StringUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import mil.nga.giat.mage.sdk.datastore.DaoHelper;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.exceptions.EventException;
import mil.nga.giat.mage.sdk.exceptions.UserException;

/**
 * A utility class for accessing {@link Event} data from the physical data model.
 * The details of ORM DAOs and Lazy Loading should not be exposed past this
 * class.
 */
public class EventHelper extends DaoHelper<Event> {

    private static final String LOG_NAME = EventHelper.class.getName();

    private final Dao<Event, Long> eventDao;
    private final Dao<Form, Long> formDao;
    private final Dao<TeamEvent, Long> teamEventDao;

    /**
     * Singleton.
     */
    private static EventHelper mEventHelper;

    /**
     * Use of a Singleton here ensures that an excessive amount of DAOs are not
     * created.
     *
     * @param context
     *            Application Context
     * @return A fully constructed and operational UserHelper.
     */
    public static EventHelper getInstance(Context context) {
        if (mEventHelper == null) {
            mEventHelper = new EventHelper(context);
        }
        return mEventHelper;
    }

    /**
     * Only one-per JVM. Singleton.
     *
     * @param pContext context
     */
    private EventHelper(Context pContext) {
        super(pContext);

        try {
            eventDao = daoStore.getEventDao();
            formDao = daoStore.getFormDao();
            teamEventDao = daoStore.getTeamEventDao();
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "Unable to communicate with Event database.", sqle);

            throw new IllegalStateException("Unable to communicate with Event database.", sqle);
        }

    }

    @Override
    public Event create(Event pEvent) throws EventException {
        Event createdEvent;
        try {
            createdEvent = eventDao.createIfNotExists(pEvent);
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "There was a problem creating event: " + pEvent, sqle);
            throw new EventException("There was a problem creating event: " + pEvent, sqle);
        }
        return createdEvent;
    }

    @Override
    public Event read(Long id) throws EventException {
        try {
            return eventDao.queryForId(id);
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "Unable to query for existence for id = '" + id + "'", sqle);
            throw new EventException("Unable to query for existence for id = '" + id + "'", sqle);
        }
    }

	public List<Event> readAll() throws EventException {
		List<Event> events = new ArrayList<>();
		try {
			events.addAll(eventDao.queryForAll());
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to read Events", sqle);
			throw new EventException("Unable to read Events.", sqle);
		}
		return events;
	}

    @Override
    public Event read(String pRemoteId) throws EventException {
        Event event = null;
        try {
            List<Event> results = eventDao.queryBuilder().where().eq("remote_id", pRemoteId).query();
            if (results != null && results.size() > 0) {
                event = results.get(0);
            }
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "Unable to query for existence for remote_id = '" + pRemoteId + "'", sqle);
            throw new EventException("Unable to query for existence for remote_id = '" + pRemoteId + "'", sqle);
        }
        return event;
    }


    @Override
    public Event update(Event event) throws EventException {
        try {
            TransactionManager.callInTransaction(DaoStore.getInstance(mApplicationContext).getConnectionSource(), (Callable<Void>) () -> {
                DeleteBuilder<Form, Long> deleteBuilder = formDao.deleteBuilder();
                deleteBuilder.where().eq(Form.Companion.getColumnNameEventId(), event.getId());
                deleteBuilder.delete();

                eventDao.update(event);

                for (Form form : event.getForms()) {
                    form.event = event;
                    formDao.create(form);
                }

                return null;
            });
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "There was a problem creating event: " + event);
            throw new EventException("There was a problem creating event: " + event, sqle);
        }
		return event;
    }

    public Event createOrUpdate(Event event) {
        try {
            Event oldEvent = read(event.getRemoteId());
            if (oldEvent == null) {
                event = create(event);

                for (Form form : event.getForms()) {
                    form.event = event;
                    formDao.create(form);
                }
                Log.d(LOG_NAME, "Created event with remote_id " + event.getRemoteId());
            } else {
                event.setId(oldEvent.getId());
                update(event);
                Log.d(LOG_NAME, "Updated event with remote_id " + event.getRemoteId());
            }
        } catch (Exception e) {
            Log.e(LOG_NAME, "There was a problem reading user: " + event, e);
        }
        return event;
    }

    public Form getForm(Long formId) {
        Form form = null;
        try {
            List<Form> forms = formDao.queryBuilder()
                .where()
                .eq("formId", formId)
                .query();

            if (forms != null && forms.size() > 0) {
                form = forms.get(0);
            }
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "Error pulling form with id: " + formId, sqle);
        }

        return form;
    }

    public Event getCurrentEvent() {
        Event event = null;
        try {
            User user = UserHelper.getInstance(mApplicationContext).readCurrentUser();
            if (user != null) {
                event = user.getUserLocal().getCurrentEvent();
            } else {
				Log.d(LOG_NAME, "Current user is null.  Why?");
			}
        } catch(UserException ue) {
            Log.e(LOG_NAME, "There is no current user. ", ue);
        }

        return event;
    }

    public List<Event> getRecentEvents() throws EventException {
        List<Event> events = new ArrayList<>();
        try {
            User user = UserHelper.getInstance(mApplicationContext).readCurrentUser();
            if (user != null) {
                List<String> recentEventIds = user.getRecentEventIds();
                List<String> cases = new ArrayList<>(recentEventIds.size());
                for (int i = 0; i < recentEventIds.size(); i++) {
                    cases.add("WHEN " + recentEventIds.get(i) + " THEN " + i);
                }

                events = eventDao
                    .queryBuilder()
                    .orderByRaw(String.format("CASE %s %s END", Event.COLUMN_NAME_REMOTE_ID, StringUtils.join(cases, " ")))
                    .where()
                    .in(Event.COLUMN_NAME_REMOTE_ID, user.getRecentEventIds())
                    .query();
            } else {
                Log.d(LOG_NAME, "Current user is null.");
            }
        } catch(Exception e) {
            Log.e(LOG_NAME, "There was a problem reading users current event", e);
        }

        return events;
    }


    /**
     * Remove any events from the database that are not in this event list.
     *
     * @param remoteEvents list of events that should remain in the database, all others will be removed
     */
    public void syncEvents(Set<Event> remoteEvents) {
        try {
            List<Event> eventsToRemove = readAll();
            eventsToRemove.removeAll(remoteEvents);

            for (Event eventToRemove : eventsToRemove) {
                Log.e(LOG_NAME, "Removing event " + eventToRemove.getName());

                LocationHelper.getInstance(mApplicationContext).deleteLocations(eventToRemove);
                ObservationHelper.getInstance(mApplicationContext).deleteObservations(eventToRemove);

                DeleteBuilder<TeamEvent, Long> teamDeleteBuilder = teamEventDao.deleteBuilder();
                teamDeleteBuilder.where().eq("event_id", eventToRemove.getId());
                teamDeleteBuilder.delete();

                DeleteBuilder<Event, Long> eventDeleteBuilder = eventDao.deleteBuilder();
                eventDeleteBuilder.where().idEq(eventToRemove.getId());
                eventDeleteBuilder.delete();
            }
        } catch (Exception e) {
            Log.e(LOG_NAME, "Error deleting event ", e);
        }
    }
}
