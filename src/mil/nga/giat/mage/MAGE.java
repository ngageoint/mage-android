package mil.nga.giat.mage;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import mil.nga.giat.mage.file.Storage;
import mil.nga.giat.mage.file.Storage.StorageType;
import mil.nga.giat.mage.login.LoginActivity;
import mil.nga.giat.mage.map.CacheOverlay;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.event.IUserEventListener;
import mil.nga.giat.mage.sdk.fetch.LocationFetchAlarmReceiver;
import mil.nga.giat.mage.sdk.fetch.LocationServerFetchAsyncTask;
import mil.nga.giat.mage.sdk.fetch.ObservationFetchAlarmReceiver;
import mil.nga.giat.mage.sdk.fetch.StaticFeatureServerFetch;
import mil.nga.giat.mage.sdk.glide.MageUrlLoader;
import mil.nga.giat.mage.sdk.http.client.HttpClientManager;
import mil.nga.giat.mage.sdk.location.LocationService;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import mil.nga.giat.mage.sdk.push.LocationServerPushAsyncTask;
import mil.nga.giat.mage.sdk.service.AttachmentAlarmReceiver;
import mil.nga.giat.mage.sdk.service.ObservationAlarmReceiver;
import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.bumptech.glide.Glide;

public class MAGE extends Application implements IUserEventListener {

    private static final String LOG_NAME = MAGE.class.getName();
    
    AlarmManager alarm;
    
    public static final int MAGE_NOTIFICATION_ID = 1414;

    public interface OnCacheOverlayListener {
        public void onCacheOverlay(List<CacheOverlay> cacheOverlays);
    }
    
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
        
        HttpClientManager.getInstance(getApplicationContext()).addListener(this);
        
        super.onCreate();
    }
    
    public void onLogin() {
    	createNotification(false);
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
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(MAGE_NOTIFICATION_ID);
    }
    
    private void createNotification(boolean tokenExpired) {
    	// this line is some magic for kitkat
    	getLogoutPendingIntent().cancel();
        
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("MAGE")
                .setContentText(tokenExpired ? "Your token has expired, please tap to login." : "You are logged in. Slide down to logout.")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .addAction(R.drawable.ic_power_off_white, "Logout", getLogoutPendingIntent());
        
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
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                    0,
                    PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(MAGE.MAGE_NOTIFICATION_ID, builder.build());
    }
    
    private PendingIntent getLogoutPendingIntent() {
    	Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
    	intent.putExtra("LOGOUT", true);
        return PendingIntent.getActivity(getApplicationContext(), 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
    
    public void scheduleAlarms() {
    	Log.i(LOG_NAME, "Scheduling alarms");
        scheduleAttachmentAlarm();
//        scheduleObservationAlarm();
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

	@Override
	public void onError(Throwable error) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTokenExpired() {
		cancelAlarms();
    	destroyFetching();
        destroyPushing();
		createNotification(true);
	}
}