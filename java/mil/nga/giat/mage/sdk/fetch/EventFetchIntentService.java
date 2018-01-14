package mil.nga.giat.mage.sdk.fetch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Collection;
import java.util.Map;

import mil.nga.giat.mage.sdk.ConnectivityAwareIntentService;
import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.Team;
import mil.nga.giat.mage.sdk.datastore.user.TeamEvent;
import mil.nga.giat.mage.sdk.datastore.user.TeamHelper;
import mil.nga.giat.mage.sdk.http.resource.EventResource;
import mil.nga.giat.mage.sdk.login.LoginTaskFactory;

/**
 * This class will fetch events, roles, users and teams just once.
 *
 */
public class EventFetchIntentService extends ConnectivityAwareIntentService {

	private static final String LOG_NAME = EventFetchIntentService.class.getName();

    public static final String EventFetchIntentServiceAction = EventFetchIntentService.class.getCanonicalName();

    private static final long retryTime = 5000;
    private static final long retryCount = 3;

	public EventFetchIntentService() {
		super(LOG_NAME);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		super.onHandleIntent(intent);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Boolean isDataFetchEnabled = sharedPreferences.getBoolean(getApplicationContext().getString(R.string.dataFetchEnabledKey), getApplicationContext().getResources().getBoolean(R.bool.dataFetchEnabledDefaultValue));

        if (isConnected && isDataFetchEnabled && !LoginTaskFactory.getInstance(getApplicationContext()).isLocalLogin()) {
            Log.d(LOG_NAME, "The device is currently connected.");

            long start = System.currentTimeMillis();
            fetchAndSaveEvents();
            long end = System.currentTimeMillis();
            Log.d(LOG_NAME, "Pulled and saved events in " + (end - start) / 1000 + " seconds");
        } else {
			Log.d(LOG_NAME, "The device is currently disconnected, or data fetch is disabled, or this is a local login. Not performing fetch.");
		}

        Intent localIntent = new Intent(EventFetchIntentService.EventFetchIntentServiceAction);
        localIntent.putExtra("status", true);
        localIntent.addCategory(Intent.CATEGORY_DEFAULT);

        // Broadcasts the Intent
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

        stopSelf();
	}


    /**
     * Create events
     * TODO make sure events get deleted
     */
    private void fetchAndSaveEvents() {
        Boolean didFetchEvents = Boolean.FALSE;
        int attemptCount = 0;
        EventResource eventResource = new EventResource(getApplicationContext());
        while(!didFetchEvents && !isCanceled && attemptCount < retryCount) {
            TeamHelper teamHelper = TeamHelper.getInstance(getApplicationContext());
            teamHelper.deleteTeamEvents();

            Log.d(LOG_NAME, "Attempting to fetch events...");

            try {
                Map<Event, Collection<Team>> events = eventResource.getEvents();
                Log.d(LOG_NAME, "Fetched " + events.size() + " events");

                EventHelper eventHelper = EventHelper.getInstance(getApplicationContext());
                for (Event event : events.keySet()) {
                    if (isCanceled) {
                        break;
                    }
                    try {
                        if (event != null) {
                            event = eventHelper.createOrUpdate(event);

                            for (Team t : events.get(event)) {
                                Team team = teamHelper.read(t.getRemoteId());
                                if (team == null) {
                                    team = teamHelper.createOrUpdate(t);
                                }

                                // populate the join table
                                teamHelper.create(new TeamEvent(team, event));
                            }
                        }
                    } catch (Exception e) {
                        Log.e(LOG_NAME, "There was a failure while performing an event fetch operation.", e);
                    }
                }

                EventHelper.getInstance(getApplicationContext()).syncEvents(events.keySet());

                didFetchEvents = Boolean.TRUE;
            } catch (Exception e) {
                Log.e(LOG_NAME, "Problem fetching events.  Will try again soon.");
                didFetchEvents = Boolean.FALSE;
                try {
                    Thread.sleep(retryTime);
                } catch (InterruptedException ie) {
                    e.printStackTrace();
                }
            }

            attemptCount++;
        }
    }
}
