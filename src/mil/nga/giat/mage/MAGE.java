package mil.nga.giat.mage;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import mil.nga.giat.mage.file.Storage;
import mil.nga.giat.mage.file.Storage.StorageType;
import mil.nga.giat.mage.map.CacheOverlay;
import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.fetch.LocationFetchAlarmReceiver;
import mil.nga.giat.mage.sdk.fetch.LocationServerFetchAsyncTask;
import mil.nga.giat.mage.sdk.fetch.ObservationFetchAlarmReceiver;
import mil.nga.giat.mage.sdk.fetch.StaticFeatureServerFetch;
import mil.nga.giat.mage.sdk.glide.MageUrlLoader;
import mil.nga.giat.mage.sdk.location.LocationService;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import mil.nga.giat.mage.sdk.push.LocationServerPushAsyncTask;
import mil.nga.giat.mage.sdk.service.AttachmentAlarmReceiver;
import mil.nga.giat.mage.sdk.service.ObservationAlarmReceiver;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.bumptech.glide.Glide;

public class MAGE extends Application {

    private static final String LOG_NAME = MAGE.class.getName();
    
    AlarmManager alarm;

    public interface OnCacheOverlayListener {
        public void onCacheOverlay(List<CacheOverlay> cacheOverlays);
    }

    // TODO temp interface to start testing static overlays UI
    //
    //
    private List<Layer> featureOverlays = null;

    public interface OnStaticLayerListener {
        public void onStaticLayer(List<Layer> layers);
        public void onStaticLayerLoaded(Layer layer);
    }
    private Collection<OnStaticLayerListener> featureOverlayListeners = new ArrayList<OnStaticLayerListener>();
    
    public void registerStaticLayerListener(OnStaticLayerListener listener) {
        featureOverlayListeners.add(listener);
        if (featureOverlays != null)
            listener.onStaticLayer(featureOverlays);
    }

    public void unregisterStaticLayerListener(OnStaticLayerListener listener) {
        featureOverlayListeners.remove(listener);
    }
    
    private void setStaticOverlays(List<Layer> featureOverlays) {
        this.featureOverlays = featureOverlays;

        for (OnStaticLayerListener listener : featureOverlayListeners) {
            listener.onStaticLayer(featureOverlays);
        }
    }
    
    private void refreshStaticLayers() {
        StaticOverlaysTask task = new StaticOverlaysTask();
        task.execute();
    }
    
    private class StaticOverlaysTask extends AsyncTask<Void, Void, List<Layer>> {
        @Override
        protected List<Layer> doInBackground(Void... params) {
            List<Layer> overlays = new ArrayList<Layer>();

//            overlays.add(new Layer("12345", "static", "Features"));
//            overlays.add(new Layer("12345", "static", "Roads"));
//            overlays.add(new Layer("12345", "static", "Rivers"));

            try {
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            return overlays;
        }

        @Override
        protected void onPostExecute(List<Layer> result) {
            setStaticOverlays(result);
        }
    }
    
    //
    //
    // END temp UI code

    private LocationService locationService;
    private LocationServerFetchAsyncTask locationFetchTask = null;
    private LocationServerPushAsyncTask locationPushTask = null;
    private List<CacheOverlay> cacheOverlays = null;
    private Collection<OnCacheOverlayListener> cacheOverlayListeners = new ArrayList<OnCacheOverlayListener>();

    private StaticFeatureServerFetch staticFeatureServerFetch = null;

    @Override
    public void onCreate() {
    	alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
    	Glide.get().register(URL.class, new MageUrlLoader.Factory());
        refreshTileOverlays();
        
        // temp UI stuff
        refreshStaticLayers();
        
        super.onCreate();
    }
    
    public void onLogin() {
    	scheduleAlarms();
    	// Start location services
        initLocationService();

        // Start fetching and pushing observations and locations
        startFetching();
        startPushing();

        // Pull static layers and features just once
        loadStaticFeatures(false);
    }
    
    public void onLogout() {
    	cancelAlarms();
    	destroyFetching();
        destroyPushing();
        destroyLocationService();
    }
    
    public void scheduleAlarms() {
    	Log.i(LOG_NAME, "Scheduling alarms");
        scheduleAttachmentAlarm();
        scheduleObservationAlarm();
        scheduleObservationFetchAlarm();
        scheduleLocationFetchAlarm();
      }
    
    public void scheduleAttachmentAlarm() {
    	Log.i(LOG_NAME, "Scheduling new attachment alarm for every 60 seconds.");
        Intent intent = new Intent(getApplicationContext(), AttachmentAlarmReceiver.class);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(this, AttachmentAlarmReceiver.REQUEST_CODE,
            intent, PendingIntent.FLAG_UPDATE_CURRENT);
        long firstMillis = System.currentTimeMillis();
        int intervalMillis = 60000; 
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstMillis, intervalMillis, pendingIntent);
    }
    
