package mil.nga.giat.mage.sdk.datastore.user;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.sdk.datastore.DaoHelper;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.exceptions.EventException;
import mil.nga.giat.mage.sdk.exceptions.UserException;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

/**
 * A utility class for accessing {@link Event} data from the physical data model.
 * The details of ORM DAOs and Lazy Loading should not be exposed past this
 * class.
 *
 * @author wiedemanns
 *
 */
public class EventHelper extends DaoHelper<Event> {

    private static final String LOG_NAME = EventHelper.class.getName();

    private final Dao<Event, Long> eventDao;
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
     * @param pContext
     */
    private EventHelper(Context pContext) {
        super(pContext);

        try {
            eventDao = daoStore.getEventDao();
            teamEventDao = daoStore.getTeamEventDao();
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "Unable to communicate with Event database.", sqle);

            throw new IllegalStateException("Unable to communicate with Event database.", sqle);
        }

    }

    @Override
    public Event create(Event pEvent) throws EventException {
        Event createdEvent = null;
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

    public void update(Event pEvent) throws EventException {
        try {
            eventDao.update(pEvent);
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "There was a problem creating event: " + pEvent);
            throw new EventException("There was a problem creating event: " + pEvent, sqle);
        }
    }

    public Event createOrUpdate(Event event) {
        try {
            Event oldEvent = read(event.getRemoteId());
            if (oldEvent == null) {
                event = create(event);
                Log.d(LOG_NAME, "Created team with remote_id " + event.getRemoteId());
            } else {
                // perform update?
                event.setId(oldEvent.getId());
                update(event);
                Log.d(LOG_NAME, "Updated user with remote_id " + event.getRemoteId());
            }
        } catch (EventException ee) {
            Log.e(LOG_NAME, "There was a problem reading user: " + event, ee);
        }
        return event;
    }

    public List<Event> getEventsByTeam(User pTeam) {
        List<Event> events = new ArrayList<Event>();
        try {
            QueryBuilder<TeamEvent, Long> teamEventQuery = teamEventDao.queryBuilder();
            teamEventQuery.selectColumns("event_id");
            Where<TeamEvent, Long> where = teamEventQuery.where();
            where.eq("team_id", pTeam.getId());

            QueryBuilder<Event, Long> eventQuery = eventDao.queryBuilder();
            eventQuery.where().in("_id", teamEventQuery);

            events = eventQuery.query();
            if(events == null) {
                events = new ArrayList<Event>();
            }

        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "There was a problem getting events for the team: " + pTeam, sqle);
        }
        return events;
    }

    public Event getCurrentEvent(Context pContext) {
        Event event = null;
        try {
            User u = UserHelper.getInstance(pContext).readCurrentUser();
            if(u != null) {
                event = u.getCurrentEvent();
            }
        } catch(UserException ue) {
            Log.e(LOG_NAME, "There is no current user. ", ue);
        }

        return event;
    }
}
