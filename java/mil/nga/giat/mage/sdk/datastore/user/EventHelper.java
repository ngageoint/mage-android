package mil.nga.giat.mage.sdk.datastore.user;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import mil.nga.giat.mage.sdk.datastore.DaoHelper;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.exceptions.EventException;
import mil.nga.giat.mage.sdk.exceptions.UserException;

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
     * @param pContext context
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

    public Event update(Event pEvent) throws EventException {
        try {
            eventDao.update(pEvent);
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "There was a problem creating event: " + pEvent);
            throw new EventException("There was a problem creating event: " + pEvent, sqle);
        }
		return pEvent;
    }

    public Event createOrUpdate(Event event) {
        try {
            Event oldEvent = read(event.getRemoteId());
            if (oldEvent == null) {
                event = create(event);
                Log.d(LOG_NAME, "Created event with remote_id " + event.getRemoteId());
            } else {
                // perform update?
                event.setId(oldEvent.getId());
                update(event);
                Log.d(LOG_NAME, "Updated event with remote_id " + event.getRemoteId());
            }
        } catch (EventException ee) {
            Log.e(LOG_NAME, "There was a problem reading user: " + event, ee);
        }
        return event;
    }

    public List<Event> getEventsByTeam(Team pTeam) {
        List<Event> events = new ArrayList<>();
        try {
            QueryBuilder<TeamEvent, Long> teamEventQuery = teamEventDao.queryBuilder();
            teamEventQuery.selectColumns("event_id");
            Where<TeamEvent, Long> where = teamEventQuery.where();
            where.eq("team_id", pTeam.getId());

            QueryBuilder<Event, Long> eventQuery = eventDao.queryBuilder();
            eventQuery.where().in("_id", teamEventQuery);

            events = eventQuery.query();
            if(events == null) {
                events = new ArrayList<>();
            }

        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "There was a problem getting events for the team: " + pTeam, sqle);
        }
        return events;
    }

	public List<Event> getEventsForCurrentUser() {
		List<Event> events = new ArrayList<>();
		try {
			User user = UserHelper.getInstance(mApplicationContext).readCurrentUser();
			if (user != null) {
				events = getEventsByUser(user);
			}
		} catch(UserException ue) {
			Log.e(LOG_NAME, "There is no current user. ", ue);
		}
		return events;
	}

	public List<Event> getEventsByUser(User pUser) {
		List<Event> events = new ArrayList<>();
		List<Team> teams = TeamHelper.getInstance(mApplicationContext).getTeamsByUser(pUser);
		for(Team team : teams) {
			for(Event e : EventHelper.getInstance(mApplicationContext).getEventsByTeam(team)) {
				if(!events.contains(e)) {
					events.add(e);
				}
			}
		}
		Collections.sort(events, new Comparator<Event>() {
			@Override
			public int compare(Event lhs, Event rhs) {
				return lhs.getName().compareTo(rhs.getName());
			}
		});
		return events;
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

    public Event getRecentEvent() throws EventException {
        Event event = null;
        try {
            User user = UserHelper.getInstance(mApplicationContext).readCurrentUser();
            if (user != null) {
                List<Event> events = eventDao.queryBuilder().limit(1l).where().eq(Event.COLUMN_NAME_REMOTE_ID, user.getRecentEventId()).query();
                event = events.isEmpty() ? null : events.get(0);
            } else {
                Log.d(LOG_NAME, "Current user is null.  Why?");
            }
        } catch(Exception e) {
            throw new EventException("There was a problem reading users current event");
        }

        return event;
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
