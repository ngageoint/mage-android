package mil.nga.giat.mage.data.repository.cache

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import mil.nga.giat.mage.data.repository.event.EventRepository
import mil.nga.giat.mage.map.cache.CacheOverlay
import mil.nga.giat.mage.map.cache.CacheOverlayFilter
import mil.nga.giat.mage.map.cache.CacheOverlayType
import mil.nga.giat.mage.map.cache.CacheProvider
import mil.nga.giat.mage.map.cache.GeoPackageCacheOverlay
import mil.nga.giat.mage.map.cache.GeoPackageFeatureTableCacheOverlay
import mil.nga.giat.mage.map.cache.GeoPackageTileTableCacheOverlay
import mil.nga.giat.mage.ui.map.geoPackage.GeoPackagesMapViewModel
import mil.nga.proj.ProjectionConstants
import mil.nga.proj.ProjectionFactory
import mil.nga.sf.GeometryType
import javax.inject.Inject

class CacheOverlayRepository @Inject constructor(
    private val application: Application,
    private val eventRepository: EventRepository,
    private val layerLocalDataSource: LayerLocalDataSource,
    val cacheProvider: CacheProvider
): CacheProvider.OnCacheOverlayListener {
    private var geoPackageCache: GeoPackageCache = GeoPackageCache(GeoPackageFactory.getManager(application))
    private var _cacheOverlays = MutableStateFlow<Map<String, CacheOverlay?>>(HashMap())
    val cacheOverlays: StateFlow<Map<String, CacheOverlay?>> = _cacheOverlays.asStateFlow()

    private var _cacheBoundingBox = MutableStateFlow<BoundingBox?>(null)
    val cacheBoundingBox: StateFlow<BoundingBox?> = _cacheBoundingBox

    init {
        cacheProvider.registerCacheOverlayListener(this)
    }

    override fun onCacheOverlay(cacheOverlays: List<CacheOverlay>) {
        CoroutineScope(Dispatchers.IO).launch {
            handleCacheOverlays(cacheOverlays)
        }
    }

    private suspend fun handleCacheOverlays(cacheOverlays: List<CacheOverlay>) {
        // Add all overlays that are in the preferences
        val currentEvent = eventRepository.getCurrentEvent()
        val layers = layerLocalDataSource.readByEvent(currentEvent, "GeoPackage");

        val currentOverlays = _cacheOverlays.value.toMutableMap()

        val overlays = CacheOverlayFilter(application, layers).filter(cacheOverlays)

        // Track enabled cache overlays
        val enabledCacheOverlays: MutableMap<String, CacheOverlay?> = HashMap()

        // Track enabled GeoPackages
        val enabledGeoPackages: MutableSet<String> = HashSet()

        var anyChanges = false
        var newCacheBoundingBox: BoundingBox? = null

        for (cacheOverlay in overlays) {
            val currentOverlay = currentOverlays.remove(cacheOverlay.cacheName)
            if (currentOverlay != null) {
                // check if anything changed
                anyChanges = anyChanges || cacheOverlay.isAdded || cacheOverlay.isEnabled != currentOverlay.isEnabled
            } else {
                anyChanges = true
            }

            // If this cache overlay potentially replaced by a new version
            if (cacheOverlay.isAdded && cacheOverlay.type == CacheOverlayType.GEOPACKAGE) {
                geoPackageCache.close(cacheOverlay.name)
            }

            // The user has asked for this overlay so open the file and set it up
            if (cacheOverlay.isEnabled && cacheOverlay is GeoPackageCacheOverlay) {

                for (tableCacheOverlay in cacheOverlay.children) {
                    // Check if the table is enabled
                    if (tableCacheOverlay.isEnabled) {

                        // Get and open if needed the GeoPackage
                        val geoPackage = geoPackageCache.getOrOpen(cacheOverlay.name)
                        enabledGeoPackages.add(geoPackage.name)

                        if (cacheOverlay.isAdded) {
                            try {
                                val boundingBox = geoPackage.getBoundingBox(
                                    ProjectionFactory.getProjection(ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM.toLong()),
                                    tableCacheOverlay.name
                                )
                                if (boundingBox != null) {
                                    newCacheBoundingBox = if (newCacheBoundingBox == null) {
                                        boundingBox
                                    } else {
                                        TileBoundingBoxUtils.union(
                                            newCacheBoundingBox,
                                            boundingBox
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    LOG_NAME,
                                    "Failed to retrieve GeoPackage Table bounding box. GeoPackage: "
                                            + geoPackage.name + ", Table: " + tableCacheOverlay.name,
                                    e
                                )
                            }
                        }
                    }
                }
            }
            cacheOverlay.isAdded = false
        }

        if (anyChanges || currentOverlays.isNotEmpty()) {
            _cacheOverlays.value = overlays.associateBy { it.cacheName }
            _cacheBoundingBox.value = newCacheBoundingBox
        }

        // Close GeoPackages no longer enabled.  The API takes the passed in GeoPackage names
        // and closes all of the non passed in GeoPackages.
        geoPackageCache.closeRetain(enabledGeoPackages)
    }

    companion object {
        private val LOG_NAME = CacheOverlayRepository::class.java.name
    }
}