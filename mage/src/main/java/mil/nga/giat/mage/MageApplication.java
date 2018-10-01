package mil.nga.giat.mage;

import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;

import com.squareup.okhttp.ResponseBody;

import dagger.android.AndroidInjector;
import dagger.android.support.DaggerApplication;
import mil.nga.giat.mage.dagger.DaggerMageComponent;
import mil.nga.giat.mage.location.LocationReportingService;
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
import mil.nga.giat.mage.sdk.push.AttachmentPushService;
import mil.nga.giat.mage.sdk.push.LocationPushIntentService;
import mil.nga.giat.mage.sdk.push.ObservationPushIntentService;
import mil.nga.giat.mage.sdk.screen.ScreenChangeReceiver;
import mil.nga.giat.mage.sdk.utils.UserUtility;
import mil.nga.giat.mage.wearable.InitializeMAGEWearBridge;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

public class MageApplication extends DaggerApplication implements SharedPreferences.OnSharedPreferenceChangeListener, ISessionEventListener, Application.ActivityLifecycleCallbacks {

	private static final String LOG_NAME = MageApplication.class.getName();

	public static final int MAGE_SUMMARY_NOTIFICATION_ID = 100;
	public static final int MAGE_ACCOUNT_NOTIFICATION_ID = 101;

	public static final String MAGE_NOTIFICATION_GROUP = "mil.nga.mage.MAGE_NOTIFICATION_GROUP";
	public static final String MAGE_NOTIFICATION_CHANNEL_ID = "mil.nga.mage.MAGE_NOTIFICATION_CHANNEL";

	public interface OnLogoutListener {
		void onLogout();
	}

	private Intent locationReportingServiceIntent;
	private Intent locationFetchIntent;
	private Intent observationFetchIntent;
	private Intent locationPushIntent;
	private Intent observationPushIntent;

	private ObservationNotificationListener observationNotificationListener = null;
	private AttachmentPushService attachmentPushService = null;

	private StaticFeatureServerFetch staticFeatureServerFetch = null;

	private Activity runningActivity;

    @Override
    protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
    	return DaggerMageComponent.builder().application(this).build();
    }

	@Override
	public void onCreate() {
		super.onCreate();

		// setup the screen unlock stuff
		registerReceiver(ScreenChangeReceiver.getInstance(), new IntentFilter(Intent.ACTION_SCREEN_ON));

		HttpClientManager.getInstance(getApplicationContext()).addListener(this);

		registerActivityLifecycleCallbacks(this);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		int dayNightTheme = preferences.getInt(getResources().getString(R.string.dayNightThemeKey), getResources().getInteger(R.integer.dayNightThemeDefaultValue));
		AppCompatDelegate.setDefaultNightMode(dayNightTheme);

		preferences.registerOnSharedPreferenceChangeListener(this);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			NotificationChannel channel = new NotificationChannel(MAGE_NOTIFICATION_CHANNEL_ID,"MAGE", NotificationManager.IMPORTANCE_LOW);
			channel.setShowBadge(true);
			notificationManager.createNotificationChannel(channel);
		}
	}

	public void onLogin() {
		createNotification();

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
		destroyNotification();
		stopLocationService();

		if (clearTokenInformationAndSendLogoutRequest) {
			UserResource userResource = new UserResource(getApplicationContext());
			userResource.logout(new Callback<ResponseBody>() {
				@Override
				public void onResponse(Response<ResponseBody> response, Retrofit retrofit) {
					if (logoutListener != null) {
						logoutListener.onLogout();
					}
				}

				@Override
				public void onFailure(Throwable t) {
					Log.e(LOG_NAME, "Unable to logout from server.");
					if (logoutListener != null) {
						logoutListener.onLogout();
					}
				}
			});
			UserUtility.getInstance(getApplicationContext()).clearTokenInformation();
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
		notificationManager.cancel(MAGE_SUMMARY_NOTIFICATION_ID);
		notificationManager.cancel(MAGE_ACCOUNT_NOTIFICATION_ID);
		notificationManager.cancel(ObservationNotificationListener.OBSERVATION_NOTIFICATION_ID);
	}

	public void createNotification() {
		boolean tokenExpired = UserUtility.getInstance(getApplicationContext()).isTokenExpired();

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

		String noticationMsg = tokenExpired ? "Your token has expired, please tap to login." : "You are currently logged into MAGE.";

		Notification accountNotification = new NotificationCompat.Builder(this, MAGE_NOTIFICATION_CHANNEL_ID)
				.setOngoing(true)
				.setSortKey("1")
				.setContentTitle("MAGE")
				.setContentText(noticationMsg)
				.setGroup(MAGE_NOTIFICATION_GROUP)
				.setContentIntent(resultPendingIntent)
				.setSmallIcon(R.drawable.ic_wand_white_50dp)
				.addAction(R.drawable.ic_power_settings_new_white_24dp, "Logout", getLogoutPendingIntent())
				.build();

		NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle()
				.addLine(noticationMsg)
				.setBigContentTitle("MAGE");

		if (isReportingLocation()) {
			style.addLine("MAGE is currently reporting your location.");
		}

		// This summary notification supports "grouping" on versions older that Android.N
		Notification summaryNotification = new NotificationCompat.Builder(this, MAGE_NOTIFICATION_CHANNEL_ID)
				.setGroupSummary(true)
				.setGroup(MAGE_NOTIFICATION_GROUP)
				.setContentTitle("MAGE")
				.setContentText(noticationMsg)
				.setSmallIcon(R.drawable.ic_wand_white_50dp)
				.setStyle(style)
				.setContentIntent(resultPendingIntent)
				.addAction(R.drawable.ic_power_settings_new_white_24dp, "Logout", getLogoutPendingIntent())
				.build();

		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
		notificationManager.notify(MAGE_ACCOUNT_NOTIFICATION_ID, accountNotification);
		notificationManager.notify(MAGE_SUMMARY_NOTIFICATION_ID, summaryNotification);
	}

	private boolean isReportingLocation() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getResources().getString(R.string.reportLocationKey), getResources().getBoolean(R.bool.reportLocationDefaultValue));
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

	public void startLocationService() {
		if (locationReportingServiceIntent == null) {
			locationReportingServiceIntent = new Intent(getApplicationContext(), LocationReportingService.class);
			ContextCompat.startForegroundService(getApplicationContext(), locationReportingServiceIntent);

			// NOTE this can go away when we remove support for < Android.N
			// This will recreate the summary notification.
			createNotification();
		}
	}

	public void stopLocationService() {
		if (locationReportingServiceIntent != null) {
			stopService(locationReportingServiceIntent);
			locationReportingServiceIntent = null;

			// NOTE this can go away when we remove support for < Android.N
			// This will recreate the summary notification
			createNotification();
		}
	}
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equalsIgnoreCase(getString(R.string.reportLocationKey))) {
			boolean reportLocation = sharedPreferences.getBoolean(getString(R.string.reportLocationKey), getResources().getBoolean(R.bool.reportLocationDefaultValue));
			if (reportLocation) {
				startLocationService();
			} else {
				stopLocationService();
			}
		}
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
