package mil.nga.giat.mage.map.cache;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.extension.link.FeatureTileTableLinker;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.geopackage.features.index.FeatureIndexManager;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.tiles.user.TileDao;
import mil.nga.geopackage.validate.GeoPackageValidate;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.cache.CacheUtils;
import mil.nga.giat.mage.cache.GeoPackageCacheUtils;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.exceptions.LayerException;
import mil.nga.giat.mage.sdk.utils.StorageUtility;
import mil.nga.sf.GeometryType;

/**
 * Created by wnewman on 2/11/16.
 */
public class CacheProvider {

    private static final String LOG_NAME = CacheProvider.class.getName();

    private final Context context;

    private final Map<String, CacheOverlay> cacheOverlays = Collections.synchronizedMap(new HashMap<String, CacheOverlay>());
    private final List<OnCacheOverlayListener> cacheOverlayListeners = Collections.synchronizedList(new ArrayList<OnCacheOverlayListener>());

    private static CacheProvider instance = null;

    private CacheProvider(Context context) {
        this.context = context;
    }

    public static synchronized CacheProvider getInstance(Context context) {
        if (instance == null) {
            instance = new CacheProvider(context);
        }

        return instance;
    }

    public interface OnCacheOverlayListener {
        void onCacheOverlay(List<CacheOverlay> cacheOverlays);
    }

    public List<CacheOverlay> getCacheOverlays() {
        List<CacheOverlay> copy = null;
        synchronized(cacheOverlays) {
            copy = Collections.unmodifiableList(new ArrayList<>(cacheOverlays.values()));
        }

        return copy;
    }

    public CacheOverlay getOverlay(String name){
        return cacheOverlays.get(name);
    }

    public void registerCacheOverlayListener(OnCacheOverlayListener listener) {
       registerCacheOverlayListener(listener, true);
    }

    public void registerCacheOverlayListener(OnCacheOverlayListener listener, boolean fire) {
        cacheOverlayListeners.add(listener);
        if(fire) {
            synchronized (cacheOverlays) {
                listener.onCacheOverlay(getCacheOverlays());
            }
        }
    }

    public void addCacheOverlay(CacheOverlay cacheOverlay) {
        cacheOverlays.put(cacheOverlay.getCacheName(), cacheOverlay);
    }

    public boolean removeCacheOverlay(String name) {
        return cacheOverlays.remove(name) != null;
    }

    public void unregisterCacheOverlayListener(OnCacheOverlayListener listener) {
        cacheOverlayListeners.remove(listener);
    }

    public void refreshTileOverlays() {
       enableAndRefreshTileOverlays(null);
    }

    public void enableAndRefreshTileOverlays(String enableOverlayName) {
        List<String> overlayNames = null;
        if(enableOverlayName != null) {
            overlayNames = new ArrayList<>(1);
            overlayNames.add(enableOverlayName);
        }
        TileOverlaysTask task = new TileOverlaysTask(overlayNames);
        task.execute();
    }

    private void setCacheOverlays(List<CacheOverlay> cacheOverlays) {
        synchronized (this.cacheOverlays) {
            this.cacheOverlays.clear();
            for(CacheOverlay overlay : cacheOverlays) {
                addCacheOverlay(overlay);
            }
        }

        synchronized(cacheOverlayListeners) {
            for (OnCacheOverlayListener listener : cacheOverlayListeners) {
                listener.onCacheOverlay(cacheOverlays);
            }
        }
    }

    private class TileOverlaysTask extends AsyncTask<Void, Void, List<CacheOverlay>> {

        private final Set<String> enable = new HashSet<>();

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

            try {
                List<Layer> imageryLayers = LayerHelper.getInstance(context).readAll("Imagery");

                for(Layer imagery : imageryLayers){
                    if(imagery.getFormat() == null || !imagery.getFormat().equalsIgnoreCase("wms")) {
                        overlays.add(new URLCacheOverlay(imagery.getName(), new URL(imagery.getUrl()), imagery));
                    }else{
                        overlays.add(new WMSCacheOverlay(imagery.getName(), new URL(imagery.getUrl()), imagery));
                    }
                }
            }catch(Exception e){
                Log.w(LOG_NAME, "Failed to load imagery layers", e);
            }

            try {
                List<Layer> featureLayers = LayerHelper.getInstance(context).readAll("Feature");

                for(Layer feature : featureLayers){
                    if(feature.isLoaded()){
                        overlays.add(new StaticFeatureCacheOverlay(feature.getName(), feature.getId()));
                    }
                }
            }catch(Exception e){
                Log.w(LOG_NAME, "Failed to load imagery layers", e);
            }

