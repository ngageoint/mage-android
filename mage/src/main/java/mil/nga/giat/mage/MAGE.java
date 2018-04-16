package mil.nga.giat.mage;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;

import mil.nga.giat.mage.login.LoginActivity;
import mil.nga.giat.mage.login.OAuthActivity;
import mil.nga.giat.mage.login.ServerUrlActivity;
import mil.nga.giat.mage.login.SignupActivity;
import mil.nga.giat.mage.observation.ObservationNotificationListener;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.event.ISessionEventListener;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.fetch.LocationFetchIntentService;
import mil.nga.giat.mage.sdk.fetch.ObservationFetchIntentService;
import mil.nga.giat.mage.sdk.fetch.StaticFeatureServerFetch;
import mil.nga.giat.mage.sdk.http.HttpClientManager;
import mil.nga.giat.mage.sdk.http.resource.UserResource;
import mil.nga.giat.mage.sdk.location.LocationService;
import mil.nga.giat.mage.sdk.push.AttachmentPushService;
import mil.nga.giat.mage.sdk.push.LocationPushIntentService;
import mil.nga.giat.mage.sdk.push.ObservationPushIntentService;
import mil.nga.giat.mage.sdk.screen.ScreenChangeReceiver;
import mil.nga.giat.mage.sdk.utils.UserUtility;
import mil.nga.giat.mage.wearable.InitializeMAGEWearBridge;

public class MAGE extends MultiDexApplication implements ISessionEventListener, Application.ActivityLifecycleCallbacks {

	private static final String LOG_NAME = MAGE.class.getName();

	public static final int MAGE_NOTIFICATION_ID = 1414;

	public interface OnLogoutListener {
		void onLogout();
	}

	private LocationService locationService;
	private Intent locationFetchIntent;
	private Intent observationFetchIntent;
	private Intent locationPushIntent;
	private Intent observationPushIntent;

	private ObservationNotificationListener observationNotificationListener = null;
	private AttachmentPushService attachmentPushService = null;

	private StaticFeatureServerFetch staticFeatureServerFetch = null;

	private Activity runningActivity;

	@Override
	public void onCreate() {
		// setup the screen unlock stuff
		registerReceiver(ScreenChangeReceiver.getInstance(), new IntentFilter(Intent.ACTION_SCREEN_ON));

		HttpClientManager.getInstance(getApplicationContext()).addListener(this);

		registerActivityLifecycleCallbacks(this);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		int dayNightTheme = preferences.getInt(getResources().getString(R.string.dayNightThemeKey), getResources().getInteger(R.integer.dayNightThemeDefaultValue));
		AppCompatDelegate.setDefaultNightMode(dayNightTheme);

		super.onCreate();
	}

	public void onLogin() {
		createNotification();

		// Start location services
		initLocationService();

		//set up Observation notifications
		if (observationNotificationListener == null) {
			observationNotificationListener = new ObservationNotificationListener(getApplicationContext());
			ObservationHelper.getInstance(getApplicationContext()).addListener(observationNotificationListener);
		}

		// Start fetching and pushing observations and locations
		startFetching();
		startPushing();

		// Pull static layers and features just once
		loadStaticFeatures(false, null);

		InitializeMAGEWearBridge.startBridgeIfWearBuild(getApplicationContext());
	}

	public void loadStaticFeatures(final boolean force, final StaticFeatureServerFetch.OnStaticLayersListener listener) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				staticFeatureServerFetch = new StaticFeatureServerFetch(getApplicationContext());
				try {
					staticFeatureServerFetch.fetch(force, listener);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		new Thread(runnable).start();
	}

