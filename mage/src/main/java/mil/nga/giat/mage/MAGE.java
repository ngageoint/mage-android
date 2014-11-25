package mil.nga.giat.mage;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.multidex.MultiDexApplication;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.model.GlideUrl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import mil.nga.giat.mage.login.LoginActivity;
import mil.nga.giat.mage.map.CacheOverlay;
import mil.nga.giat.mage.sdk.event.IUserEventListener;
import mil.nga.giat.mage.sdk.fetch.LocationFetchIntentService;
import mil.nga.giat.mage.sdk.fetch.ObservationFetchIntentService;
import mil.nga.giat.mage.sdk.fetch.StaticFeatureServerFetch;
import mil.nga.giat.mage.sdk.fetch.UserFetchIntentService;
import mil.nga.giat.mage.sdk.glide.MageDiskCache;
import mil.nga.giat.mage.sdk.glide.MageUrlLoader;
import mil.nga.giat.mage.sdk.http.client.HttpClientManager;
import mil.nga.giat.mage.sdk.location.LocationService;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import mil.nga.giat.mage.sdk.push.AttachmentPushAlarmReceiver;
import mil.nga.giat.mage.sdk.push.LocationPushIntentService;
import mil.nga.giat.mage.sdk.push.ObservationPushIntentService;
import mil.nga.giat.mage.sdk.screen.ScreenChangeReceiver;
import mil.nga.giat.mage.sdk.utils.StorageUtility;
import mil.nga.giat.mage.sdk.utils.StorageUtility.StorageType;
import mil.nga.giat.mage.sdk.utils.UserUtility;

public class MAGE extends MultiDexApplication implements IUserEventListener {

	private static final String LOG_NAME = MAGE.class.getName();

	private AlarmManager alarm;

	public static final int MAGE_NOTIFICATION_ID = 1414;

	public interface OnCacheOverlayListener {
		public void onCacheOverlay(List<CacheOverlay> cacheOverlays);
	}

	private LocationService locationService;
	private Intent locationFetchIntent;
	private Intent observationFetchIntent;
	private Intent userFetchIntent;
	private Intent locationPushIntent;
	private Intent observationPushIntent;
	private Intent attachmentPushIntent;
	private List<CacheOverlay> cacheOverlays = null;
	private Collection<OnCacheOverlayListener> cacheOverlayListeners = new ArrayList<OnCacheOverlayListener>();

	private StaticFeatureServerFetch staticFeatureServerFetch = null;

	@Override
	public void onCreate() {
		alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
		try {
			Glide.setup(new GlideBuilder(getApplicationContext()).setDiskCache(new MageDiskCache(getApplicationContext())));
			Glide.get(getApplicationContext()).register(GlideUrl.class, InputStream.class, new MageUrlLoader.Factory());
		} catch (IOException e) {
			Log.e(LOG_NAME, "Unable to create Mage disk cache", e);
		}
		refreshTileOverlays();

		// setup the screen unlock stuff
		registerReceiver(ScreenChangeReceiver.getInstance(), new IntentFilter(Intent.ACTION_SCREEN_ON));
		
		HttpClientManager.getInstance(getApplicationContext()).addListener(this);

		super.onCreate();
	}

	public void onLogin() {
		createNotification();
		// Start location services
		initLocationService();

		// Start fetching and pushing observations and locations
		startFetching();
		startPushing();

		// Pull static layers and features just once
		loadStaticFeatures(false);
	}
	
	public void loadStaticFeatures(final boolean force) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				staticFeatureServerFetch = new StaticFeatureServerFetch(getApplicationContext());
				try {
					staticFeatureServerFetch.fetch(force);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		new Thread(runnable).start();
	}

	public void onLogout() {
		destroyFetching();
		destroyPushing();
		destroyLocationService();
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(MAGE_NOTIFICATION_ID);
		
		if(PreferenceHelper.getInstance(getApplicationContext()).getValue(R.string.deleteAllDataOnLogoutKey, Boolean.class, R.string.deleteAllDataOnLogoutDefaultValue)) {
			LandingActivity.deleteAllData(getApplicationContext());
		}		
	}

