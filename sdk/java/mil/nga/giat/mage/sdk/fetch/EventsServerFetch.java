package mil.nga.giat.mage.sdk.fetch;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.Role;
import mil.nga.giat.mage.sdk.datastore.user.RoleHelper;
import mil.nga.giat.mage.sdk.datastore.user.Team;
import mil.nga.giat.mage.sdk.datastore.user.TeamHelper;
import mil.nga.giat.mage.sdk.http.resource.EventResource;
import mil.nga.giat.mage.sdk.http.resource.RoleResource;

/**
 * Created by wnewman on 2/21/18.
 */

public class EventsServerFetch extends AsyncTask<Void, Void, Exception> {

    public interface EventsFetchListener {
        void onEventsFetched(boolean status, Exception error);
    }

    private static final String LOG_NAME = EventsServerFetch.class.getName();

    private Context context;
    private EventsFetchListener listener;

    public EventsServerFetch(Context context) {
        this.context = context;
    }

    public void setEventFetchListener(EventsFetchListener listener) {
        this.listener = listener;
    }

    @Override
    protected Exception doInBackground(Void[] params) {
        Log.d(LOG_NAME, "The device is currently connected.");
        long start = System.currentTimeMillis();
        Exception e = fetchAndSaveRoles();
        long end = System.currentTimeMillis();
        Log.d(LOG_NAME, "Pulled and saved roles in " + (end - start) / 1000 + " seconds");
        if (e != null) {
            return e;
        }

        start = System.currentTimeMillis();
        e = fetchAndSaveEvents();
        end = System.currentTimeMillis();
        Log.d(LOG_NAME, "Pulled and saved events in " + (end - start) / 1000 + " seconds");
        if (e != null) {
            return e;
        }

        return null;
    }

    @Override
    protected void onPostExecute(Exception e) {
        if (listener != null) {
            listener.onEventsFetched(e == null, e);
        }
    }

    private Exception fetchAndSaveRoles() {
        Log.d(LOG_NAME, "Attempting to fetch roles...");

        RoleResource roleResource = new RoleResource(context);
        try {
            Collection<Role> roles = roleResource.getRoles();
            Log.d(LOG_NAME, "Fetched " + roles.size() + " roles");

            RoleHelper roleHelper = RoleHelper.getInstance(context);
            for (Role role : roles) {
                if (role != null) {
                    roleHelper.createOrUpdate(role);
                }
            }
        } catch (IOException e) {
            Log.e(LOG_NAME, "Problem fetching roles.  Will try again soon.");
            return e;
        }

        return null;
    }

    private Exception fetchAndSaveEvents() {
        EventResource eventResource = new EventResource(context);
        TeamHelper teamHelper = TeamHelper.getInstance(context);
        teamHelper.deleteTeamEvents();

        Log.d(LOG_NAME, "Attempting to fetch events...");

        try {
            Map<Event, Collection<Team>> events = eventResource.getEvents();
            Log.d(LOG_NAME, "Fetched " + events.size() + " events");

            EventHelper eventHelper = EventHelper.getInstance(context);
            for (Event event : events.keySet()) {
                try {
                    eventHelper.createOrUpdate(event);
                } catch (Exception e) {
                    Log.e(LOG_NAME, "There was a failure while performing an event fetch operation.", e);
                }
            }

            EventHelper.getInstance(context).syncEvents(events.keySet());

        } catch (Exception e) {
            Log.e(LOG_NAME, "Problem fetching events.  Will try again soon.");
            return e;
        }

        return null;
    }
}
