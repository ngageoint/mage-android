package mil.nga.giat.mage.ui.map.geoPackage

import android.app.Application
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.TileProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mil.nga.geopackage.BoundingBox
import mil.nga.geopackage.GeoPackage
import mil.nga.geopackage.GeoPackageCache
import mil.nga.geopackage.GeoPackageFactory
import mil.nga.geopackage.extension.nga.link.FeatureTileTableLinker
import mil.nga.geopackage.extension.nga.scale.TileTableScaling
import mil.nga.geopackage.map.tiles.overlay.FeatureOverlay
import mil.nga.geopackage.map.tiles.overlay.FeatureOverlayQuery
import mil.nga.geopackage.map.tiles.overlay.GeoPackageOverlayFactory
import mil.nga.geopackage.tiles.TileBoundingBoxUtils
import mil.nga.geopackage.tiles.features.DefaultFeatureTiles
import mil.nga.geopackage.tiles.features.FeatureTiles
import mil.nga.geopackage.tiles.features.custom.NumberFeaturesTile
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.datasource.layer.LayerLocalDataSource
import mil.nga.giat.mage.data.repository.cache.CacheOverlayRepository
import mil.nga.giat.mage.data.repository.event.EventRepository
import mil.nga.giat.mage.map.FileSystemTileProvider
import mil.nga.giat.mage.map.TMSTileProvider
import mil.nga.giat.mage.map.WMSTileProvider
import mil.nga.giat.mage.map.XYZTileProvider
import mil.nga.giat.mage.map.cache.CacheOverlay
import mil.nga.giat.mage.map.cache.CacheOverlayFilter
import mil.nga.giat.mage.map.cache.CacheOverlayType
import mil.nga.giat.mage.map.cache.CacheProvider
import mil.nga.giat.mage.map.cache.GeoPackageCacheOverlay
import mil.nga.giat.mage.map.cache.GeoPackageFeatureTableCacheOverlay
import mil.nga.giat.mage.map.cache.GeoPackageTileTableCacheOverlay
import mil.nga.giat.mage.map.cache.URLCacheOverlay
import mil.nga.giat.mage.map.cache.WMSCacheOverlay
import mil.nga.giat.mage.map.cache.XYZDirectoryCacheOverlay
import mil.nga.proj.ProjectionConstants
import mil.nga.proj.ProjectionFactory
import mil.nga.sf.GeometryType
import javax.inject.Inject

