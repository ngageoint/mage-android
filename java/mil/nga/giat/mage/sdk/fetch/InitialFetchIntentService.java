package mil.nga.giat.mage.sdk.fetch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import mil.nga.giat.mage.sdk.ConnectivityAwareIntentService;
import mil.nga.giat.mage.sdk.R;
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
import mil.nga.giat.mage.sdk.login.LoginTaskFactory;
import mil.nga.giat.mage.sdk.retrofit.resource.EventResource;
import mil.nga.giat.mage.sdk.retrofit.resource.RoleResource;
import mil.nga.giat.mage.sdk.retrofit.resource.TeamResource;
import mil.nga.giat.mage.sdk.retrofit.resource.UserResource;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

/**
 * This class will fetch events, roles, users and teams just once.
 *
 */
public class InitialFetchIntentService extends ConnectivityAwareIntentService {
	
	private static final String LOG_NAME = InitialFetchIntentService.class.getName();

    public static final String InitialFetchIntentServiceAction = InitialFetchIntentService.class.getCanonicalName();

	private final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(64);
	private final ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8, 10, TimeUnit.SECONDS, queue);

    private static final long retryTime = 4000;
    private static final long retryCount = 4;

	private DownloadImageTask avatarFetch;
	private DownloadImageTask iconFetch;

	public InitialFetchIntentService() {
		super(LOG_NAME);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		super.onHandleIntent(intent);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Boolean isDataFetchEnabled = sharedPreferences.getBoolean(getApplicationContext().getString(R.string.dataFetchEnabledKey), getApplicationContext().getResources().getBoolean(R.bool.dataFetchEnabledDefaultValue));

        if (isConnected && isDataFetchEnabled && !LoginTaskFactory.getInstance(getApplicationContext()).isLocalLogin()) {
            Log.d(LOG_NAME, "The device is currently connected.");
            fetchAndSaveRoles();
			fetchAndSaveUsers();
            fetchAndSaveTeams();
            fetchAndSaveEvents();

            Handler handler = new Handler(Looper.getMainLooper());
			// users are updated, finish getting image content
            if (avatarFetch != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        avatarFetch.executeOnExecutor(executor);
                    }
                });

            }
            if (iconFetch != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        iconFetch.executeOnExecutor(executor);
                    }
                });
            }
        } else {
			Log.d(LOG_NAME, "The device is currently disconnected, or data fetch is disabled, or this is a local login. Not performing fetch.");
		}

        Intent localIntent = new Intent(InitialFetchIntentService.InitialFetchIntentServiceAction);
        localIntent.putExtra("status", true);
        localIntent.addCategory(Intent.CATEGORY_DEFAULT);

        // Broadcasts the Intent
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

        stopSelf();
	}

    /**
     * Create roles
     */
    private void fetchAndSaveRoles() {
        Boolean didFetchRoles = Boolean.FALSE;
        int attemptCount = 0;
        while (!didFetchRoles && !isCanceled && attemptCount < retryCount) {
            Log.d(LOG_NAME, "Attempting to fetch roles...");

            RoleResource roleResource = new RoleResource(getApplicationContext());
            try {
                Collection<Role> roles = roleResource.getRoles();
                Log.d(LOG_NAME, "Fetched " + roles.size() + " roles");

                RoleHelper roleHelper = RoleHelper.getInstance(getApplicationContext());
                for (Role role : roles) {
                    if (isCanceled) {
                        break;
                    }
                    if (role != null) {
                        roleHelper.createOrUpdate(role);
                    }
                }

                didFetchRoles = Boolean.TRUE;
            } catch (IOException e) {
                Log.e(LOG_NAME, "Problem fetching roles.  Will try again soon.");
                didFetchRoles = Boolean.FALSE;
                try {
                    Thread.sleep(retryTime);
                } catch (InterruptedException ie) {
                    e.printStackTrace();
                }
            }

            attemptCount++;
        }
    }

    /**
     * Create users
     */
    private void fetchAndSaveUsers() {
        int attemptCount = 0;
        Boolean didFetchUsers = Boolean.FALSE;

        final UserResource userResource = new UserResource(getApplicationContext());
        final UserHelper userHelper = UserHelper.getInstance(getApplicationContext());

        User currentUser = null;
        try {
            currentUser = userHelper.readCurrentUser();
        } catch (UserException e) {
            Log.e(LOG_NAME, "Could not get current user.");
        }

        while(!didFetchUsers && !isCanceled && attemptCount < retryCount) {
            Log.d(LOG_NAME, "Attempting to fetch users...");

            try {
                Collection<User> users = userResource.getUsers();
                Log.d(LOG_NAME, "Fetched " + users.size() + " users");

                final ArrayList<User> userAvatarsToFetch = new ArrayList<>();
                final ArrayList<User> userIconsToFetch = new ArrayList<>();
                for (User user : users) {
                    if (isCanceled) {
                        break;
                    }
                    try {
                        if (user != null) {
                            if (currentUser != null) {
                                boolean isCurrentUser = currentUser.getRemoteId().equalsIgnoreCase(user.getRemoteId());
                                user.setCurrentUser(isCurrentUser);
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
                    }
                }

                // pull down images (map icons and profile pictures)
                List<String> avatarUrls = new ArrayList<>();
                List<String> avatarLocalFilePaths = new ArrayList<>();
                for(User u : userAvatarsToFetch) {
                    avatarUrls.add(u.getAvatarUrl());
                    avatarLocalFilePaths.add(MediaUtility.getAvatarDirectory() + "/" + u.getId() + ".png");
                }
                avatarFetch = new DownloadImageTask(getApplicationContext(), avatarUrls, avatarLocalFilePaths, true) {

                    @Override
                    protected Void doInBackground(Void... v) {
                        Void result = super.doInBackground(v);
                        for(int i = 0; i < localFilePaths.size(); i++) {
                            try {
                                if(!errors.get(i)) {
                                    User u = userHelper.read(userAvatarsToFetch.get(i).getId());
                                    u.setLocalAvatarPath(localFilePaths.get(i));
                                    userHelper.update(u);
                                }
                            } catch(Exception e) {
                                Log.e(LOG_NAME, "Could not read or update user.", e);
                            }
                        }
                        return result;
                    }
                };

                List<String> iconUrls = new ArrayList<>();
                List<String> iconLocalFilePaths = new ArrayList<>();
                for(User u : userIconsToFetch) {
                    iconUrls.add(u.getIconUrl());
                    iconLocalFilePaths.add(MediaUtility.getUserIconDirectory() + "/" + u.getId() + ".png");
                }

                iconFetch = new DownloadImageTask(getApplicationContext(), iconUrls, iconLocalFilePaths, true) {

                    @Override
                    protected Void doInBackground(Void... v) {
                        Void result = super.doInBackground(v);

                        for(int i = 0; i < localFilePaths.size(); i++) {
                            try {
                                if(!errors.get(i)) {
                                    User u = userHelper.read(userIconsToFetch.get(i).getId());
                                    u.setLocalIconPath(localFilePaths.get(i));
                                    userHelper.update(u);
                                }
                            } catch(Exception e) {
                                Log.e(LOG_NAME, "Could not read or update user.", e);
                            }
                        }

                        return result;
                    }
                };

                didFetchUsers = Boolean.TRUE;
            } catch (Exception e) {
                Log.e(LOG_NAME, "Problem fetching users.  Will try again soon.");
                didFetchUsers = Boolean.FALSE;
                try {
                    Thread.sleep(retryTime);
                } catch (InterruptedException ie) {
                    e.printStackTrace();
                }
            }

            attemptCount++;
        }
    }

    /**
     * Create teams
     */
    private void fetchAndSaveTeams() {
        UserHelper userHelper = UserHelper.getInstance(getApplicationContext());

        Boolean didFetchTeams = Boolean.FALSE;
        int attemptCount = 0;

        TeamResource teamResource = new TeamResource(getApplicationContext());
        while(!didFetchTeams && !isCanceled && attemptCount < retryCount) {
            userHelper.deleteUserTeams();
            Log.d(LOG_NAME, "Attempting to fetch teams...");

            try {
                Map<Team, Collection<User>> teams = teamResource.getTeams();
                Log.d(LOG_NAME, "Fetched " + teams.size() + " teams");

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
                    }
                }

                TeamHelper.getInstance(getApplicationContext()).syncTeams(teams.keySet());

                didFetchTeams = Boolean.TRUE;
            } catch (Exception e) {
                Log.e(LOG_NAME, "Problem fetching teams.  Will try again soon.");
                didFetchTeams = Boolean.FALSE;
                try {
                    Thread.sleep(retryTime);
                } catch (InterruptedException ie) {
                    e.printStackTrace();
                }
            }

            attemptCount++;
        }
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