            // Set what should be enabled based on preferences.
            boolean update = false;
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            Set<String> updatedEnabledOverlays = new HashSet<>();
            updatedEnabledOverlays.addAll(preferences.getStringSet(context.getString(R.string.tileOverlaysKey), Collections.<String>emptySet()));
            updatedEnabledOverlays.addAll(preferences.getStringSet(context.getString(R.string.onlineLayersKey), Collections.<String>emptySet()));
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
                if (enable.contains(cacheName)) {

                    update = true;
                    cacheOverlay.setEnabled(true);
                    cacheOverlay.setAdded(true);
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

            try {
                LayerHelper layerHelper = LayerHelper.getInstance(context);
                List<Layer> layers = layerHelper.readAll("GeoPackage");
                for (Layer layer : layers) {
                    if (!layer.isLoaded()) {
                        continue;
                    }

                    String relativePath = layer.getRelativePath();
                    if (relativePath != null) {
                        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), relativePath);
                        if (!file.exists()) {
                            layer.setLoaded(true);
                            layerHelper.update(layer);
                        }
                    }
                }
            } catch (LayerException e) {
                Log.i(LOG_NAME, "Error reconciling downloaded layers", e);
            }

            // Add each existing database as a cache
            List<String> externalDatabases = geoPackageManager.externalDatabases();
            for (String database : externalDatabases) {
                GeoPackageCacheOverlay cacheOverlay = getGeoPackageCacheOverlay(context, geoPackageManager, database);
                if (cacheOverlay != null) {
                    overlays.add(cacheOverlay);
                }
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
    public GeoPackageCacheOverlay getGeoPackageCacheOverlay(Context context, File cache, GeoPackageManager geoPackageManager) {

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

            List<GeoPackageTableCacheOverlay> tables = new ArrayList<>();

            // GeoPackage tile tables, build a mapping between table name and the created cache overlays
            Map<String, GeoPackageTileTableCacheOverlay> tileCacheOverlays = new HashMap<>();
            List<String> tileTables = geoPackage.getTileTables();
            for (String tileTable : tileTables) {
                String tableCacheName = CacheOverlay.buildChildCacheName(database, tileTable);
                TileDao tileDao = geoPackage.getTileDao(tileTable);
                int count = tileDao.count();
                int minZoom = (int) tileDao.getMinZoom();
                int maxZoom = (int) tileDao.getMaxZoom();
                GeoPackageTileTableCacheOverlay tableCache = new GeoPackageTileTableCacheOverlay(tileTable, database, tableCacheName, count, minZoom, maxZoom);
                tileCacheOverlays.put(tileTable, tableCache);
            }

            // Get a linker to find tile tables linked to features
            FeatureTileTableLinker linker = new FeatureTileTableLinker(geoPackage);
            Map<String, GeoPackageTileTableCacheOverlay> linkedTileCacheOverlays = new HashMap<>();

            // GeoPackage feature tables
            List<String> featureTables = geoPackage.getFeatureTables();
            for (String featureTable : featureTables) {
                String tableCacheName = CacheOverlay.buildChildCacheName(database, featureTable);
                FeatureDao featureDao = geoPackage.getFeatureDao(featureTable);
                int count = featureDao.count();
                GeometryType geometryType = featureDao.getGeometryType();
                FeatureIndexManager indexer = new FeatureIndexManager(context, geoPackage, featureDao);
                boolean indexed = indexer.isIndexed();
                indexer.close();
                int minZoom = 0;
                if (indexed) {
                    minZoom = featureDao.getZoomLevel() + context.getResources().getInteger(R.integer.geopackage_feature_tiles_min_zoom_offset);
                    minZoom = Math.max(minZoom, 0);
                    minZoom = Math.min(minZoom, GeoPackageFeatureTableCacheOverlay.MAX_ZOOM);
                }
                GeoPackageFeatureTableCacheOverlay tableCache = new GeoPackageFeatureTableCacheOverlay(featureTable, database, tableCacheName, count, minZoom, indexed, geometryType);

                // If indexed, check for linked tile tables
                if(indexed){
                    List<String> linkedTileTables = linker.getTileTablesForFeatureTable(featureTable);
                    for(String linkedTileTable: linkedTileTables){
                        // Get the tile table cache overlay
                        GeoPackageTileTableCacheOverlay tileCacheOverlay = tileCacheOverlays.get(linkedTileTable);
                        if(tileCacheOverlay != null){
                            // Remove from tile cache overlays so the tile table is not added as stand alone, and add to the linked overlays
                            tileCacheOverlays.remove(linkedTileTable);
                            linkedTileCacheOverlays.put(linkedTileTable, tileCacheOverlay);
                        }else{
                            // Another feature table may already be linked to this table, so check the linked overlays
                            tileCacheOverlay = linkedTileCacheOverlays.get(linkedTileTable);
                        }

                        // Add the linked tile table to the feature table
                        if(tileCacheOverlay != null){
                            tableCache.addLinkedTileTable(tileCacheOverlay);
                        }
                    }
                }

                tables.add(tableCache);
            }

            // Add stand alone tile tables that were not linked to feature tables
            for(GeoPackageTileTableCacheOverlay tileCacheOverlay: tileCacheOverlays.values()){
                tables.add(tileCacheOverlay);
            }

            // Create the GeoPackage overlay with child tables
            cacheOverlay = new GeoPackageCacheOverlay(database, geoPackage.getPath(), tables);
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
