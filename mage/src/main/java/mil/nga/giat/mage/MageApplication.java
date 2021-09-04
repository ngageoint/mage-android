package mil.nga.giat.mage;

import android.annotation.SuppressLint;
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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import dagger.hilt.android.HiltAndroidApp;
import mil.nga.giat.mage.location.LocationFetchService;
import mil.nga.giat.mage.location.LocationReportingService;
import mil.nga.giat.mage.login.AccountStateActivity;
import mil.nga.giat.mage.login.LoginActivity;
import mil.nga.giat.mage.login.ServerUrlActivity;
import mil.nga.giat.mage.login.SignupActivity;
import mil.nga.giat.mage.login.idp.IdpLoginActivity;
import mil.nga.giat.mage.observation.ObservationNotificationListener;
import mil.nga.giat.mage.observation.sync.AttachmentPushService;
import mil.nga.giat.mage.observation.sync.ObservationFetchService;
import mil.nga.giat.mage.observation.sync.ObservationFetchWorker;
import mil.nga.giat.mage.observation.sync.ObservationPushService;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeatureHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.event.ISessionEventListener;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.fetch.ImageryServerFetch;
import mil.nga.giat.mage.sdk.fetch.StaticFeatureServerFetch;
import mil.nga.giat.mage.sdk.http.HttpClientManager;
import mil.nga.giat.mage.sdk.http.resource.UserResource;
import mil.nga.giat.mage.sdk.screen.ScreenChangeReceiver;
import mil.nga.giat.mage.sdk.utils.UserUtility;
import mil.nga.giat.mage.wearable.InitializeMAGEWearBridge;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltAndroidApp
public class MageApplication extends Application implements LifecycleObserver, SharedPreferences.OnSharedPreferenceChangeListener, ISessionEventListener, Application.ActivityLifecycleCallbacks {

	private static final String LOG_NAME = MageApplication.class.getName();

	public static final int MAGE_SUMMARY_NOTIFICATION_ID = 100;
	public static final int MAGE_ACCOUNT_NOTIFICATION_ID = 101;
	public static final int MAGE_OBSERVATION_NOTIFICATION_PREFIX = 10000;


	public static final String MAGE_NOTIFICATION_GROUP = "mil.nga.mage.MAGE_NOTIFICATION_GROUP";
	public static final String MAGE_OBSERVATION_NOTIFICATION_GROUP = "mil.nga.mage.MAGE_OBSERVATION_NOTIFICATION_GROUP";
	public static final String MAGE_NOTIFICATION_CHANNEL_ID = "mil.nga.mage.MAGE_NOTIFICATION_CHANNEL";
	public static final String MAGE_OBSERVATION_NOTIFICATION_CHANNEL_ID = "mil.nga.mage.MAGE_OBSERVATION_NOTIFICATION_CHANNEL";

	public interface OnLogoutListener {
		void onLogout();
	}

	private Intent observationPushServiceIntent;
	private Intent attachmentPushServiceIntent;

	private Intent locationReportingServiceIntent;

	private ObservationNotificationListener observationNotificationListener = null;

	private Activity runningActivity;