	private void createNotification() {
		// this line is some magic for kitkat
		getLogoutPendingIntent().cancel();
		boolean tokenExpired = UserUtility.getInstance(getApplicationContext()).isTokenExpired();

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_launcher).setContentTitle("MAGE").setContentText(tokenExpired ? "Your token has expired, please tap to login." : "You are logged in. Slide down to logout.").setOngoing(true)
				.setPriority(NotificationCompat.PRIORITY_MAX).addAction(R.drawable.ic_power_off_white, "Logout", getLogoutPendingIntent());

		NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
		bigTextStyle.setBigContentTitle("MAGE");
		bigTextStyle.bigText(tokenExpired ? "Your token has expired, please tap to login." : "You are logged in.  Tap to open MAGE.");
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
		notificationManager.notify(MAGE.MAGE_NOTIFICATION_ID, builder.build());
	}

	private PendingIntent getLogoutPendingIntent() {
		Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
		intent.putExtra("LOGOUT", true);
		return PendingIntent.getActivity(getApplicationContext(), 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	/**
	 * Start Tasks responsible for fetching Observations and Locations from the server.
	 */
	private void startFetching() {
		if (userFetchIntent == null) {
			userFetchIntent = new Intent(getApplicationContext(), UserFetchIntentService.class);
			startService(userFetchIntent);
		}
		
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
		if (userFetchIntent != null) {
			stopService(userFetchIntent);
			userFetchIntent = null;
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
		startAttachmentPushing();
	}

	private void startAttachmentPushing() {
		if(attachmentPushIntent == null) {
			attachmentPushIntent = new Intent(getApplicationContext(), AttachmentPushAlarmReceiver.class);
			final PendingIntent pendingIntent = PendingIntent.getBroadcast(this, AttachmentPushAlarmReceiver.REQUEST_CODE, attachmentPushIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			long firstMillis = System.currentTimeMillis();
			int intervalMillis = 60000;
			alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstMillis, intervalMillis, pendingIntent);
		}
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
		cancelAttachmentPush();
	}
	
	private void cancelAttachmentPush() {
		Intent intent = new Intent(getApplicationContext(), AttachmentPushAlarmReceiver.class);
		final PendingIntent pIntent = PendingIntent.getBroadcast(this, AttachmentPushAlarmReceiver.REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		alarm.cancel(pIntent);

		if(attachmentPushIntent != null) {
			this.stopService(attachmentPushIntent);
			attachmentPushIntent = null;
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

	public void registerCacheOverlayListener(OnCacheOverlayListener listener) {
		cacheOverlayListeners.add(listener);
		if (cacheOverlays != null)
			listener.onCacheOverlay(cacheOverlays);
	}

	public void unregisterCacheOverlayListener(OnCacheOverlayListener listener) {
		cacheOverlayListeners.remove(listener);
	}

	public void refreshTileOverlays() {
		TileOverlaysTask task = new TileOverlaysTask();
		task.execute();
	}

	private void setCacheOverlays(List<CacheOverlay> cacheOverlays) {
		this.cacheOverlays = cacheOverlays;

		for (OnCacheOverlayListener listener : cacheOverlayListeners) {
			listener.onCacheOverlay(cacheOverlays);
		}
	}

	private class TileOverlaysTask extends AsyncTask<Void, Void, List<CacheOverlay>> {
		@Override
		protected List<CacheOverlay> doInBackground(Void... params) {
			List<CacheOverlay> overlays = new ArrayList<CacheOverlay>();

			Map<StorageType, File> storageLocations = StorageUtility.getAllStorageLocations();
			for (File storageLocation : storageLocations.values()) {
				File root = new File(storageLocation, "MapCache");
				if (root.exists() && root.isDirectory() && root.canRead()) {
					for (File cache : root.listFiles()) {
						if (cache.isDirectory() && cache.canRead()) {
							// found a cache
							overlays.add(new CacheOverlay(cache.getName(), cache));
						}
					}
				}
			}

			return overlays;
		}

		@Override
		protected void onPostExecute(List<CacheOverlay> result) {
			setCacheOverlays(result);
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
	}
}