// The entirety of this class should be updated to use some kind of GeoPackage repository
// which presents the layers out as live data or flows or something similar, not callbacks
// from the Cache Provider
@HiltViewModel
class GeoPackagesMapViewModel @Inject constructor(
    private val application: Application,
    val cacheProvider: CacheProvider,
    cacheOverlayRepository: CacheOverlayRepository
): ViewModel() {
    val tileProviders: LiveData<Map<String, TileProvider>> = cacheOverlayRepository
        .cacheOverlays
        .map {

            val tileOverlays = HashMap<String, TileProvider>()
            for (cacheOverlay in it.values.filterNotNull()) {
                // If this cache overlay potentially replaced by a new version
                if (cacheOverlay.isAdded && cacheOverlay.type == CacheOverlayType.GEOPACKAGE) {
                    geoPackageCache.close(cacheOverlay.name)
                }

                // The user has asked for this overlay
                if (cacheOverlay.isEnabled) {
                    val tileProviders = when (cacheOverlay) {
                        is URLCacheOverlay -> addURLCacheOverlay(cacheOverlay)
                        is GeoPackageCacheOverlay -> addGeoPackageCacheOverlay(cacheOverlay)
                        is XYZDirectoryCacheOverlay -> addXYZDirectoryCacheOverlay(cacheOverlay)
                        else -> {
                            HashMap()
                        }
                    }
                    tileOverlays.putAll(tileProviders)
                }
                cacheOverlay.isAdded = false
            }
            tileOverlays
        }
        .asLiveData()

    val cacheBoundingBox = cacheOverlayRepository.cacheBoundingBox
    private var geoPackageCache: GeoPackageCache = GeoPackageCache(GeoPackageFactory.getManager(application))
    private var cacheOverlays = HashMap<String, CacheOverlay?>()

        // If a new cache was added, zoom to the bounding box area  This needs to go somewhere else
//        cacheBoundingBox.value?.let {
//            val boundsBuilder = LatLngBounds.Builder()
//            boundsBuilder.include(LatLng(it.minLatitude, it.minLongitude))
//            boundsBuilder.include(LatLng(it.minLatitude, it.maxLongitude))
//            boundsBuilder.include(LatLng(it.maxLatitude, it.minLongitude))
//            boundsBuilder.include(LatLng(it.maxLatitude, it.maxLongitude))

//            try {
//                map?.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 0))
//            } catch (e: Exception) {
//                Log.e(MapFragment.LOG_NAME, "Unable to move camera to newly added cache location", e)
//            }
//        }
//    }

    /**
     * Add a GeoPackage cache overlay, which contains tile and feature tables
     * @param enabledCacheOverlays
     * @param enabledGeoPackages
     * @param geoPackageCacheOverlay
     */
    private fun addGeoPackageCacheOverlay(
        geoPackageCacheOverlay: GeoPackageCacheOverlay
    ): Map<String, TileProvider> {
        val tileProviders = HashMap<String, TileProvider>()
        // Check each GeoPackage table
        for (tableCacheOverlay in geoPackageCacheOverlay.children) {
            // Check if the table is enabled
            if (tableCacheOverlay.isEnabled) {

                // Get and open if needed the GeoPackage
                val geoPackage = geoPackageCache.getOrOpen(geoPackageCacheOverlay.name)
                // Handle tile and feature tables
                try {
                    val tileProvider = when (tableCacheOverlay.type) {
                        CacheOverlayType.GEOPACKAGE_TILE_TABLE -> addGeoPackageTileCacheOverlay(
                            tableCacheOverlay as GeoPackageTileTableCacheOverlay,
                            geoPackage
                        )
                        CacheOverlayType.GEOPACKAGE_FEATURE_TABLE -> addGeoPackageFeatureCacheOverlay(
                            tableCacheOverlay as GeoPackageFeatureTableCacheOverlay,
                            geoPackage
                        )
                        else -> throw UnsupportedOperationException("Unsupported GeoPackage type: " + tableCacheOverlay.type)
                    }
                    tileProviders.putAll(tileProvider)
                } catch (e: Exception) {
                    Log.e(
                        LOG_NAME,
                        "Failed to add GeoPackage overlay. GeoPackage: " + geoPackage.name + ", Name: " + tableCacheOverlay.name,
                        e
                    )
                }
            }
        }
        return tileProviders
    }

    /**
     * Add the GeoPackage Tile Table Cache Overlay
     * @param enabledCacheOverlays
     * @param tileTableCacheOverlay
     * @param geoPackage
     */
    private fun addGeoPackageTileCacheOverlay(
        tileTableCacheOverlay: GeoPackageTileTableCacheOverlay,
        geoPackage: GeoPackage
    ): Map<String,TileProvider> {
        val tileProviders = HashMap<String, TileProvider>()
        // Retrieve the cache overlay if it already exists (and remove from cache overlays)
        var cacheOverlay = cacheOverlays.remove(tileTableCacheOverlay.cacheName)
        if (cacheOverlay != null) {
            // If the existing cache overlay is being replaced, create a new cache overlay
            if (tileTableCacheOverlay.parent.isAdded) {
                cacheOverlay = null
            }
        }
        if (cacheOverlay == null) {
            // Create a new GeoPackage tile provider and add to the map
            val tileDao = geoPackage.getTileDao(tileTableCacheOverlay.name)
            val tileTableScaling = TileTableScaling(geoPackage, tileDao)
            val tileScaling = tileTableScaling.get()
            val overlay = GeoPackageOverlayFactory.getBoundedOverlay(tileDao, application.resources.displayMetrics.density, tileScaling)



            val overlayOptions = createTileOverlayOptions(overlay)
            // Check for linked feature tables
            tileTableCacheOverlay.clearFeatureOverlayQueries()
            val linker = FeatureTileTableLinker(geoPackage)
            val featureDaos = linker.getFeatureDaosForTileTable(tileDao.tableName)
            for (featureDao in featureDaos) {
                val featureTiles: FeatureTiles = DefaultFeatureTiles(application, geoPackage, featureDao, application.resources.displayMetrics.density)

                // Add the feature overlay query
                val featureOverlayQuery = FeatureOverlayQuery(application, overlay, featureTiles)
                tileTableCacheOverlay.addFeatureOverlayQuery(featureOverlayQuery)
            }
            cacheOverlay = tileTableCacheOverlay
            cacheOverlays[tileTableCacheOverlay.cacheName] = cacheOverlay
            tileProviders[cacheOverlay.cacheName] = overlay
        }
        return tileProviders
    }

    /**
     * Add the GeoPackage Feature Table Cache Overlay
     * @param enabledCacheOverlays
     * @param featureTableCacheOverlay
     * @param geoPackage
     */
    private fun addGeoPackageFeatureCacheOverlay(
        featureTableCacheOverlay: GeoPackageFeatureTableCacheOverlay,
        geoPackage: GeoPackage
    ): Map<String,TileProvider> {
        val tileProviders = HashMap<String, TileProvider>()
        // Retrieve the cache overlay if it already exists (and remove from cache overlays)
        var cacheOverlay = cacheOverlays.remove(featureTableCacheOverlay.cacheName)
        if (cacheOverlay != null) {
            // If the existing cache overlay is being replaced, create a new cache overlay
            if (featureTableCacheOverlay.parent.isAdded) {
                cacheOverlay = null
            }
            for (linkedTileTable in featureTableCacheOverlay.linkedTileTables) {
                cacheOverlays.remove(linkedTileTable.cacheName)
            }
        }

        if (cacheOverlay == null) {
            // Add the features to the map
            val featureDao = geoPackage.getFeatureDao(featureTableCacheOverlay.name)

            // If indexed, add as a tile overlay
            if (featureTableCacheOverlay.isIndexed) {
                val featureTiles: FeatureTiles = DefaultFeatureTiles(
                    application, geoPackage, featureDao,
                    application.resources.displayMetrics.density
                )

                featureTiles.maxFeaturesPerTile = if (featureDao.geometryType == GeometryType.POINT) {
                    application.resources.getInteger(R.integer.geopackage_feature_tiles_max_points_per_tile)
                } else {
                    application.resources.getInteger(R.integer.geopackage_feature_tiles_max_features_per_tile)
                }

                // Adjust the max features number tile draw paint attributes here as needed to
                // change how tiles are drawn when more than the max features exist in a tile
                featureTiles.maxFeaturesTileDraw = NumberFeaturesTile(application)

                // Adjust the feature tiles draw paint attributes here as needed to change how
                // features are drawn on tiles
                val featureOverlay = FeatureOverlay(featureTiles)
                featureOverlay.minZoom = featureTableCacheOverlay.minZoom

                // Get the tile linked overlay
                val overlay = GeoPackageOverlayFactory.getLinkedFeatureOverlay(featureOverlay, geoPackage)
                val featureOverlayQuery = FeatureOverlayQuery(application, overlay, featureTiles)
                featureTableCacheOverlay.featureOverlayQuery = featureOverlayQuery
                val overlayOptions = createFeatureTileOverlayOptions(overlay)

                cacheOverlays[featureTableCacheOverlay.cacheName] = featureTableCacheOverlay
                tileProviders[featureTableCacheOverlay.cacheName] = overlay
            } else {
//                val maxFeaturesPerTable = if (featureDao.geometryType == GeometryType.POINT) {
//                    resources.getInteger(R.integer.geopackage_features_max_points_per_table)
//                } else {
//                    resources.getInteger(R.integer.geopackage_features_max_features_per_table)
//                }
//
//                val projection = featureDao.projection
//                val shapeConverter = GoogleMapShapeConverter(projection)
//                featureDao.queryForAll().use { cursor ->
//                    val totalCount = cursor.count
//                    var count = 0
//                    while (cursor.moveToNext()) {
//                        try {
//                            val featureRow = cursor.row
//                            val geometryData = featureRow.geometry
//                            if (geometryData != null && !geometryData.isEmpty) {
//                                val geometry = geometryData.geometry
//                                if (geometry != null) {
//                                    val shape = shapeConverter.toShape(geometry)
//                                    // Set the Shape Marker, PolylineOptions, and PolygonOptions here if needed to change color and style
//                                    featureTableCacheOverlay.addShapeToMap(featureRow.id, shape, map)
//                                    if (++count >= maxFeaturesPerTable) {
//                                        if (count < totalCount) {
//                                            Toast.makeText(application, featureTableCacheOverlay.cacheName + "- added " + count + " of " + totalCount, Toast.LENGTH_LONG).show()
//                                        }
//                                        break
//                                    }
//                                }
//                            }
//                        } catch (e: Exception) {
//                            Log.e(MapFragment.LOG_NAME, "Failed to display feature. GeoPackage: " + geoPackage.name + ", Table: " + featureDao.tableName + ", Row: " + cursor.position, e)
//                        }
//                    }
//                }
            }
        } else {
            this.tileProviders.value?.get(featureTableCacheOverlay.cacheName)?.let {
                tileProviders[featureTableCacheOverlay.cacheName] = it
            }
        }

        return tileProviders
    }

    private fun addURLCacheOverlay(
        urlCacheOverlay: URLCacheOverlay
    ): Map<String, TileProvider> {
        val tileProviders = HashMap<String, TileProvider>()
        // Retrieve the cache overlay if it already exists (and remove from cache overlays)
        if (!cacheOverlays.containsKey(urlCacheOverlay.cacheName)) {
            // Create a new tile provider and add to the map
            var isTransparent = false
            val tileProvider = when {
                urlCacheOverlay.format.equals("xyz", ignoreCase = true) -> {
                    XYZTileProvider(256, 256, urlCacheOverlay)
                }
                urlCacheOverlay.format.equals("tms", ignoreCase = true) -> {
                    TMSTileProvider(256, 256, urlCacheOverlay)
                }
                urlCacheOverlay is WMSCacheOverlay -> {
                    isTransparent = urlCacheOverlay.wmsTransparent.toBoolean()
                    WMSTileProvider(256, 256, urlCacheOverlay)
                }
                else -> null
            }

            if (tileProvider != null) {
                val overlayOptions = createTileOverlayOptions(tileProvider)
                if (urlCacheOverlay.isBase) {
                    overlayOptions.zIndex(-4f)
                } else if (!isTransparent) {
                    overlayOptions.zIndex(-3f)
                } else {
                    overlayOptions.zIndex(-2f)
                }

                tileProviders[urlCacheOverlay.cacheName] = tileProvider

                cacheOverlays[urlCacheOverlay.cacheName] = urlCacheOverlay
            }
        } else {
            this.tileProviders.value?.get(urlCacheOverlay.cacheName)?.let {
                tileProviders[urlCacheOverlay.cacheName] = it
            }
        }
        return tileProviders
    }

    /**
     * Add XYZ Directory tile cache overlay
     * @param enabledCacheOverlays
     * @param xyzDirectoryCacheOverlay
     */
    private fun addXYZDirectoryCacheOverlay(
        xyzDirectoryCacheOverlay: XYZDirectoryCacheOverlay
    ): Map<String, TileProvider> {
        val tileProviders = HashMap<String, TileProvider>()
        // Retrieve the cache overlay if it already exists (and remove from cache overlays)
        var cacheOverlay = cacheOverlays.remove(xyzDirectoryCacheOverlay.cacheName)
        if (cacheOverlay == null) {
            // Create a new tile provider and add to the map
            val tileProvider: TileProvider = FileSystemTileProvider(256, 256, xyzDirectoryCacheOverlay.directory.absolutePath)
            val overlayOptions = createTileOverlayOptions(tileProvider)
            cacheOverlay = xyzDirectoryCacheOverlay
        }

        cacheOverlays[cacheOverlay.cacheName] = cacheOverlay
        return tileProviders
    }

    /**
     * Create Feature Tile Overlay Options with the default z index for tile layers drawn from features
     * @param tileProvider
     * @return
     */
    private fun createFeatureTileOverlayOptions(tileProvider: TileProvider): TileOverlayOptions {
        return createTileOverlayOptions(tileProvider, -1)
    }

    /**
     * Create Tile Overlay Options with the default z index for tile layers
     * @param tileProvider
     * @param zIndex
     * @return
     */
    private fun createTileOverlayOptions(
        tileProvider: TileProvider,
        zIndex: Int = -2
    ): TileOverlayOptions {
        val overlayOptions = TileOverlayOptions()
        overlayOptions.tileProvider(tileProvider)
        overlayOptions.zIndex(zIndex.toFloat())
        return overlayOptions
    }

    companion object {
        private val LOG_NAME = GeoPackagesMapViewModel::class.java.name
    }
}