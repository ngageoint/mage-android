package mil.nga.giat.mage.data.repository.cache

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mil.nga.geopackage.GeoPackageCache
import mil.nga.geopackage.GeoPackageFactory
import mil.nga.giat.mage.data.datasource.layer.LayerLocalDataSource
import mil.nga.giat.mage.data.repository.event.EventRepository
import mil.nga.giat.mage.map.cache.CacheOverlay
import mil.nga.giat.mage.map.cache.CacheOverlayFilter
import mil.nga.giat.mage.map.cache.CacheOverlayType
import mil.nga.giat.mage.map.cache.CacheProvider
import javax.inject.Inject

class CacheOverlayRepository @Inject constructor(
    private val application: Application,
    private val eventRepository: EventRepository,
    private val layerLocalDataSource: LayerLocalDataSource,
    val cacheProvider: CacheProvider
): CacheProvider.OnCacheOverlayListener {
    private var geoPackageCache: GeoPackageCache = GeoPackageCache(GeoPackageFactory.getManager(application))
    private var _cacheOverlays = MutableStateFlow<MutableMap<String, CacheOverlay?>>(HashMap())
    val cacheOverlays: StateFlow<Map<String, CacheOverlay?>> = _cacheOverlays.asStateFlow()

    init {
        cacheProvider.registerCacheOverlayListener(this)
        CoroutineScope(Dispatchers.IO).launch {
            cacheProvider.refreshTileOverlays()
        }
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

        val overlays = CacheOverlayFilter(application, layers).filter(cacheOverlays)

        // Track enabled cache overlays
        val enabledCacheOverlays: MutableMap<String, CacheOverlay?> = HashMap()

        // Track enabled GeoPackages
        val enabledGeoPackages: MutableSet<String> = HashSet()

        // Reset the bounding box for newly added caches
//        _cacheBoundingBox.value = null
        for (cacheOverlay in overlays) {
            // If this cache overlay potentially replaced by a new version
            if (cacheOverlay.isAdded && cacheOverlay.type == CacheOverlayType.GEOPACKAGE) {
                geoPackageCache.close(cacheOverlay.name)
            }

            // The user has asked for this overlay
//            if (cacheOverlay.isEnabled) {
//                when (cacheOverlay) {
//                    is URLCacheOverlay -> addURLCacheOverlay(enabledCacheOverlays, cacheOverlay)
//                    is GeoPackageCacheOverlay -> addGeoPackageCacheOverlay(enabledCacheOverlays, enabledGeoPackages, cacheOverlay)
//                    is XYZDirectoryCacheOverlay -> addXYZDirectoryCacheOverlay(enabledCacheOverlays, cacheOverlay)
//                }
//            }
            cacheOverlay.isAdded = false
        }

        // Remove any overlays that are on the map but no longer selected in
        // preferences, update the tile overlays to the enabled tile overlays
        _cacheOverlays.value.values.forEach {
            it?.removeFromMap()
        }
        _cacheOverlays.value = enabledCacheOverlays

        // Close GeoPackages no longer enabled
        geoPackageCache.closeRetain(enabledGeoPackages)

        // If a new cache was added, zoom to the bounding box area
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
    }

}