	public void onLogout(Boolean clearTokenInformationAndSendLogoutRequest, final OnLogoutListener logoutListener) {

		if (observationNotificationListener != null) {
			ObservationHelper.getInstance(getApplicationContext()).removeListener(observationNotificationListener);
			observationNotificationListener = null;
		}

		destroyFetching();
		destroyPushing();
		destroyLocationService();
		destroyNotification();

		if (clearTokenInformationAndSendLogoutRequest) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					UserResource userResource = new UserResource(getApplicationContext());
					if (!userResource.logout()) {
						Log.e(LOG_NAME, "Unable to logout from server.");
					}

					if (logoutListener != null) {
						logoutListener.onLogout();
					}
				}
			};

			UserUtility.getInstance(getApplicationContext()).clearTokenInformation();

			new Thread(runnable).start();
		} else {
			if (logoutListener != null) {
				logoutListener.onLogout();
			}
		}

		unregisterActivityLifecycleCallbacks(this);

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean(getString(R.string.disclaimerAcceptedKey), false).commit();

		try {
			UserHelper userHelper = UserHelper.getInstance(getApplicationContext());
			User user = userHelper.readCurrentUser();
			userHelper.removeCurrentEvent(user);
		} catch (UserException e) {
			e.printStackTrace();
		}

		Boolean deleteAllDataOnLogout = sharedPreferences.getBoolean(getApplicationContext().getString(R.string.deleteAllDataOnLogoutKey), getResources().getBoolean(R.bool.deleteAllDataOnLogoutDefaultValue));

		if (deleteAllDataOnLogout) {
			LandingActivity.deleteAllData(getApplicationContext());
		}
	}

	private void destroyNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(MAGE_NOTIFICATION_ID);
		notificationManager.cancel(ObservationNotificationListener.OBSERVATION_NOTIFICATION_ID);
	}

	private void createNotification() {
		// this line is some magic for kitkat
		getLogoutPendingIntent().cancel();
		boolean tokenExpired = UserUtility.getInstance(getApplicationContext()).isTokenExpired();

		String notificationMsg = tokenExpired ? "Your token has expired, please tap to login." : "You are logged in. Slide down to logout.";
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_wand_white_50dp)
				.setContentTitle("MAGE")
				.setOngoing(true)
				.setPriority(NotificationCompat.PRIORITY_MAX)
				.setContentText(notificationMsg)
				.addAction(R.drawable.ic_power_settings_new_white_24dp, "Logout", getLogoutPendingIntent());

		NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
		bigTextStyle.setBigContentTitle("MAGE").bigText(notificationMsg);

		builder.setStyle(bigTextStyle);

		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(this, LoginActivity.class);

		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(LoginActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(resultPendingIntent);
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(MAGE_NOTIFICATION_ID, builder.build());
	}

	private PendingIntent getLogoutPendingIntent() {
		Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
		intent.putExtra("LOGOUT", true);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		return PendingIntent.getActivity(getApplicationContext(), 1, intent, 0);
	}

	/**
	 * Start Tasks responsible for fetching Observations and Locations from the server.
	 */
	private void startFetching() {
        if(locationFetchIntent == null) {
            locationFetchIntent = new Intent(getApplicationContext(), LocationFetchIntentService.class);
            startService(locationFetchIntent);
        }

        if(observationFetchIntent == null) {
            observationFetchIntent = new Intent(getApplicationContext(), ObservationFetchIntentService.class);
            startService(observationFetchIntent);
        }
	}

    /**
	 * Stop Tasks responsible for fetching Observations and Locations from the server.
	 */
	private void destroyFetching() {
		if (staticFeatureServerFetch != null) {
			staticFeatureServerFetch.destroy();
			staticFeatureServerFetch = null;
		}
		if(locationFetchIntent != null) {
			stopService(locationFetchIntent);
			locationFetchIntent = null;
		}
		if(observationFetchIntent != null) {
			stopService(observationFetchIntent);
			observationFetchIntent = null;
		}
	}

	/**
	 * Start Tasks responsible for pushing Observations, Attachments and Locations to the server.
	 */
	private void startPushing() {
		if (locationPushIntent == null) {
			locationPushIntent = new Intent(getApplicationContext(), LocationPushIntentService.class);
			startService(locationPushIntent);
		}
		if (observationPushIntent == null) {
			observationPushIntent = new Intent(getApplicationContext(), ObservationPushIntentService.class);
			startService(observationPushIntent);
		}

		attachmentPushService = new AttachmentPushService(getApplicationContext());
		attachmentPushService.start();

		attachmentPushService = new AttachmentPushService(getApplicationContext());
		attachmentPushService.start();
	}

	/**
	 * Stop Tasks responsible for pushing Observations and Locations to the server.
	 */
	private void destroyPushing() {
		if (locationPushIntent != null) {
			stopService(locationPushIntent);
			locationPushIntent = null;
		}
		if (observationPushIntent != null) {
			stopService(observationPushIntent);
			observationPushIntent = null;
		}

		if (attachmentPushService != null) {
			attachmentPushService.stop();
			attachmentPushService = null;
		}
	}

	private void initLocationService() {
		if (locationService == null) {
			locationService = new LocationService(getApplicationContext());
			locationService.init();
		}
	}

	private void destroyLocationService() {
		if (locationService != null) {
			locationService.destroy();
			locationService = null;
		}
	}

	public LocationService getLocationService() {
		return locationService;
	}

	@Override
	public void onError(Throwable error) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onTokenExpired() {
		destroyFetching();
		destroyPushing();
		createNotification();

		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean(getString(R.string.disclaimerAcceptedKey), false).commit();

		if (runningActivity != null &&
				!(runningActivity instanceof LoginActivity) &&
				!(runningActivity instanceof OAuthActivity) &&
				!(runningActivity instanceof SignupActivity) &&
				!(runningActivity instanceof ServerUrlActivity)) {
			forceLogin(true);
		}
	}

	@Override
	public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

	}

	@Override
	public void onActivityStarted(Activity activity) {

	}

	@Override
	public void onActivityResumed(Activity activity) {

		if (UserUtility.getInstance(getApplicationContext()).isTokenExpired() &&
				!(activity instanceof LoginActivity) &&
				!(activity instanceof OAuthActivity) &&
				!(activity instanceof SignupActivity) &&
				!(activity instanceof ServerUrlActivity)) {
			forceLogin(false);
		}

		runningActivity = activity;
	}

	@Override
	public void onActivityPaused(Activity activity) {
		runningActivity = null;
	}

	@Override
	public void onActivityStopped(Activity activity) {

	}

	@Override
	public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

	}

	@Override
	public void onActivityDestroyed(Activity activity) {

	}

	private void forceLogin(boolean applicationInUse) {
		Intent intent = new Intent(this, LoginActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(LoginActivity.EXTRA_CONTINUE_SESSION, true);

		if (applicationInUse) {
			intent.putExtra(LoginActivity.EXTRA_CONTINUE_SESSION_WHILE_USING, true);
		}

		startActivity(intent);
	}

}
