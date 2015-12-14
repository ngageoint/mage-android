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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.geopackage.features.index.FeatureIndexManager;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.tiles.user.TileDao;
import mil.nga.geopackage.validate.GeoPackageValidate;
import mil.nga.giat.mage.cache.GeoPackageCacheUtils;
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
import mil.nga.giat.mage.sdk.http.HttpClientManager;
import mil.nga.giat.mage.sdk.http.resource.UserResource;
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
		void onCacheOverlay(List<CacheOverlay> cacheOverlays);
	}

	public interface OnLogoutListener {
		void onLogout();
	}

	private LocationService locationService;
	private Intent locationFetchIntent;
	private Intent observationFetchIntent;
	private Intent locationPushIntent;
	private Intent observationPushIntent;
	private List<CacheOverlay> cacheOverlays = null;
	private Collection<OnCacheOverlayListener> cacheOverlayListeners = new ArrayList<>();

	private ObservationNotificationListener observationNotificationListener = null;
	private AttachmentPushService attachmentPushService = null;

	private StaticFeatureServerFetch staticFeatureServerFetch = null;

	@Override
	public void onCreate() {
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

	public void onLogout(Boolean clearTokenInformationAndSendLogoutRequest, final OnLogoutListener logoutListener) {

		if (observationNotificationListener != null) {
			ObservationHelper.getInstance(getApplicationContext()).removeListener(observationNotificationListener);
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

					UserUtility.getInstance(getApplicationContext()).clearTokenInformation();

					if (logoutListener != null) {
						logoutListener.onLogout();
					}
				}
			};

			new Thread(runnable).start();
		} else {
			if (logoutListener != null) {
				logoutListener.onLogout();
			}
		}

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.remove(getApplicationContext().getString(mil.nga.giat.mage.sdk.R.string.currentEventKey)).commit();

		editor.putBoolean(getString(R.string.disclaimerAcceptedKey), false).commit();

		Boolean deleteAllDataOnLogout = sharedPreferences.getBoolean(getApplicationContext().getString(R.string.deleteAllDataOnLogoutKey), getResources().getBoolean(R.bool.deleteAllDataOnLogoutDefaultValue));

		if (deleteAllDataOnLogout) {
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
		TileOverlaysTask task = new TileOverlaysTask(this, null);
		task.execute();
	}

	public void enableAndRefreshTileOverlays(String enableOverlayName) {
		List<String> overlayNames = new ArrayList<>();
		overlayNames.add(enableOverlayName);
		enableAndRefreshTileOverlays(overlayNames);
	}

	public void enableAndRefreshTileOverlays(Collection<String> enableOverlayNames) {
		TileOverlaysTask task = new TileOverlaysTask(this, enableOverlayNames);
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
		private Set<String> enable = new HashSet<>();

		public TileOverlaysTask(Context context, Collection<String> enable){
			this.context = context;
			if(enable != null) {
				this.enable.addAll(enable);
			}
		}

		@Override
		protected List<CacheOverlay> doInBackground(Void... params) {
			List<CacheOverlay> overlays = new ArrayList<CacheOverlay>();

			// Add the existing external GeoPackage databases as cache overlays
			GeoPackageManager geoPackageManager = GeoPackageFactory.getManager(context);
			addGeoPackageCacheOverlays(context, overlays, geoPackageManager);

			// Add each cache file or directory structure
			Map<StorageType, File> storageLocations = StorageUtility.getAllStorageLocations();
			for (File storageLocation : storageLocations.values()) {
				File root = new File(storageLocation, getString(R.string.overlay_cache_directory));
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
								GeoPackageCacheOverlay cacheOverlay = getGeoPackageCacheOverlay(context, cache, geoPackageManager);
								if(cacheOverlay != null){
									overlays.add(cacheOverlay);
								}
							}
						}
					}
				}
			}

			// Set what should be enabled based on preferences.
			boolean update = false;
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			Set<String> updatedEnabledOverlays = preferences.getStringSet(getString(R.string.tileOverlaysKey), Collections.<String>emptySet());
			Set<String> enabledOverlays = new HashSet<>();
			enabledOverlays.addAll(updatedEnabledOverlays);

			// Determine which caches are enabled
			for (CacheOverlay cacheOverlay : overlays) {

				// Check and enable the cache
				String cacheName = cacheOverlay.getCacheName();
				if (enabledOverlays.remove(cacheName)) {
					cacheOverlay.setEnabled(true);
				}

				// Check the child caches
				for (CacheOverlay childCache : cacheOverlay.getChildren()) {
					if (enabledOverlays.remove(childCache.getCacheName())) {
						childCache.setEnabled(true);
						cacheOverlay.setEnabled(true);
					}
				}

				// Check for new caches to enable in the overlays and preferences
				if(enable.contains(cacheName) && !cacheOverlay.isEnabled()){

					update = true;
					cacheOverlay.setEnabled(true);
					if(cacheOverlay.isSupportsChildren()){
						for (CacheOverlay childCache : cacheOverlay.getChildren()) {
							childCache.setEnabled(true);
							updatedEnabledOverlays.add(childCache.getCacheName());
						}
					}else{
						updatedEnabledOverlays.add(cacheName);
					}
				}

			}

			// Remove overlays in the preferences that no longer exist
			if(!enabledOverlays.isEmpty()){
				updatedEnabledOverlays.removeAll(enabledOverlays);
				update = true;
			}

			// If new enabled cache overlays, update them in the preferences
			if(update){
				SharedPreferences.Editor editor = preferences.edit();
				editor.putStringSet(getString(R.string.tileOverlaysKey), updatedEnabledOverlays);
				editor.apply();
			}

			return overlays;
		}

		@Override
		protected void onPostExecute(List<CacheOverlay> result) {
			setCacheOverlays(result);
		}
	}

    /**
     * Add GeoPackage Cache Overlay for the existing databases
     *
     * @param context
     * @param overlays
     * @param geoPackageManager
     */
    private void addGeoPackageCacheOverlays(Context context, List<CacheOverlay> overlays, GeoPackageManager geoPackageManager) {

        // Delete any GeoPackages where the file is no longer accessible
        geoPackageManager.deleteAllMissingExternal();

        // Add each existing database as a cache
        List<String> externalDatabases = geoPackageManager.externalDatabases();
        for (String database : externalDatabases) {
			GeoPackageCacheOverlay cacheOverlay = getGeoPackageCacheOverlay(context, geoPackageManager, database);
			if(cacheOverlay != null){
				overlays.add(cacheOverlay);
			}
        }
    }

    /**
     * Get GeoPackage Cache Overlay for the database file
     *
     * @param context
     * @param cache
     * @param geoPackageManager
	 * @return cache overlay
     */
    private GeoPackageCacheOverlay getGeoPackageCacheOverlay(Context context, File cache, GeoPackageManager geoPackageManager) {

		GeoPackageCacheOverlay cacheOverlay = null;

		// Import the GeoPackage if needed
		String cacheName = GeoPackageCacheUtils.importGeoPackage(geoPackageManager, cache);
		if(cacheName != null){
			// Get the GeoPackage overlay
			cacheOverlay = getGeoPackageCacheOverlay(context, geoPackageManager, cacheName);
		}

		return cacheOverlay;
    }

    /**
     * Get the GeoPackage database as a cache overlay
     *
     * @param context
     * @param geoPackageManager
     * @param database
	 * @return cache overlay
     */
    private GeoPackageCacheOverlay getGeoPackageCacheOverlay(Context context, GeoPackageManager geoPackageManager, String database) {

		GeoPackageCacheOverlay cacheOverlay = null;

        // Add the GeoPackage overlay
        GeoPackage geoPackage = geoPackageManager.open(database);
        try {
            List<CacheOverlay> tables = new ArrayList<>();

            // GeoPackage tile tables
            List<String> tileTables = geoPackage.getTileTables();
            for (String tileTable : tileTables) {
                String tableCacheName = CacheOverlay.buildChildCacheName(database, tileTable);
                TileDao tileDao = geoPackage.getTileDao(tileTable);
                int count = tileDao.count();
                int minZoom = (int) tileDao.getMinZoom();
                int maxZoom = (int) tileDao.getMaxZoom();
                GeoPackageTableCacheOverlay tableCache = new GeoPackageTileTableCacheOverlay(tileTable, database, tableCacheName, count, minZoom, maxZoom);
                tables.add(tableCache);
            }

            // GeoPackage feature tables
            List<String> featureTables = geoPackage.getFeatureTables();
            for (String featureTable : featureTables) {
                String tableCacheName = CacheOverlay.buildChildCacheName(database, featureTable);
                FeatureDao featureDao = geoPackage.getFeatureDao(featureTable);
                int count = featureDao.count();
                GeometryType geometryType = featureDao.getGeometryType();
                FeatureIndexManager indexer = new FeatureIndexManager(context, geoPackage, featureDao);
                boolean indexed = indexer.isIndexed();
                int minZoom = 0;
                if (indexed) {
                    minZoom = featureDao.getZoomLevel() + getResources().getInteger(R.integer.geopackage_feature_tiles_min_zoom_offset);
                    minZoom = Math.max(minZoom, 0);
                    minZoom = Math.min(minZoom, GeoPackageFeatureTableCacheOverlay.MAX_ZOOM);
                }
                GeoPackageTableCacheOverlay tableCache = new GeoPackageFeatureTableCacheOverlay(featureTable, database, tableCacheName, count, minZoom, indexed, geometryType);
                tables.add(tableCache);
            }

            // Create the GeoPackage overlay with child tables
			cacheOverlay = new GeoPackageCacheOverlay(database, tables);
        } finally {
            geoPackage.close();
        }

		return cacheOverlay;
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
