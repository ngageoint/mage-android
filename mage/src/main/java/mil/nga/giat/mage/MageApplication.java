package mil.nga.giat.mage;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.work.Configuration;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;
import mil.nga.giat.mage.feed.FeedFetchService;
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
public class MageApplication extends Application implements Configuration.Provider, LifecycleObserver, SharedPreferences.OnSharedPreferenceChangeListener, ISessionEventListener, Application.ActivityLifecycleCallbacks {

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

	private ObservationNotificationListener observationNotificationListener = null;

	private Activity runningActivity;

	@Inject
	HiltWorkerFactory workerFactory;

	@NonNull
	@Override
	public Configuration getWorkManagerConfiguration() {
		return new Configuration.Builder()
				.setWorkerFactory(workerFactory)
				.build();
	}

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
		//set up observation notifications
		if (observationNotificationListener == null) {
			observationNotificationListener = new ObservationNotificationListener(getApplicationContext());
			ObservationHelper.getInstance(getApplicationContext()).addListener(observationNotificationListener);
		}

		// Start fetching and pushing observations and locations
		startPushing();
		startFetching();

		ObservationFetchWorker.Companion.beginWork(getApplicationContext());

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

		ObservationFetchWorker.Companion.stopWork(getApplicationContext());

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
	}

	private void destroyNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(MAGE_SUMMARY_NOTIFICATION_ID);
		notificationManager.cancel(MAGE_ACCOUNT_NOTIFICATION_ID);
		notificationManager.cancel(ObservationNotificationListener.OBSERVATION_NOTIFICATION_ID);
	}

	private void startFetching() {
		startService(new Intent(getApplicationContext(), LocationFetchService.class));
		startService(new Intent(getApplicationContext(), ObservationFetchService.class));
		startService(new Intent(getApplicationContext(), FeedFetchService.class));
	}

	/**
	 * Stop Tasks responsible for fetching Observations and Locations from the server.
	 */
	private void destroyFetching() {
		stopService(new Intent(getApplicationContext(), LocationFetchService.class));
		stopService(new Intent(getApplicationContext(), ObservationFetchService.class));
		stopService(new Intent(getApplicationContext(), FeedFetchService.class));
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
		Intent intent = new Intent(getApplicationContext(), LocationReportingService.class);
		ContextCompat.startForegroundService(getApplicationContext(), intent);
	}

	public void stopLocationService() {
		Intent intent = new Intent(getApplicationContext(), LocationReportingService.class);
		stopService(intent);
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

		ObservationFetchWorker.Companion.stopWork(getApplicationContext());

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
