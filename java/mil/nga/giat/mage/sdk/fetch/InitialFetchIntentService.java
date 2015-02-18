package mil.nga.giat.mage.sdk.fetch;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import mil.nga.giat.mage.sdk.ConnectivityAwareIntentService;
import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.Role;
import mil.nga.giat.mage.sdk.datastore.user.RoleHelper;
import mil.nga.giat.mage.sdk.datastore.user.Team;
import mil.nga.giat.mage.sdk.datastore.user.TeamEvent;
import mil.nga.giat.mage.sdk.datastore.user.TeamHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.datastore.user.UserTeam;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.gson.deserializer.RoleDeserializer;
import mil.nga.giat.mage.sdk.http.client.HttpClientManager;
import mil.nga.giat.mage.sdk.http.get.MageServerGetRequests;
import mil.nga.giat.mage.sdk.login.LoginTaskFactory;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * This class will fetch events, roles, users and teams just once.
 *
 */
public class InitialFetchIntentService extends ConnectivityAwareIntentService {
	
	private static final String LOG_NAME = InitialFetchIntentService.class.getName();

    public static final String InitialFetchIntentServiceAction = InitialFetchIntentService.class.getCanonicalName();

    private static final long retryTime = 4000;
    private static final long retryCount = 4;

	public InitialFetchIntentService() {
		super(LOG_NAME);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		super.onHandleIntent(intent);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Boolean isDataFetchEnabled = sharedPreferences.getBoolean(getApplicationContext().getString(R.string.dataFetchEnabledKey), true);

        isDataFetchEnabled = sharedPreferences.getBoolean(getApplicationContext().getString(R.string.dataFetchEnabledKey), true);

        if (isConnected && isDataFetchEnabled && !LoginTaskFactory.getInstance(getApplicationContext()).isLocalLogin()) {
            Log.d(LOG_NAME, "The device is currently connected.");
            getRoles();
            getUsers();
            getTeams();
            getEvents();
            // now that the client has fetched the events, fetch the users again in order to populate the user's currentEvent.  a chicken in the egg thing
            getUsers();

		} else {
			Log.d(LOG_NAME, "The device is currently disconnected, or data fetch is disabled, or this is a local login. Not performing fetch.");
		}

        Intent localIntent = new Intent(InitialFetchIntentService.InitialFetchIntentServiceAction);
        localIntent.putExtra("status", true);
        localIntent.addCategory(Intent.CATEGORY_DEFAULT);

        // Broadcasts the Intent
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
	}