    public void scheduleObservationAlarm() {
    	Log.i(LOG_NAME, "Scheduling new observation alarm.");
		Intent observationAlarmIntent = new Intent(getApplicationContext(), ObservationAlarmReceiver.class);
        final PendingIntent observationAlarmPendingIntent = PendingIntent.getBroadcast(this, ObservationAlarmReceiver.REQUEST_CODE,
        		observationAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarm.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), observationAlarmPendingIntent);
    }
    
    public void scheduleObservationFetchAlarm() {
    	long fetchFrequency = PreferenceHelper.getInstance(getApplicationContext()).getValue(R.string.observationFetchFrequencyKey, Long.class, R.string.observationFetchFrequencyDefaultValue);
    	Log.i(LOG_NAME, "Scheduling new observation fetch alarm for every " + (fetchFrequency/1000) + " seconds.");
        Intent intent = new Intent(getApplicationContext(), ObservationFetchAlarmReceiver.class);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(this, ObservationFetchAlarmReceiver.REQUEST_CODE,
            intent, PendingIntent.FLAG_UPDATE_CURRENT);
        long firstMillis = System.currentTimeMillis();
        alarm.setInexactRepeating(AlarmManager.RTC, firstMillis, fetchFrequency, pendingIntent);
    }
    
    public void scheduleLocationFetchAlarm() {
    	long fetchFrequency = PreferenceHelper.getInstance(getApplicationContext()).getValue(R.string.locationFetchFrequencyKey, Long.class, R.string.locationFetchFrequencyDefaultValue);
    	Log.i(LOG_NAME, "Scheduling new location fetch alarm for every " + (fetchFrequency/1000) + " seconds.");
        Intent intent = new Intent(getApplicationContext(), LocationFetchAlarmReceiver.class);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(this, LocationFetchAlarmReceiver.REQUEST_CODE,
            intent, PendingIntent.FLAG_UPDATE_CURRENT);
        long firstMillis = System.currentTimeMillis();
        alarm.setInexactRepeating(AlarmManager.RTC, firstMillis, fetchFrequency, pendingIntent);
    }
    
	public void cancelAlarms() {
		cancelAttachmentAlarm();
		cancelObservationAlarm();
		cancelObservationFetchAlarm();
		cancelLocationFetchAlarm();
	}
	
	private void cancelAttachmentAlarm() {
		Intent intent = new Intent(getApplicationContext(), AttachmentAlarmReceiver.class);
		final PendingIntent pIntent = PendingIntent.getBroadcast(this, AttachmentAlarmReceiver.REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		alarm.cancel(pIntent);
	}
	
	private void cancelObservationAlarm() {
		Intent intent = new Intent(getApplicationContext(), ObservationAlarmReceiver.class);
		final PendingIntent pIntent = PendingIntent.getBroadcast(this, ObservationAlarmReceiver.REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		alarm.cancel(pIntent);
	}
	
	private void cancelObservationFetchAlarm() {
		Intent intent = new Intent(getApplicationContext(), ObservationFetchAlarmReceiver.class);
		final PendingIntent pIntent = PendingIntent.getBroadcast(this, ObservationFetchAlarmReceiver.REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		alarm.cancel(pIntent);
	}
	
	private void cancelLocationFetchAlarm() {
		Intent intent = new Intent(getApplicationContext(), LocationFetchAlarmReceiver.class);
		final PendingIntent pIntent = PendingIntent.getBroadcast(this, LocationFetchAlarmReceiver.REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		alarm.cancel(pIntent);
	}

    public void initLocationService() {
        if (locationService == null) {
            locationService = new LocationService(getApplicationContext());
            locationService.init();
        }
    }

    public void destroyLocationService() {
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

            Map<StorageType, File> storageLocations = Storage.getAllStorageLocations();
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

    /**
     * Start Tasks responsible for fetching Observations and Locations from the server.
     */
    public void startFetching() {
        //locationFetchTask = new LocationServerFetchAsyncTask(getApplicationContext());
        //observationFetchTask = new ObservationServerFetchAsyncTask(getApplicationContext());
//        try {
//            //locationFetchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//            //observationFetchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//        } catch (Exception e) {
//            Log.e(LOG_NAME, "Error starting fetching tasks!");
//        }
    }

    /**
     * Stop Tasks responsible for fetching Observations and Locations from the server.
     */
    public void destroyFetching() {
//        if (locationFetchTask != null) {
//            locationFetchTask.destroy();
//        }

        if (staticFeatureServerFetch != null) {
        	staticFeatureServerFetch.destroy();
        }
    }

    /**
     * Start Tasks responsible for pushing Observations and Locations to the server.
     */
    public void startPushing() {
        locationPushTask = new LocationServerPushAsyncTask(getApplicationContext());
        
        try {
            locationPushTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            Log.e(LOG_NAME, "Error starting fetching tasks!");
        }
    }

    /**
     * Stop Tasks responsible for pushing Observations and Locations to the server.
     */
    public void destroyPushing() {
        if (locationPushTask != null) {
        	locationPushTask.destroy();
        }
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
}