	@Override
	public void onCreate() {
		super.onCreate();

		//This ensures the singleton is created with the correct context, which needs to be the
		//application context
		DaoStore.getInstance(this.getApplicationContext());
		LayerHelper.getInstance(this.getApplicationContext());
		StaticFeatureHelper.getInstance(this.getApplicationContext());

		ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

		HttpClientManager.initialize(this);

		// setup the screen unlock stuff
		registerReceiver(ScreenChangeReceiver.getInstance(), new IntentFilter(Intent.ACTION_SCREEN_ON));

		registerActivityLifecycleCallbacks(this);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		int dayNightTheme = preferences.getInt(getResources().getString(R.string.dayNightThemeKey), getResources().getInteger(R.integer.dayNightThemeDefaultValue));
		AppCompatDelegate.setDefaultNightMode(dayNightTheme);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			NotificationChannel channel = new NotificationChannel(MAGE_NOTIFICATION_CHANNEL_ID,"MAGE", NotificationManager.IMPORTANCE_LOW);
			channel.setShowBadge(true);
			notificationManager.createNotificationChannel(channel);

			NotificationChannel observationChannel = new NotificationChannel(MAGE_OBSERVATION_NOTIFICATION_CHANNEL_ID,"MAGE Observations", NotificationManager.IMPORTANCE_HIGH);
			observationChannel.setShowBadge(true);
			notificationManager.createNotificationChannel(observationChannel);
		}
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_START)
	public void onApplicationStart() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		preferences.registerOnSharedPreferenceChangeListener(this);

		HttpClientManager.getInstance().addListener(this);

		// Start fetching and pushing observations and locations
		if (!UserUtility.getInstance(getApplicationContext()).isTokenExpired()) {
			startPushing();
			startFetching();
		}
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_STOP)
	public void onApplicationStop() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		preferences.registerOnSharedPreferenceChangeListener(this);

		HttpClientManager.getInstance().removeListener(this);

		destroyFetching();
		destroyPushing();
	}

	public void onLogin() {
		createNotification();

		//set up observation notifications
		if (observationNotificationListener == null) {
			observationNotificationListener = new ObservationNotificationListener(getApplicationContext());
			ObservationHelper.getInstance(getApplicationContext()).addListener(observationNotificationListener);
		}

		// Start fetching and pushing observations and locations
		startPushing();
		startFetching();

		ObservationFetchWorker.Companion.beginWork();

		// Pull static layers and features just once
		loadOnlineAndOfflineLayers(false, null);

		InitializeMAGEWearBridge.startBridgeIfWearBuild(getApplicationContext());
	}

	private void loadOnlineAndOfflineLayers(final boolean force, final StaticFeatureServerFetch.OnStaticLayersListener listener) {
		@SuppressLint("StaticFieldLeak") AsyncTask<Void, Void, Void> fetcher = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... voids) {
				StaticFeatureServerFetch staticFeatureServerFetch = new StaticFeatureServerFetch(getApplicationContext());
				try {
					staticFeatureServerFetch.fetch(force, listener);
				} catch (Exception e) {
					e.printStackTrace();
				}

				try {
					ImageryServerFetch imageryServerFetch = new ImageryServerFetch(getApplicationContext());
					imageryServerFetch.fetch();
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
		} ;
		fetcher.execute();
	}

	public void onLogout(Boolean clearTokenInformationAndSendLogoutRequest, final OnLogoutListener logoutListener) {

		if (observationNotificationListener != null) {
			ObservationHelper.getInstance(getApplicationContext()).removeListener(observationNotificationListener);
			observationNotificationListener = null;
		}

		destroyFetching();
		destroyNotification();
		stopLocationService();

		ObservationFetchWorker.Companion.stopWork();

		if (clearTokenInformationAndSendLogoutRequest) {
			UserResource userResource = new UserResource(getApplicationContext());
			userResource.logout(new Callback<ResponseBody>() {
				@Override
				public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
					if (logoutListener != null) {
						logoutListener.onLogout();
					}
				}

				@Override
				public void onFailure(Call<ResponseBody> call, Throwable t) {
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

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean(getString(R.string.disclaimerAcceptedKey), false).apply();

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

	private void startFetching() {
		startService(new Intent(getApplicationContext(), LocationFetchService.class));
		startService(new Intent(getApplicationContext(), ObservationFetchService.class));
	}

	/**
	 * Stop Tasks responsible for fetching Observations and Locations from the server.
	 */
	private void destroyFetching() {
		stopService(new Intent(getApplicationContext(), LocationFetchService.class));
		stopService(new Intent(getApplicationContext(), ObservationFetchService.class));
	}

	/**
	 * Start Tasks responsible for pushing Observations and Attachments to the server.
	 */
	private void startPushing() {
		if (observationPushServiceIntent == null) {
			observationPushServiceIntent = new Intent(getApplicationContext(), ObservationPushService.class);
			startService(observationPushServiceIntent);
		}

		if (attachmentPushServiceIntent == null) {
			attachmentPushServiceIntent = new Intent(getApplicationContext(), AttachmentPushService.class);
			startService(attachmentPushServiceIntent);
		}
	}

	/**
	 * Stop Tasks responsible for pushing Observations and Attachments to the server.
	 */
	private void destroyPushing() {
		if (observationPushServiceIntent != null) {
			stopService(observationPushServiceIntent);
			observationPushServiceIntent = null;
		}

		if (attachmentPushServiceIntent != null) {
			stopService(attachmentPushServiceIntent);
			attachmentPushServiceIntent = null;
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
		if (getString(R.string.reportLocationKey).equalsIgnoreCase(key) && !UserUtility.getInstance(getApplicationContext()).isTokenExpired()) {
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
		invalidateSession(runningActivity, true);
	}

	@Override
	public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

	}

	@Override
	public void onActivityStarted(Activity activity) {

	}

	@Override
	public void onActivityResumed(Activity activity) {
		if (UserUtility.getInstance(getApplicationContext()).isTokenExpired()) {
			invalidateSession(activity, false);
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

	private void invalidateSession(Activity activity, Boolean applicationInUse) {
		destroyFetching();
		destroyPushing();
		createNotification();

		ObservationFetchWorker.Companion.stopWork();

		// TODO JWT where else is disclaimer accepted set to false.
		// Why not set to false if activity resumed onActivityResumed and token is invalid?
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean(getString(R.string.disclaimerAcceptedKey), false).apply();

		if (!(activity instanceof LoginActivity) &&
				!(activity instanceof IdpLoginActivity) &&
				!(activity instanceof AccountStateActivity) &&
				!(activity instanceof SignupActivity) &&
				!(activity instanceof ServerUrlActivity)) {
			forceLogin(applicationInUse);
		}
	}

	private void forceLogin(boolean applicationInUse) {
		Intent intent = new Intent(this, LoginActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(LoginActivity.EXTRA_CONTINUE_SESSION, true);
		intent.putExtra(LoginActivity.EXTRA_CONTINUE_SESSION_WHILE_USING, applicationInUse);

		startActivity(intent);
	}

}
