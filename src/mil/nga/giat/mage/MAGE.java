package mil.nga.giat.mage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import mil.nga.giat.mage.file.Storage;
import mil.nga.giat.mage.file.Storage.StorageType;
import mil.nga.giat.mage.map.CacheOverlay;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.fetch.LocationServerFetchAsyncTask;
import mil.nga.giat.mage.sdk.fetch.ObservationServerFetchAsyncTask;
import mil.nga.giat.mage.sdk.fetch.StaticFeatureServerFetch;
import mil.nga.giat.mage.sdk.location.LocationService;
import mil.nga.giat.mage.sdk.push.ObservationServerPushAsyncTask;
import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

public class MAGE extends Application {

    private static final String LOG_NAME = MAGE.class.getName();

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

            overlays.add(new Layer("12345", "static", "Features"));
            overlays.add(new Layer("12345", "static", "Roads"));
            overlays.add(new Layer("12345", "static", "Rivers"));

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
    private LocationServerFetchAsyncTask locationTask = null;
    private ObservationServerFetchAsyncTask observationFetchTask = null;
    private ObservationServerPushAsyncTask observationPushTask = null;
    private List<CacheOverlay> cacheOverlays = null;
    private Collection<OnCacheOverlayListener> cacheOverlayListeners = new ArrayList<OnCacheOverlayListener>();

    private StaticFeatureServerFetch staticFeatureServerFetch = null;

    @Override
    public void onCreate() {
        refreshTileOverlays();
        
        // temp UI stuff
        refreshStaticLayers();
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
        locationTask = new LocationServerFetchAsyncTask(getApplicationContext());
        observationFetchTask = new ObservationServerFetchAsyncTask(getApplicationContext());
        try {
            locationTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            observationFetchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            Log.e(LOG_NAME, "Error starting fetching tasks!");
        }
    }

    public void destroyFetching() {
        if (locationTask != null) {
            locationTask.destroy();
        }

        if (observationFetchTask != null) {
            observationFetchTask.destroy();
        }
    }

    public void startPushing() {
        observationPushTask = new ObservationServerPushAsyncTask(getApplicationContext());
        try {
            observationPushTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            Log.e(LOG_NAME, "Error starting fetching tasks!");
        }
    }

    public void destroyPushing() {
        if (observationPushTask != null) {
            observationPushTask.destroy();
        }
    }

    // FIXME : testing this stuff!
    public void testingStaticFeatures() {

        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                staticFeatureServerFetch = new StaticFeatureServerFetch(getApplicationContext());
                try {
                    staticFeatureServerFetch.fetch();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        new Thread(runnable).start();
    }
}