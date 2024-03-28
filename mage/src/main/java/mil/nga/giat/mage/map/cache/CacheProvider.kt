package mil.nga.giat.mage.map.cache

import android.app.Application
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mil.nga.geopackage.GeoPackage
import mil.nga.geopackage.GeoPackageFactory
import mil.nga.geopackage.GeoPackageManager
import mil.nga.geopackage.extension.nga.link.FeatureTileTableLinker
import mil.nga.geopackage.features.index.FeatureIndexManager
import mil.nga.geopackage.validate.GeoPackageValidate
import mil.nga.giat.mage.R
import mil.nga.giat.mage.cache.CacheUtils
import mil.nga.giat.mage.cache.GeoPackageCacheUtils
import mil.nga.giat.mage.data.datasource.layer.LayerLocalDataSource
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.sdk.exceptions.LayerException
import mil.nga.giat.mage.sdk.utils.StorageUtility
import java.io.File
import java.net.URL
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheProvider @Inject constructor(
   private val application: Application,
   private val layerLocalDataSource: LayerLocalDataSource,
   private val eventLocalDataSource: EventLocalDataSource,
   private val preferences: SharedPreferences
) {
   val cacheOverlays = Collections.synchronizedMap(HashMap<String, CacheOverlay>())
   private val cacheOverlayListeners = Collections.synchronizedList(ArrayList<OnCacheOverlayListener>())

   interface OnCacheOverlayListener {
      fun onCacheOverlay(cacheOverlays: List<CacheOverlay>)
   }

   fun getCacheOverlays(): List<CacheOverlay> {
      var copy: List<CacheOverlay>
      synchronized(cacheOverlays) {
         copy = cacheOverlays.values.toList()
      }
      return copy
   }

   fun getOverlay(name: String): CacheOverlay? {
      return cacheOverlays[name]
   }

   fun registerCacheOverlayListener(listener: OnCacheOverlayListener, fire: Boolean = true) {
      cacheOverlayListeners.add(listener)
      if (fire) {
         synchronized(cacheOverlays) { listener.onCacheOverlay(getCacheOverlays()) }
      }
   }

   fun addCacheOverlay(cacheOverlay: CacheOverlay?) {
      cacheOverlays[cacheOverlay!!.cacheName] = cacheOverlay
   }

   fun removeCacheOverlay(name: String): Boolean {
      return cacheOverlays.remove(name) != null
   }

   fun unregisterCacheOverlayListener(listener: OnCacheOverlayListener) {
      cacheOverlayListeners.remove(listener)
   }

   suspend fun refreshTileOverlays() = withContext(Dispatchers.IO) {
      enableAndRefreshTileOverlays()
   }

   suspend fun enableAndRefreshTileOverlays(enableOverlayName: String? = null) {
      val overlayNames = mutableListOf<String>().apply {
         enableOverlayName?.let { add(it) }
      }

      enableAndRefreshTileOverlays(overlayNames)
   }

   private fun setCacheOverlays(cacheOverlays: List<CacheOverlay>) {
      synchronized(this.cacheOverlays) {
         this.cacheOverlays.clear()
         for (overlay in cacheOverlays) {
            addCacheOverlay(overlay)
         }
      }
      synchronized(cacheOverlayListeners) {
         for (listener in cacheOverlayListeners) {
            listener.onCacheOverlay(cacheOverlays)
         }
      }
   }

   suspend private fun enableAndRefreshTileOverlays(enable: List<String>): List<CacheOverlay> {
      val event = eventLocalDataSource.currentEvent ?: return emptyList()

      val overlays = mutableListOf<CacheOverlay>()

      // Add the existing external GeoPackage databases as cache overlays
      val geoPackageManager = GeoPackageFactory.getManager(application)
      overlays.addAll(getGeoPackageCacheOverlays(geoPackageManager))

      // Get public external caches stored in /MapCache folder
      val storageLocations = StorageUtility.getReadableStorageLocations()
      for (storageLocation in storageLocations.values) {
         val root = File(storageLocation, application.getString(R.string.overlay_cache_directory))
         if (root.exists() && root.isDirectory && root.canRead()) {
            root.listFiles()?.forEach { cache ->
               if (cache.canRead()) {
                  if (cache.isDirectory) {
                     overlays.add(XYZDirectoryCacheOverlay(cache.name, cache))
                  } else if (GeoPackageValidate.hasGeoPackageExtension(cache)) {
                     getGeoPackageCacheOverlay(cache, geoPackageManager)?.let {
                        overlays.add(it)
                     }
                  }
               }
            }
         }
      }

      // Check internal/external application storage
      val applicationCacheDirectory = CacheUtils.getApplicationCacheDirectory(application)
      if (applicationCacheDirectory != null && applicationCacheDirectory.exists()) {
         applicationCacheDirectory.listFiles()?.forEach { cache ->
            if (GeoPackageValidate.hasGeoPackageExtension(cache)) {
               getGeoPackageCacheOverlay(cache, geoPackageManager)?.let {
                  overlays.add(it)
               }
            }
         }
      }

      for (imagery in layerLocalDataSource.readByEvent(event, "Imagery")) {
         if (imagery.format == null || !imagery.format.equals("wms", ignoreCase = true)) {
            overlays.add(URLCacheOverlay(imagery.name, URL(imagery.url), imagery))
         } else {
            overlays.add(WMSCacheOverlay(imagery.name, URL(imagery.url), imagery))
         }
      }

      for (feature in layerLocalDataSource.readByEvent(event, "Feature")) {
         if (feature.isLoaded) {
            overlays.add(StaticFeatureCacheOverlay(feature.name, feature.id))
         }
      }

      // Set what should be enabled based on preferences.
      var update = false

      val updatedEnabledOverlays: MutableSet<String> = HashSet()
      updatedEnabledOverlays.addAll(
         preferences.getStringSet(
            application.getString(R.string.tileOverlaysKey),
            emptySet()
         )!!
      )
      updatedEnabledOverlays.addAll(
         preferences.getStringSet(
            application.getString(R.string.onlineLayersKey),
            emptySet()
         )!!
      )
      val enabledOverlays: MutableSet<String> = HashSet()
      enabledOverlays.addAll(updatedEnabledOverlays)

      // Determine which caches are enabled
      for (cacheOverlay in overlays) {
         // Check and enable the cache
         val cacheName = cacheOverlay.cacheName
         if (enabledOverlays.remove(cacheName)) {
            cacheOverlay.isEnabled = true
         }

         // Check the child caches
         for (childCache in cacheOverlay.children) {
            if (enabledOverlays.remove(childCache.cacheName)) {
               childCache.isEnabled = true
               cacheOverlay.isEnabled = true
            }
         }

         // Check for new caches to enable in the overlays and preferences
         if (enable.contains(cacheName)) {
            update = true
            cacheOverlay.isEnabled = true
            cacheOverlay.isAdded = true
            if (cacheOverlay.isSupportsChildren) {
               for (childCache in cacheOverlay.children) {
                  childCache.isEnabled = true
                  updatedEnabledOverlays.add(childCache.cacheName)
               }
            } else {
               updatedEnabledOverlays.add(cacheName)
            }
         }
      }

      // Remove overlays in the preferences that no longer exist
      if (enabledOverlays.isNotEmpty()) {
         updatedEnabledOverlays.removeAll(enabledOverlays)
         update = true
      }

      // If new enabled cache overlays, update them in the preferences
      if (update) {
         val editor = preferences.edit()
         editor.putStringSet(application.getString(R.string.tileOverlaysKey), updatedEnabledOverlays)
         editor.apply()
      }

      CoroutineScope(Dispatchers.Main).launch {
         setCacheOverlays(overlays)
      }

      return overlays
   }

   /**
    * Add GeoPackage Cache Overlay for the existing databases
    *
    * @param context
    * @param overlays
    * @param geoPackageManager
    */
   suspend private fun getGeoPackageCacheOverlays(
      geoPackageManager: GeoPackageManager
   ): List<CacheOverlay> {
      val overlays = mutableListOf<CacheOverlay>()

      // Delete any GeoPackages where the file is no longer accessible
      geoPackageManager.deleteAllMissingExternal()
      val remoteGeopackages = mutableMapOf<String, String>()
      try {
         val layers = layerLocalDataSource.readAll("GeoPackage")
         for (layer in layers) {
            if (!layer.isLoaded) {
               continue
            }
            val relativePath = layer.relativePath
            if (relativePath != null) {
               val file = File(
                  application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                  relativePath
               )
               remoteGeopackages[file.name] = file.name
               if (!file.exists()) {
                  layer.isLoaded = true
                  layerLocalDataSource.update(layer)
               }
            }
         }
      } catch (e: LayerException) {
         Log.i(LOG_NAME, "Error reconciling downloaded layers", e)
      }

      // Add each existing database as a cache
      val externalDatabases = geoPackageManager.externalDatabases()
      for (database in externalDatabases) {
         val cacheOverlay = getGeoPackageCacheOverlay(geoPackageManager, database)
         if (cacheOverlay != null) {
            val f = File(cacheOverlay.filePath)
            //TODO what happens if there are 2 geopackages with the same name
            if (!remoteGeopackages.containsKey(f.name)) {
               cacheOverlay.isSideloaded = true
            }
            overlays.add(cacheOverlay)
         }
      }

      return overlays
   }

   /**
    * Get GeoPackage Cache Overlay for the database file
    *
    * @param cache
    * @param geoPackageManager
    * @return cache overlay
    */
   fun getGeoPackageCacheOverlay(
      cache: File?,
      geoPackageManager: GeoPackageManager
   ): GeoPackageCacheOverlay? {
      var cacheOverlay: GeoPackageCacheOverlay? = null

      // Import the GeoPackage if needed
      val cacheName = GeoPackageCacheUtils.importGeoPackage(geoPackageManager, cache)
      if (cacheName != null) {
         // Get the GeoPackage overlay
         cacheOverlay = getGeoPackageCacheOverlay(geoPackageManager, cacheName)
      }
      return cacheOverlay
   }

   /**
    * Get the GeoPackage database as a cache overlay
    *
    * @param geoPackageManager
    * @param database
    * @return cache overlay
    */
   private fun getGeoPackageCacheOverlay(
      geoPackageManager: GeoPackageManager,
      database: String
   ): GeoPackageCacheOverlay? {
      var cacheOverlay: GeoPackageCacheOverlay? = null
      var geoPackage: GeoPackage? = null

      // Add the GeoPackage overlay
      try {
         geoPackage = geoPackageManager.open(database)
         val tables: MutableList<GeoPackageTableCacheOverlay> = ArrayList()

         // GeoPackage tile tables, build a mapping between table name and the created cache overlays
         val tileCacheOverlays: MutableMap<String, GeoPackageTileTableCacheOverlay> = HashMap()
         val tileTables = geoPackage.tileTables
         for (tileTable in tileTables) {
            val tableCacheName = CacheOverlay.buildChildCacheName(database, tileTable)
            val tileDao = geoPackage.getTileDao(tileTable)
            val count = tileDao.count()
            val minZoom = tileDao.minZoom.toInt()
            val maxZoom = tileDao.maxZoom.toInt()
            val tableCache = GeoPackageTileTableCacheOverlay(
               tileTable,
               database,
               tableCacheName,
               count,
               minZoom,
               maxZoom
            )
            tileCacheOverlays[tileTable] = tableCache
         }

         // Get a linker to find tile tables linked to features
         val linker = FeatureTileTableLinker(geoPackage)
         val linkedTileCacheOverlays: MutableMap<String, GeoPackageTileTableCacheOverlay> =
            HashMap()

         // GeoPackage feature tables
         val featureTables = geoPackage.featureTables
         for (featureTable in featureTables) {
            val tableCacheName = CacheOverlay.buildChildCacheName(database, featureTable)
            val featureDao = geoPackage.getFeatureDao(featureTable)
            val count = featureDao.count()
            val geometryType = featureDao.geometryType
            val indexer = FeatureIndexManager(application, geoPackage, featureDao)
            val indexed = indexer.isIndexed
            indexer.close()
            var minZoom = 0
            if (indexed) {
               minZoom =
                  featureDao.zoomLevel + application.resources.getInteger(R.integer.geopackage_feature_tiles_min_zoom_offset)
               minZoom = minZoom.coerceAtLeast(0)
               minZoom = minZoom.coerceAtMost(GeoPackageFeatureTableCacheOverlay.MAX_ZOOM)
            }
            val tableCache = GeoPackageFeatureTableCacheOverlay(
               featureTable,
               database,
               tableCacheName,
               count,
               minZoom,
               indexed,
               geometryType
            )

            // If indexed, check for linked tile tables
            if (indexed) {
               val linkedTileTables = linker.getTileTablesForFeatureTable(featureTable)
               for (linkedTileTable in linkedTileTables) {
                  // Get the tile table cache overlay
                  var tileCacheOverlay = tileCacheOverlays[linkedTileTable]
                  if (tileCacheOverlay != null) {
                     // Remove from tile cache overlays so the tile table is not added as stand alone, and add to the linked overlays
                     tileCacheOverlays.remove(linkedTileTable)
                     linkedTileCacheOverlays[linkedTileTable] = tileCacheOverlay
                  } else {
                     // Another feature table may already be linked to this table, so check the linked overlays
                     tileCacheOverlay = linkedTileCacheOverlays[linkedTileTable]
                  }

                  // Add the linked tile table to the feature table
                  if (tileCacheOverlay != null) {
                     tableCache.addLinkedTileTable(tileCacheOverlay)
                  }
               }
            }
            tables.add(tableCache)
         }

         // Add stand alone tile tables that were not linked to feature tables
         for (tileCacheOverlay in tileCacheOverlays.values) {
            tables.add(tileCacheOverlay)
         }

         // Create the GeoPackage overlay with child tables
         cacheOverlay = GeoPackageCacheOverlay(database, geoPackage.path, tables)
      } catch (e: Exception) {
         Log.e(LOG_NAME, "Could not get geopackage cache", e)
      } finally {
         geoPackage?.close()
      }
      return cacheOverlay
   }

   companion object {
      private val LOG_NAME = CacheProvider::class.java.name
   }
}