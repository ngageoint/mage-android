package mil.nga.giat.mage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import mil.nga.giat.mage.file.Storage;
import mil.nga.giat.mage.file.Storage.StorageType;
import mil.nga.giat.mage.map.CacheOverlay;
import mil.nga.giat.mage.sdk.fetch.RoleServerFetchAsyncTask;
import mil.nga.giat.mage.sdk.location.LocationService;
import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

public class MAGE extends Application {

    private static final String LOG_NAME = MAGE.class.getName();

    public interface OnCacheOverlayListener {
        public void onCacheOverlay(List<CacheOverlay> cacheOverlays);
    }

    private LocationService locationService;
    private RoleServerFetchAsyncTask roleTask;
    private List<CacheOverlay> cacheOverlays = null;
    private Collection<OnCacheOverlayListener> cacheOverlayListeners = new ArrayList<OnCacheOverlayListener>();

    @Override
    public void onCreate() {
        refreshTileOverlays();
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

    public void startFetching() {
        roleTask = new RoleServerFetchAsyncTask(getApplicationContext(), true);
        try {
            roleTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            Log.e(LOG_NAME, "Error fetching!  Could not populate role table!");
        }
    }

    public void destroyFetching() {
        roleTask.destroy();
    }

}