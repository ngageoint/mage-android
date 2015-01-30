package mil.nga.giat.mage.sdk.fetch;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import mil.nga.giat.mage.sdk.ConnectivityAwareIntentService;
import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.user.Role;
import mil.nga.giat.mage.sdk.datastore.user.RoleHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.gson.deserializer.RoleDeserializer;
import mil.nga.giat.mage.sdk.http.client.HttpClientManager;
import mil.nga.giat.mage.sdk.http.get.MageServerGetRequests;
import mil.nga.giat.mage.sdk.login.LoginTaskFactory;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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
 * This class will also fetch roles once too!
 *
 */
public class UserFetchIntentService extends ConnectivityAwareIntentService {
	
	private static final String LOG_NAME = UserFetchIntentService.class.getName();

    private Boolean alreadyFetchedRoles = Boolean.FALSE;

	public UserFetchIntentService() {
		super(LOG_NAME);
	}
	
	protected AtomicBoolean fetchSemaphore = new AtomicBoolean(false);

	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		super.onHandleIntent(intent);

        UserHelper userHelper = UserHelper.getInstance(getApplicationContext());

        User currentUser = null;
        try {
            currentUser = userHelper.readCurrentUser();
        } catch (UserException e) {
            Log.e(LOG_NAME, "Could not get current users.");
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Boolean isDataFetchEnabled = sharedPreferences.getBoolean(getApplicationContext().getString(R.string.dataFetchEnabledKey), true);

        isDataFetchEnabled = sharedPreferences.getBoolean(getApplicationContext().getString(R.string.dataFetchEnabledKey), true);

        if (isConnected && isDataFetchEnabled && !LoginTaskFactory.getInstance(getApplicationContext()).isLocalLogin()) {
            if(!alreadyFetchedRoles) {
                Log.d(LOG_NAME, "The device is currently connected. Attempting to fetch roles...");
                List<Exception> exceptions = new ArrayList<Exception>();
                Collection<Role> roles = MageServerGetRequests.getAllRoles(getApplicationContext(), exceptions);

                if(exceptions.isEmpty()) {
                    RoleHelper roleHelper = RoleHelper.getInstance(getApplicationContext());
                    for(Role role : roles) {
                        try  {
                            if (roleHelper.read(role.getRemoteId()) == null) {
                                role = roleHelper.create(role);
                                Log.d(LOG_NAME, "created role with remote_id " + role.getRemoteId());
                            }
                        } catch (Exception e) {
                            Log.e(LOG_NAME, "There was a failure while performing an User Fetch opperation.", e);
                            continue;
                        }
                    }
                    alreadyFetchedRoles = Boolean.TRUE;
                } else {
                    Log.e(LOG_NAME, "Problem fetching roles.  Will try again.");
                    alreadyFetchedRoles = Boolean.TRUE;
                }
            } else {
                Log.d(LOG_NAME, "The device is currently connected. Attempting to fetch users...");
                Collection<User> users = MageServerGetRequests.getAllUsers(getApplicationContext());
                Log.d(LOG_NAME, "Fetched " + users.size() + " users");

                UserAvatarFetchTask avatarFetch = new UserAvatarFetchTask(getApplicationContext());
                UserIconFetchTask iconFetch = new UserIconFetchTask(getApplicationContext());

                ArrayList<User> userAvatarsToFetch = new ArrayList<User>();
                ArrayList<User> userIconsToFetch = new ArrayList<User>();

                for (User user : users) {
                    try {
                        if (isCanceled) {
                            break;
                        }

                        if (user != null) {
                            if (currentUser != null) {
                                user.setCurrentUser(currentUser.getRemoteId().equalsIgnoreCase(user.getRemoteId()));
                            }
                            user.setFetchedDate(new Date());
                            userHelper.createOrUpdate(user);
                            if (user.getAvatarUrl() != null) {
                                userAvatarsToFetch.add(user);
                            }
                            if (user.getIconUrl() != null) {
                                userIconsToFetch.add(user);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(LOG_NAME, "There was a failure while performing an User Fetch opperation.", e);
                        continue;
                    }
                }
                avatarFetch.execute(userAvatarsToFetch.toArray(new User[userAvatarsToFetch.size()]));
                iconFetch.execute(userIconsToFetch.toArray(new User[userIconsToFetch.size()]));
            }
		} else {
			Log.d(LOG_NAME, "The device is currently disconnected, or data fetch is disabled. Not performing fetch.");
		}
	}
}
