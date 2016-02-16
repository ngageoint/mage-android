package mil.nga.giat.mage.map.cache;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
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
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.cache.CacheUtils;
import mil.nga.giat.mage.cache.GeoPackageCacheUtils;
import mil.nga.giat.mage.sdk.utils.StorageUtility;
import mil.nga.wkb.geom.GeometryType;

/**
 * Created by wnewman on 2/11/16.
 */
public class CacheProvider {

    private static final String LOG_NAME = CacheProvider.class.getName();

    private Context context;

    private static CacheProvider instance = null;

    protected CacheProvider(Context context) {
        this.context = context;
    }

    public static CacheProvider getInstance(Context context) {
        if (instance == null) {
            instance = new CacheProvider(context);
        }

        return instance;
    }

    public interface OnCacheOverlayListener {
        void onCacheOverlay(List<CacheOverlay> cacheOverlays);
    }

    private List<CacheOverlay> cacheOverlays = null;
    private Collection<OnCacheOverlayListener> cacheOverlayListeners = new ArrayList<>();

    public void registerCacheOverlayListener(OnCacheOverlayListener listener) {
        cacheOverlayListeners.add(listener);
        if (cacheOverlays != null)
            listener.onCacheOverlay(cacheOverlays);
    }

    public void unregisterCacheOverlayListener(OnCacheOverlayListener listener) {
        cacheOverlayListeners.remove(listener);
    }

    public void refreshTileOverlays() {
        TileOverlaysTask task = new TileOverlaysTask(null);
        task.execute();
    }

    public void enableAndRefreshTileOverlays(String enableOverlayName) {
        List<String> overlayNames = new ArrayList<>();
        overlayNames.add(enableOverlayName);
        enableAndRefreshTileOverlays(overlayNames);
    }

    public void enableAndRefreshTileOverlays(Collection<String> enableOverlayNames) {
        TileOverlaysTask task = new TileOverlaysTask(enableOverlayNames);
        task.execute();
    }

    private void setCacheOverlays(List<CacheOverlay> cacheOverlays) {
        this.cacheOverlays = cacheOverlays;

        for (OnCacheOverlayListener listener : cacheOverlayListeners) {
            listener.onCacheOverlay(cacheOverlays);
        }
    }

    private class TileOverlaysTask extends AsyncTask<Void, Void, List<CacheOverlay>> {

        private Set<String> enable = new HashSet<>();

        public TileOverlaysTask(Collection<String> enable){
            if(enable != null) {
                this.enable.addAll(enable);
            }
        }

        @Override
        protected List<CacheOverlay> doInBackground(Void... params) {
            List<CacheOverlay> overlays = new ArrayList<>();

            // Add the existing external GeoPackage databases as cache overlays
            GeoPackageManager geoPackageManager = GeoPackageFactory.getManager(context);
            addGeoPackageCacheOverlays(context, overlays, geoPackageManager);

            // Get public external caches stored in /MapCache folder
            Map<StorageUtility.StorageType, File> storageLocations = StorageUtility.getReadableStorageLocations();
            for (File storageLocation : storageLocations.values()) {
                File root = new File(storageLocation, context.getString(R.string.overlay_cache_directory));
                if (root.exists() && root.isDirectory() && root.canRead()) {
                    for (File cache : root.listFiles()) {
                        if(cache.canRead()) {
                            if (cache.isDirectory()) {
                                // found a cache
                                overlays.add(new XYZDirectoryCacheOverlay(cache.getName(), cache));
                            } else if (GeoPackageValidate.hasGeoPackageExtension(cache)) {
                                GeoPackageCacheOverlay cacheOverlay = getGeoPackageCacheOverlay(context, cache, geoPackageManager);
                                if (cacheOverlay != null) {
                                    overlays.add(cacheOverlay);
                                }
                            }
                        }
                    }
                }
            }

            // Check internal/external application storage
            File applicationCacheDirectory = CacheUtils.getApplicationCacheDirectory(context);
            if (applicationCacheDirectory != null && applicationCacheDirectory.exists()) {
                for (File cache : applicationCacheDirectory.listFiles()) {
                    if (GeoPackageValidate.hasGeoPackageExtension(cache)) {
                        GeoPackageCacheOverlay cacheOverlay = getGeoPackageCacheOverlay(context, cache, geoPackageManager);
                        if (cacheOverlay != null) {
                            overlays.add(cacheOverlay);
                        }
                    }
                }
            }

            // Set what should be enabled based on preferences.
            boolean update = false;
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            Set<String> updatedEnabledOverlays = new HashSet<>();
            updatedEnabledOverlays.addAll(preferences.getStringSet(context.getString(R.string.tileOverlaysKey), Collections.<String>emptySet()));
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
                if (enable.contains(cacheName) && !cacheOverlay.isEnabled()) {

                    update = true;
                    cacheOverlay.setEnabled(true);
                    if (cacheOverlay.isSupportsChildren()) {
                        for (CacheOverlay childCache : cacheOverlay.getChildren()) {
                            childCache.setEnabled(true);
                            updatedEnabledOverlays.add(childCache.getCacheName());
                        }
                    } else {
                        updatedEnabledOverlays.add(cacheName);
                    }
                }

            }

            // Remove overlays in the preferences that no longer exist
            if (!enabledOverlays.isEmpty()) {
                updatedEnabledOverlays.removeAll(enabledOverlays);
                update = true;
            }

            // If new enabled cache overlays, update them in the preferences
            if (update) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putStringSet(context.getString(R.string.tileOverlaysKey), updatedEnabledOverlays);
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
            if (cacheOverlay != null) {
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
        GeoPackage geoPackage = null;

        // Add the GeoPackage overlay
        try {
            geoPackage = geoPackageManager.open(database);

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
                    minZoom = featureDao.getZoomLevel() + context.getResources().getInteger(R.integer.geopackage_feature_tiles_min_zoom_offset);
                    minZoom = Math.max(minZoom, 0);
                    minZoom = Math.min(minZoom, GeoPackageFeatureTableCacheOverlay.MAX_ZOOM);
                }
                GeoPackageTableCacheOverlay tableCache = new GeoPackageFeatureTableCacheOverlay(featureTable, database, tableCacheName, count, minZoom, indexed, geometryType);
                tables.add(tableCache);
            }

            // Create the GeoPackage overlay with child tables
            cacheOverlay = new GeoPackageCacheOverlay(database, tables);
        } catch (Exception e) {
            Log.e(LOG_NAME, "Could not get geopackage cache", e);
        } finally {
            if (geoPackage != null) {
                geoPackage.close();
            }
        }

        return cacheOverlay;
    }

}