    /**
     * Create roles
     */
    private void getRoles() {
        Boolean didFetchRoles = Boolean.FALSE;
        int attemptCount = 0;
        while (!didFetchRoles && !isCanceled && attemptCount < retryCount) {
            Log.d(LOG_NAME, "Attempting to fetch roles...");
            List<Exception> exceptions = new ArrayList<Exception>();
            Collection<Role> roles = MageServerGetRequests.getAllRoles(getApplicationContext(), exceptions);
            Log.d(LOG_NAME, "Fetched " + roles.size() + " roles");

            if (exceptions.isEmpty()) {
                RoleHelper roleHelper = RoleHelper.getInstance(getApplicationContext());
                for (Role role : roles) {
                    if (isCanceled) {
                        break;
                    }
                    if (role != null) {
                        role = roleHelper.createOrUpdate(role);
                    }
                }
                didFetchRoles = Boolean.TRUE;
            } else {
                Log.e(LOG_NAME, "Problem fetching roles.  Will try again soon.");
                didFetchRoles = Boolean.FALSE;
                try {
                    Thread.sleep(retryTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            attemptCount++;
        }
    }

    /**
     * Create users
     */
    private void getUsers() {
        Boolean didFetchUsers = Boolean.FALSE;
        UserHelper userHelper = UserHelper.getInstance(getApplicationContext());

        User currentUser = null;
        try {
            currentUser = userHelper.readCurrentUser();
        } catch (UserException e) {
            Log.e(LOG_NAME, "Could not get current user.");
        }
        int attemptCount = 0;
        while(!didFetchUsers && !isCanceled && attemptCount < retryCount) {
            Log.d(LOG_NAME, "Attempting to fetch users...");
            List<Exception> exceptions = new ArrayList<Exception>();
            Collection<User> users = MageServerGetRequests.getAllUsers(getApplicationContext(), exceptions);
            Log.d(LOG_NAME, "Fetched " + users.size() + " users");

            UserAvatarFetchTask avatarFetch = new UserAvatarFetchTask(getApplicationContext());
            UserIconFetchTask iconFetch = new UserIconFetchTask(getApplicationContext());

            ArrayList<User> userAvatarsToFetch = new ArrayList<User>();
            ArrayList<User> userIconsToFetch = new ArrayList<User>();

            if(exceptions.isEmpty()) {
                for (User user : users) {
                    if (isCanceled) {
                        break;
                    }
                    try {
                        if (user != null) {
                            if (currentUser != null) {
                                user.setCurrentUser(currentUser.getRemoteId().equalsIgnoreCase(user.getRemoteId()));
                            }
                            user.setFetchedDate(new Date());
                            user = userHelper.createOrUpdate(user);
                            if (user.getAvatarUrl() != null) {
                                userAvatarsToFetch.add(user);
                            }
                            if (user.getIconUrl() != null) {
                                userIconsToFetch.add(user);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(LOG_NAME, "There was a failure while performing an user fetch operation.", e);
                        continue;
                    }
                }
                avatarFetch.execute(userAvatarsToFetch.toArray(new User[userAvatarsToFetch.size()]));
                iconFetch.execute(userIconsToFetch.toArray(new User[userIconsToFetch.size()]));
                didFetchUsers = Boolean.TRUE;
            } else {
                Log.e(LOG_NAME, "Problem fetching users.  Will try again soon.");
                didFetchUsers = Boolean.FALSE;
                try {
                    Thread.sleep(retryTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            attemptCount++;
        }
    }


    /**
     * Create teams
     */
    private void getTeams() {
        UserHelper userHelper = UserHelper.getInstance(getApplicationContext());

        Boolean didFetchTeams = Boolean.FALSE;
        int attemptCount = 0;

        while(!didFetchTeams && !isCanceled && attemptCount < retryCount) {
            userHelper.deleteUserTeams();
            Log.d(LOG_NAME, "Attempting to fetch teams...");
            List<Exception> exceptions = new ArrayList<Exception>();
            Map<Team, Collection<User>> teams = MageServerGetRequests.getAllTeams(getApplicationContext(), exceptions);
            Log.d(LOG_NAME, "Fetched " + teams.size() + " teams");

            if(exceptions.isEmpty()) {
                TeamHelper teamHelper = TeamHelper.getInstance(getApplicationContext());
                for (Team team : teams.keySet()) {
                    if (isCanceled) {
                        break;
                    }
                    try {
                        if (team != null) {
                            team = teamHelper.createOrUpdate(team);

                            for (User user : teams.get(team)) {
                                if(userHelper.read(user.getRemoteId()) == null) {
                                    user = userHelper.createOrUpdate(user);
                                }
                                // populate the join table
                                userHelper.create(new UserTeam(user, team));
                            }
                        }
                    } catch (Exception e) {
                        Log.e(LOG_NAME, "There was a failure while performing a team fetch operation.", e);
                        continue;
                    }
                }
                didFetchTeams = Boolean.TRUE;
            } else {
                Log.e(LOG_NAME, "Problem fetching teams.  Will try again soon.");
                didFetchTeams = Boolean.FALSE;
                try {
                    Thread.sleep(retryTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            attemptCount++;
        }
    }


    /**
     * Create events
     */
    private void getEvents() {
        Boolean didFetchEvents = Boolean.FALSE;
        int attemptCount = 0;
        while(!didFetchEvents && !isCanceled && attemptCount < retryCount) {
            TeamHelper teamHelper = TeamHelper.getInstance(getApplicationContext());
            teamHelper.deleteTeamEvents();
            Log.d(LOG_NAME, "Attempting to fetch events...");
            List<Exception> exceptions = new ArrayList<Exception>();
            Map<Event, Collection<Team>> events = MageServerGetRequests.getAllEvents(getApplicationContext(), exceptions);
            Log.d(LOG_NAME, "Fetched " + events.size() + " events");

            if(exceptions.isEmpty()) {
                EventHelper eventHelper = EventHelper.getInstance(getApplicationContext());
                for (Event event : events.keySet()) {
                    if (isCanceled) {
                        break;
                    }
                    try {
                        if (event != null) {
                            event = eventHelper.createOrUpdate(event);

                            for (Team team : events.get(event)) {
                                if(teamHelper.read(team.getRemoteId()) == null) {
                                    team = teamHelper.createOrUpdate(team);
                                }
                                // populate the join table
                                teamHelper.create(new TeamEvent(team, event));
                            }
                        }
                    } catch (Exception e) {
                        Log.e(LOG_NAME, "There was a failure while performing an event fetch operation.", e);
                        continue;
                    }
                }
                didFetchEvents = Boolean.TRUE;
            } else {
                Log.e(LOG_NAME, "Problem fetching events.  Will try again soon.");
                didFetchEvents = Boolean.FALSE;
                try {
                    Thread.sleep(retryTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            attemptCount++;
        }
    }
}
