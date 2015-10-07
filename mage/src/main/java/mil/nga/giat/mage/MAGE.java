package mil.nga.giat.mage;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.geopackage.features.index.FeatureIndexManager;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.io.GeoPackageIOUtils;
import mil.nga.geopackage.tiles.user.TileDao;
import mil.nga.geopackage.validate.GeoPackageValidate;
import mil.nga.giat.mage.login.LoginActivity;
import mil.nga.giat.mage.map.cache.CacheOverlay;
import mil.nga.giat.mage.map.cache.GeoPackageCacheOverlay;
import mil.nga.giat.mage.map.cache.GeoPackageFeatureTableCacheOverlay;
import mil.nga.giat.mage.map.cache.GeoPackageTableCacheOverlay;
import mil.nga.giat.mage.map.cache.GeoPackageTileTableCacheOverlay;
import mil.nga.giat.mage.map.cache.XYZDirectoryCacheOverlay;
import mil.nga.giat.mage.observation.ObservationNotificationListener;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.event.IUserEventListener;
import mil.nga.giat.mage.sdk.fetch.LocationFetchIntentService;
import mil.nga.giat.mage.sdk.fetch.ObservationFetchIntentService;
import mil.nga.giat.mage.sdk.fetch.StaticFeatureServerFetch;
import mil.nga.giat.mage.sdk.glide.MageDiskCache;
import mil.nga.giat.mage.sdk.glide.MageUrlLoader;
import mil.nga.giat.mage.sdk.http.client.HttpClientManager;
import mil.nga.giat.mage.sdk.http.post.MageServerPostRequests;
import mil.nga.giat.mage.sdk.location.LocationService;
import mil.nga.giat.mage.sdk.push.AttachmentPushService;
import mil.nga.giat.mage.sdk.push.LocationPushIntentService;
import mil.nga.giat.mage.sdk.push.ObservationPushIntentService;
import mil.nga.giat.mage.sdk.screen.ScreenChangeReceiver;
import mil.nga.giat.mage.sdk.utils.StorageUtility;
import mil.nga.giat.mage.sdk.utils.StorageUtility.StorageType;
import mil.nga.giat.mage.sdk.utils.UserUtility;
import mil.nga.giat.mage.wearable.InitializeMAGEWearBridge;
import mil.nga.wkb.geom.GeometryType;

public class MAGE extends MultiDexApplication implements IUserEventListener {

	private static final String LOG_NAME = MAGE.class.getName();

	public static final int MAGE_NOTIFICATION_ID = 1414;

	public interface OnCacheOverlayListener {
		public void onCacheOverlay(List<CacheOverlay> cacheOverlays);
	}

	private LocationService locationService;
	private Intent locationFetchIntent;
	private Intent observationFetchIntent;
	private Intent locationPushIntent;
	private Intent observationPushIntent;
	private List<CacheOverlay> cacheOverlays = null;
	private Collection<OnCacheOverlayListener> cacheOverlayListeners = new ArrayList<OnCacheOverlayListener>();

	private ObservationNotificationListener observationNotificationListener = null;
	private AttachmentPushService attachmentPushService = null;

	private StaticFeatureServerFetch staticFeatureServerFetch = null;

	@Override
	public void onCreate() {
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

		//set up Observation notifications
		observationNotificationListener = new ObservationNotificationListener(getApplicationContext());
		ObservationHelper.getInstance(getApplicationContext()).addListener(observationNotificationListener);

		// Start fetching and pushing observations and locations
		startFetching();
		startPushing();

		// Pull static layers and features just once
		loadStaticFeatures(false);

		InitializeMAGEWearBridge.startBridgeIfWearBuild(getApplicationContext());
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

	public void onLogout(Boolean clearTokenInformationAndSendLogoutRequest) {

		if (observationNotificationListener != null) {
			ObservationHelper.getInstance(getApplicationContext()).removeListener(observationNotificationListener);
		}

		destroyFetching();
		destroyPushing();
		destroyLocationService();
		destroyNotification();

		if(clearTokenInformationAndSendLogoutRequest) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					if(!MageServerPostRequests.logout(getApplicationContext())) {
						Log.e(LOG_NAME, "Unable to logout from server.");
					}
				}
			};
			new Thread(runnable).start();

