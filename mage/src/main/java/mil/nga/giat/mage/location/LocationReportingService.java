package mil.nga.giat.mage.location;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.arch.lifecycle.LifecycleService;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.R;

public class LocationReportingService extends LifecycleService implements
        Observer<Location>,
        LocationSaveTask.LocationDatabaseListener,
        LocationPushTask.LocationSyncListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String LOG_NAME = LocationReportingService.class.getName();
    public static final String NOTIFICATION_CHANNEL_ID = "mil.nga.mage.LOCATION_NOTIFICATION_CHANNEL";

    private static final int NOTIFICATION_ID = 500;

    public static final Executor SAVE_EXECUTOR = Executors.newSingleThreadExecutor();
    public static final Executor PUSH_EXECUTOR = new ThreadPoolExecutor(1, 1, 1L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadPoolExecutor.DiscardPolicy());

    private SharedPreferences preferences;
    private LiveData<Location> locationLiveData;

    private boolean shouldReportLocation;
    private long locationPushFrequency;
    private long oldestLocationTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.v(LOG_NAME, "onCreate LocationService");

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        preferences.registerOnSharedPreferenceChangeListener(this);

        locationPushFrequency = getLocationPushFrequency();
        shouldReportLocation = getShouldReportLocation();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,"MAGE", NotificationManager.IMPORTANCE_MIN);
            channel.setShowBadge(true);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("MAGE Location Service")
                .setContentText("MAGE is currently reporting your location.")
                .setSmallIcon(R.drawable.ic_place_white_24dp)
                .setGroup(MAGE.MAGE_NOTIFICATION_GROUP)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        locationLiveData = ((MAGE) getApplication()).getLocationLiveData();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.v(LOG_NAME, "onStartCommand received from service, this service has been restarted");
        locationLiveData.observe(this, this);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.v(LOG_NAME, "onDestroy received from service, this service is being destroyed");

        locationLiveData.removeObserver(this);

        preferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onChanged(@Nullable Location location) {
        if (shouldReportLocation && location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            Log.v(LOG_NAME, "GPS location changed");
            new LocationSaveTask(getApplicationContext(), LocationReportingService.this).executeOnExecutor(SAVE_EXECUTOR, location);
        }
    }

    @Override
    public void onSaveComplete(Location location) {
        if (location.getTime() - oldestLocationTime > locationPushFrequency) {
            new LocationPushTask(getApplicationContext(), this).executeOnExecutor(PUSH_EXECUTOR);
        }

        if (oldestLocationTime == 0) {
            oldestLocationTime = location.getTime();
        }
    }

    @Override
    public void onSyncComplete(Boolean status) {
        if (status) {
            oldestLocationTime = 0;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equalsIgnoreCase(getString(R.string.reportLocationKey))) {
            shouldReportLocation = getShouldReportLocation();
            Log.d(LOG_NAME, "Report location changed " + shouldReportLocation);
        } else if (key.equalsIgnoreCase(getString(R.string.locationPushFrequencyKey))) {
            locationPushFrequency = getLocationPushFrequency();
            Log.d(LOG_NAME, "Location push frequency changed " + locationPushFrequency);
        }
    }

    private long getLocationPushFrequency() {
        return preferences.getInt(getString(R.string.locationPushFrequencyKey), getResources().getInteger(R.integer.locationPushFrequencyDefaultValue));
    }

    private boolean getShouldReportLocation() {
        return preferences.getBoolean(getString(R.string.reportLocationKey), getResources().getBoolean(R.bool.reportLocationDefaultValue));
    }
}
