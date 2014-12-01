package mil.nga.giat.mage.sdk.fetch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import mil.nga.giat.mage.sdk.ConnectivityAwareIntentService;
import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.http.get.MageServerGetRequests;
import mil.nga.giat.mage.sdk.login.LoginTaskFactory;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class UserFetchIntentService extends ConnectivityAwareIntentService {
	
	private static final String LOG_NAME = UserFetchIntentService.class.getName();

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
		Log.d(LOG_NAME, "Going to fetch all the users");
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
			
		} else {
			Log.d(LOG_NAME, "The device is currently disconnected, or data fetch is disabled. Not performing fetch.");
		}
	}
}