			UserUtility.getInstance(getApplicationContext()).clearTokenInformation();
		}

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.remove(getApplicationContext().getString(mil.nga.giat.mage.sdk.R.string.currentEventKey)).commit();

		editor.putBoolean(getString(R.string.disclaimerAcceptedKey), false).commit();

		Boolean deleteAllDataOnLogout = sharedPreferences.getBoolean(getApplicationContext().getString(R.string.deleteAllDataOnLogoutKey), getResources().getBoolean(R.bool.deleteAllDataOnLogoutDefaultValue));

		if(deleteAllDataOnLogout) {
			LandingActivity.deleteAllData(getApplicationContext());
		}
	}

	private void destroyNotification(){
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
				.setSmallIcon(R.drawable.ic_wand)
				.setContentTitle("MAGE")
				.setOngoing(true)
				.setPriority(NotificationCompat.PRIORITY_MAX)
				.setContentText(notificationMsg)
				.addAction(R.drawable.ic_power_off_white, "Logout", getLogoutPendingIntent());

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
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		return PendingIntent.getActivity(getApplicationContext(), 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
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

	public void registerCacheOverlayListener(OnCacheOverlayListener listener) {
		cacheOverlayListeners.add(listener);
		if (cacheOverlays != null)
			listener.onCacheOverlay(cacheOverlays);
	}

	public void unregisterCacheOverlayListener(OnCacheOverlayListener listener) {
		cacheOverlayListeners.remove(listener);
	}

	public void refreshTileOverlays() {
		TileOverlaysTask task = new TileOverlaysTask(this);
		task.execute();
	}

	private void setCacheOverlays(List<CacheOverlay> cacheOverlays) {
		this.cacheOverlays = cacheOverlays;

		for (OnCacheOverlayListener listener : cacheOverlayListeners) {
			listener.onCacheOverlay(cacheOverlays);
		}
	}

	private class TileOverlaysTask extends AsyncTask<Void, Void, List<CacheOverlay>> {

		private Context context;

		public TileOverlaysTask(Context context){
			this.context = context;
		}

		@Override
		protected List<CacheOverlay> doInBackground(Void... params) {
			List<CacheOverlay> overlays = new ArrayList<CacheOverlay>();

			// Delete all external GeoPackage links
			GeoPackageManager geoPackageManager = GeoPackageFactory.getManager(context);
			geoPackageManager.deleteAllExternal();

			Map<StorageType, File> storageLocations = StorageUtility.getAllStorageLocations();
			for (File storageLocation : storageLocations.values()) {
				File root = new File(storageLocation, "MapCache");
				if (root.exists() && root.isDirectory() && root.canRead()) {

					for (File cache : root.listFiles()) {
						if(cache.canRead()) {

							// XYZ Directory
							if (cache.isDirectory()) {
								// found a cache
								overlays.add(new XYZDirectoryCacheOverlay(cache.getName(), cache));
							}
							// GeoPackage File
							else if(GeoPackageValidate.hasGeoPackageExtension(cache)){
								addGeoPackageCacheOverlays(context, overlays, cache, geoPackageManager);
							}
						}
					}
				}
			}

			// Set what should be enabled based on preferences.
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			Set<String> enabledOverlays = preferences.getStringSet(getString(R.string.tileOverlaysKey), Collections.<String>emptySet());

			// Determine which caches are enabled
			for (CacheOverlay cacheOverlay : overlays) {
				if (enabledOverlays.contains(cacheOverlay.getCacheName())) {
					cacheOverlay.setEnabled(true);
				}

				// If a child is enabled, enable the parent
				for (CacheOverlay childCache : cacheOverlay.getChildren()) {
					if (enabledOverlays.contains(childCache.getCacheName())) {
						childCache.setEnabled(true);
						cacheOverlay.setEnabled(true);
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

	/**
	 * Add GeoPackage Cache Overlays
     *
	 * @param context
	 * @param overlays
	 * @param cache
	 * @param geoPackageManager
	 */
	private void addGeoPackageCacheOverlays(Context context, List<CacheOverlay> overlays, File cache, GeoPackageManager geoPackageManager){
		// Import the GeoPackage as a linked file
		String cacheName = GeoPackageIOUtils.getFileNameWithoutExtension(cache);
		if(geoPackageManager.importGeoPackageAsExternalLink(cache, cacheName)){
			// Add the GeoPackage overlay
			GeoPackage geoPackage = geoPackageManager.open(cacheName);
			try {
				List<CacheOverlay> tables = new ArrayList<>();

				// GeoPackage tile tables
				List<String> tileTables = geoPackage.getTileTables();
				for (String tileTable : tileTables) {
					String tableCacheName = CacheOverlay.buildChildCacheName(cacheName, tileTable);
					TileDao tileDao = geoPackage.getTileDao(tileTable);
					int count = tileDao.count();
					int minZoom = (int) tileDao.getMinZoom();
					int maxZoom = (int) tileDao.getMaxZoom();
					GeoPackageTableCacheOverlay tableCache = new GeoPackageTileTableCacheOverlay(tileTable, cacheName, tableCacheName, count, minZoom, maxZoom);
					tables.add(tableCache);
				}

				// GeoPackage feature tables
				List<String> featureTables = geoPackage.getFeatureTables();
				for (String featureTable : featureTables) {
					String tableCacheName = CacheOverlay.buildChildCacheName(cacheName, featureTable);
					FeatureDao featureDao = geoPackage.getFeatureDao(featureTable);
					int count = featureDao.count();
					GeometryType geometryType = featureDao.getGeometryType();
					FeatureIndexManager indexer = new FeatureIndexManager(context, geoPackage, featureDao);
					boolean indexed = indexer.isIndexed();
					int minZoom = 0;
					if(indexed) {
						minZoom = featureDao.getZoomLevel() + getResources().getInteger(R.integer.geopackage_feature_tiles_min_zoom_offset);
						minZoom = Math.max(minZoom, 0);
						minZoom = Math.min(minZoom, GeoPackageFeatureTableCacheOverlay.MAX_ZOOM);
					}
					GeoPackageTableCacheOverlay tableCache = new GeoPackageFeatureTableCacheOverlay(featureTable, cacheName, tableCacheName, count, minZoom, indexed, geometryType);
					tables.add(tableCache);
				}

				// Add the GeoPackage overlay with child tables
				overlays.add(new GeoPackageCacheOverlay(cacheName, tables));
			}finally {
				geoPackage.close();
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
	}

}
