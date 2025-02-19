package mil.nga.giat.mage.map.preference

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.ExpandableListView
import androidx.annotation.MainThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.ListFragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mil.nga.geopackage.GeoPackageFactory
import mil.nga.giat.mage.R
import mil.nga.giat.mage.cache.CacheUtils
import mil.nga.giat.mage.data.repository.layer.LayerRepository
import mil.nga.giat.mage.map.cache.CacheOverlay
import mil.nga.giat.mage.map.cache.CacheOverlayFilter
import mil.nga.giat.mage.map.cache.CacheOverlayType
import mil.nga.giat.mage.map.cache.CacheProvider
import mil.nga.giat.mage.map.cache.CacheProvider.OnCacheOverlayListener
import mil.nga.giat.mage.map.cache.GeoPackageCacheOverlay
import mil.nga.giat.mage.map.cache.StaticFeatureCacheOverlay
import mil.nga.giat.mage.map.cache.XYZDirectoryCacheOverlay
import mil.nga.giat.mage.map.download.GeoPackageDownloadManager
import mil.nga.giat.mage.database.model.layer.Layer
import mil.nga.giat.mage.data.datasource.layer.LayerLocalDataSource
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.sdk.exceptions.LayerException
import mil.nga.giat.mage.sdk.utils.StorageUtility
import java.io.File
import java.util.Collections
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

@AndroidEntryPoint
class TileOverlayPreferenceActivity : AppCompatActivity() {
   @Inject lateinit var preferences: SharedPreferences

   private lateinit var offlineLayersFragment: OverlayListFragment

   public override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContentView(R.layout.activity_offline_layers)
      offlineLayersFragment = supportFragmentManager.findFragmentById(R.id.offline_layers_fragment) as OverlayListFragment
   }

   @Deprecated("Deprecated in Java")
   override fun onBackPressed() {
       super.onBackPressed()
       val editor = preferences.edit()
      editor.putStringSet(resources.getString(R.string.tileOverlaysKey), HashSet(offlineLayersFragment.getSelectedOverlays()))
      editor.apply()
      synchronized(offlineLayersFragment.timerLock) {
         if (offlineLayersFragment.downloadRefreshTimer != null) {
            offlineLayersFragment.downloadRefreshTimer!!.cancel()
         }
      }
      finish()
   }

   override fun onOptionsItemSelected(item: MenuItem): Boolean {
      if (item.itemId == android.R.id.home) {
         onBackPressed()
         return true
      }
      return super.onOptionsItemSelected(item)
   }

   @AndroidEntryPoint
   class OverlayListFragment : ListFragment(), OnCacheOverlayListener {

      @Inject lateinit var cacheProvider: CacheProvider
      @Inject lateinit var layerRepository: LayerRepository
      @Inject lateinit var layerLocalDataSource: LayerLocalDataSource
      @Inject lateinit var eventLocalDataSource: EventLocalDataSource

      private lateinit var adapter: OfflineLayersAdapter
      private val adapterLock = Any()
      private lateinit var listView: ExpandableListView
      private lateinit var contentView: View
      private lateinit var noContentView: View
      private lateinit var refreshButton: MenuItem
      private lateinit var swipeContainer: SwipeRefreshLayout
      private lateinit var downloadManager: GeoPackageDownloadManager
      var downloadRefreshTimer: Timer? = null
      val timerLock = Any()

      override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)

         setHasOptionsMenu(true)
         downloadManager =
            GeoPackageDownloadManager(
               requireActivity().applicationContext,
               cacheProvider,
               layerLocalDataSource
            ) { layer: Layer, overlay: CacheOverlay ->
               activity?.runOnUiThread {
                  synchronized(adapterLock) {
                     adapter.addOverlay(overlay, layer)
                     adapter.notifyDataSetChanged()
                  }
               }
            }
         val event = eventLocalDataSource.currentEvent
         adapter = OfflineLayersAdapter(
            requireActivity(),
            cacheProvider,
            downloadManager,
            layerRepository,
            layerLocalDataSource,
            event
         )
      }

      override fun onCreateView(
         inflater: LayoutInflater,
         container: ViewGroup?,
         savedInstanceState: Bundle?
      ): View? {
         val view = inflater.inflate(R.layout.fragment_offline_layers, container, false)
         listView = view.findViewById(android.R.id.list)
         listView.isEnabled = true
         listView.setAdapter(adapter)
         listView.onItemLongClickListener = OnItemLongClickListener { _, _, _, id ->
            val itemType = ExpandableListView.getPackedPositionType(id)
            if (itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
               // TODO Handle child row long clicks here
               return@OnItemLongClickListener true
            } else if (itemType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
               val groupPosition = ExpandableListView.getPackedPositionGroup(id)
               if (groupPosition != -1) {
                  synchronized(adapterLock) {
                     val group = adapter.getGroup(groupPosition)
                     if (group is CacheOverlay) {
                        val cacheOverlay = adapter.getGroup(groupPosition) as CacheOverlay
                        deleteCacheOverlayConfirm(cacheOverlay)
                        return@OnItemLongClickListener true
                     }
                  }
               }
               Log.w(LOG_NAME, "Failed to locate group at index $id")
               return@OnItemLongClickListener false
            }
            false
         }
         swipeContainer = view.findViewById(R.id.offline_layers_swipeContainer)
         swipeContainer.setColorSchemeResources(R.color.md_blue_600, R.color.md_orange_A200)
         swipeContainer.setOnRefreshListener {
            softRefresh(refreshButton)
            hardRefresh()
         }
         contentView = view.findViewById(R.id.downloadable_layers_content)
         noContentView = view.findViewById(R.id.downloadable_layers_no_content)
         noContentView.visibility = View.GONE

         CoroutineScope(Dispatchers.IO).launch {
            cacheProvider.refreshTileOverlays()
         }

         return view
      }

      override fun onDestroy() {
         super.onDestroy()
         cacheProvider.unregisterCacheOverlayListener(this)
      }

      override fun onResume() {
         super.onResume()
         downloadManager.onResume()
         synchronized(timerLock) {
            downloadRefreshTimer = Timer()
            downloadRefreshTimer?.schedule(GeopackageDownloadProgressTimer(activity), 0, 2000)
         }
      }

      override fun onPause() {
         super.onPause()
         cacheProvider.unregisterCacheOverlayListener(this)
         downloadManager.onPause()
         synchronized(timerLock) {
            if (downloadRefreshTimer != null) {
               downloadRefreshTimer?.cancel()
            }
         }
      }

      override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
         inflater.inflate(R.menu.offline_layers_menu, menu)
         refreshButton = menu.findItem(R.id.tile_overlay_refresh)
         refreshButton.isEnabled = true
         cacheProvider.registerCacheOverlayListener(this, false)
         softRefresh(refreshButton)
         refreshLocalDownloadableLayers()
      }

      override fun onOptionsItemSelected(item: MenuItem): Boolean {
         if (item.itemId == R.id.tile_overlay_refresh) {
            softRefresh(item)
            hardRefresh()
            return true
         }
         return super.onOptionsItemSelected(item)
      }

      @MainThread
      private fun softRefresh(item: MenuItem?) {
         item!!.isEnabled = false
         synchronized(adapterLock) {
            adapter.downloadableLayers.clear()
            adapter.overlays.clear()
            adapter.sideloadedOverlays.clear()
            adapter.notifyDataSetChanged()
         }
         contentView.visibility = View.GONE
         noContentView.visibility = View.VISIBLE
         listView.isEnabled = false
         swipeContainer.isRefreshing = true
      }

      /**
       * Attempt to pull all the layers from the remote server as well as refreshing any local overlays
       *
       */
      private fun hardRefresh() {
         CoroutineScope(Dispatchers.IO).launch {
            fetchRemoteGeopackageLayers()
            fetchRemoteStaticLayers()
            refreshLocalDownloadableLayers()
         }
      }

      private fun refreshLocalDownloadableLayers() {
         CoroutineScope(Dispatchers.IO).launch {
            val event = eventLocalDataSource.currentEvent
            val layers: MutableList<Layer> = ArrayList()
            for (layer in layerLocalDataSource.readByEvent(event, null)) {
               if (layer.type.equals("GeoPackage", ignoreCase = true) ||
                  layer.type.equals("Feature", ignoreCase = true)
               ) {
                  if (!layer.isLoaded && layer.downloadId == null) {
                     layers.add(layer)
                  }
               }
            }

            synchronized(adapterLock) {
               adapter.downloadableLayers.addAll(layers)
               Collections.sort(adapter.downloadableLayers, LayerNameComparator())
               // The adapter will be notified of a data set change in onCacheOverlay

               CoroutineScope(Dispatchers.IO).launch {
                  cacheProvider.refreshTileOverlays()
               }
            }
         }
      }

      /**
       * This reads the remote layers from the server but does not download them
       *
       */
      private fun fetchRemoteStaticLayers() {
         CoroutineScope(Dispatchers.IO).launch {
            eventLocalDataSource.currentEvent?.let { event ->
               layerRepository.fetchFeatureLayers(event, false)
            }
         }
      }

      private fun fetchRemoteGeopackageLayers() {
         CoroutineScope(Dispatchers.IO).launch {
            eventLocalDataSource.currentEvent?.let { event ->
               val layers = layerRepository.fetchLayers(event, "GeoPackage")
               saveGeopackageLayers(layers, event)
            }
         }
      }

      private fun saveGeopackageLayers(remoteLayers: Collection<Layer>, event: Event) {
         val context = requireActivity().applicationContext
         try {
            // get local layers
            val localLayers: Collection<Layer> = layerLocalDataSource.readAll("GeoPackage")
            val remoteIdToLayer: MutableMap<String, Layer> = HashMap(localLayers.size)
            for (layer in localLayers) {
               remoteIdToLayer[layer.remoteId] = layer
            }
            val manager = GeoPackageFactory.getManager(context)
            for (remoteLayer in remoteLayers) {
               remoteLayer.event = event
               // Check if its loaded
               val file = File(
                  context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                  String.format(
                     "MAGE/geopackages/%s/%s",
                     remoteLayer.remoteId,
                     remoteLayer.fileName
                  )
               )
               if (file.exists() && manager.existsAtExternalFile(file)) {
                  remoteLayer.isLoaded = true
               }
               if (!localLayers.contains(remoteLayer)) {
                  layerLocalDataSource.create(remoteLayer)
               } else {
                  val localLayer = remoteIdToLayer[remoteLayer.remoteId]
                  //Only remove a local layer if the even has changed
                  if (remoteLayer.event != localLayer!!.event) {
                     layerLocalDataSource.delete(localLayer.id)
                     layerLocalDataSource.create(remoteLayer)
                  }
               }
            }
         } catch (e: LayerException) {
            Log.e(LOG_NAME, "Error saving geopackage layers", e)
         }
      }

      @MainThread
      override fun onCacheOverlay(cacheOverlays: List<CacheOverlay>) {
         val event = eventLocalDataSource.currentEvent
         val geopackages = layerLocalDataSource.readByEvent(event, "GeoPackage")

         downloadManager.reconcileDownloads(
            geopackages
         ) { layers ->
            var isEmpty = false
            synchronized(adapterLock) {
               adapter.downloadableLayers.removeAll(layers)
               adapter.downloadableLayers.addAll(layers)
               adapter.overlays.clear()
               adapter.sideloadedOverlays.clear()

               val filtered = CacheOverlayFilter(
                  context = requireContext().applicationContext,
                  layers = layerLocalDataSource.readByEvent(event, "GeoPackage")
               ).filter(cacheOverlays)

               filtered.forEach { overlay ->
                  if (overlay is GeoPackageCacheOverlay) {
                     if (overlay.isSideloaded()) {
                        adapter.sideloadedOverlays.add(overlay)
                     } else {
                        adapter.overlays.add(overlay)
                     }
                  } else if (overlay is StaticFeatureCacheOverlay) {
                     adapter.overlays.add(overlay)
                  }
               }

               Collections.sort(adapter.downloadableLayers, LayerNameComparator())
               adapter.sideloadedOverlays.sort()
               adapter.overlays.sort()
               if (adapter.downloadableLayers.isEmpty()
                  && adapter.overlays.isEmpty()
                  && adapter.sideloadedOverlays.isEmpty()
               ) {
                  isEmpty = true
               }
               refreshButton.isEnabled = true
               if (!isEmpty) {
                  noContentView.visibility = View.GONE
                  contentView.visibility = View.VISIBLE
               } else {
                  contentView.visibility = View.GONE
                  noContentView.visibility = View.VISIBLE
               }
               swipeContainer.isRefreshing = false
               listView.isEnabled = true
               adapter.notifyDataSetChanged()
            }
         }
      }

      /**
       * Get the selected cache overlays and child cache overlays
       *
       * @return added cache overlays
       */
      fun getSelectedOverlays(): ArrayList<String> {
         val overlays = ArrayList<String>()
         cacheProvider.cacheOverlays.forEach { (_, overlay) ->
            if (overlay is GeoPackageCacheOverlay) {
               var childAdded = false
               for (childCache in overlay.children) {
                  if (childCache.isEnabled) {
                     overlays.add(childCache.cacheName)
                     childAdded = true
                  }
               }
               if (!childAdded && overlay.isEnabled) {
                  overlays.add(overlay.cacheName)
               }
            } else if (overlay.isEnabled) {
               if (overlay is StaticFeatureCacheOverlay ||
                  overlay is XYZDirectoryCacheOverlay
               ) {
                  overlays.add(overlay.cacheName)
               }
            }
         }

         return overlays
      }

      /**
       * Delete the cache overlay
       * @param cacheOverlay
       */
      @MainThread
      private fun deleteCacheOverlayConfirm(cacheOverlay: CacheOverlay) {
         val deleteDialog = AlertDialog.Builder(requireActivity())
            .setTitle("Delete Layer")
            .setMessage("Delete " + cacheOverlay.name + " Layer?")
            .setPositiveButton(
               "Delete"
            ) { _, _ -> deleteCacheOverlay(cacheOverlay) }
            .setNegativeButton(
               getString(R.string.cancel)
            ) { dialog, _ -> dialog.dismiss() }.create()
         deleteDialog.show()
      }

      /**
       * Delete the XYZ cache overlay
       * @param xyzCacheOverlay
       */
      private fun deleteXYZCacheOverlay(xyzCacheOverlay: XYZDirectoryCacheOverlay) {
         val directory = xyzCacheOverlay.directory
         if (directory.canWrite()) {
            deleteFile(directory)
         }
      }

      /**
       * Delete the base directory file
       * @param base directory
       */
      private fun deleteFile(base: File) {
         if (base.isDirectory) {
            base.listFiles()?.forEach { file ->
               deleteFile(file)
            }
         }
         base.delete()
      }

      /**
       * Delete the cache overlay
       * @param cacheOverlay
       */
      private fun deleteCacheOverlay(cacheOverlay: CacheOverlay) {
         CoroutineScope(Dispatchers.IO).launch {
            when (cacheOverlay.type) {
               CacheOverlayType.XYZ_DIRECTORY -> deleteXYZCacheOverlay(cacheOverlay as XYZDirectoryCacheOverlay)
               CacheOverlayType.GEOPACKAGE -> deleteGeoPackageCacheOverlay(cacheOverlay as GeoPackageCacheOverlay)
               CacheOverlayType.STATIC_FEATURE -> deleteStaticFeatureCacheOverlay(cacheOverlay as StaticFeatureCacheOverlay)
               else -> {}
            }
            hardRefresh()
         }
      }

      /**
       * Delete the GeoPackage cache overlay
       * @param geoPackageCacheOverlay
       */
      private fun deleteGeoPackageCacheOverlay(geoPackageCacheOverlay: GeoPackageCacheOverlay) {
         val database = geoPackageCacheOverlay.name

         // Get the GeoPackage file
         val manager = GeoPackageFactory.getManager(activity)
         val path = manager.getFile(database)

         // Delete the cache from the GeoPackage manager
         manager.delete(database)

         // Attempt to delete the cache file if it is in the cache directory
         val pathDirectory = path.parentFile
         if (path.canWrite() && pathDirectory != null) {
            val storageLocations = StorageUtility.getWritableStorageLocations()
            for (storageLocation in storageLocations.values) {
               val root = File(storageLocation, getString(R.string.overlay_cache_directory))
               if (root == pathDirectory) {
                  path.delete()
                  break
               }
            }
         }

         // Check internal/external application storage
         val applicationCacheDirectory = CacheUtils.getApplicationCacheDirectory(requireContext().applicationContext)
         if (applicationCacheDirectory.exists()) {
            applicationCacheDirectory.listFiles()
               ?.filter { it == path }
               ?.forEach { it.delete() }
         }
         if (path.absolutePath.startsWith(
               String.format(
                  "%s/MAGE/geopackages",
                  requireActivity().applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
               )
            )
         ) {
            try {
               val relativePath = path.absolutePath.split(
                  (requireActivity().applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath + "/").toRegex()
               ).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
               val layer = layerLocalDataSource.getByRelativePath(relativePath)
               if (layer != null) {
                  layer.isLoaded = false
                  layer.downloadId = null
                  layerLocalDataSource.update(layer)
               }
            } catch (e: LayerException) {
               Log.e(LOG_NAME, "Error setting loaded to false for path $path", e)
            }
            if (!path.delete()) {
               Log.e(LOG_NAME, "Error deleting geopackage file from filesystem for path $path")
            }
         }
      }

      private fun deleteStaticFeatureCacheOverlay(cacheOverlay: StaticFeatureCacheOverlay) {
         try {
            layerLocalDataSource.delete(cacheOverlay.id)
         } catch (e: LayerException) {
            Log.w(LOG_NAME, "Failed to delete static feature " + cacheOverlay.cacheName, e)
         }
      }

      private inner class GeopackageDownloadProgressTimer(private val myActivity: Activity?) :
         TimerTask() {
         private var canceled = false
         override fun run() {
            synchronized(timerLock) {
               if (!canceled) {
                  try {
                     updateGeopackageDownloadProgress()
                  } catch (ignore: Exception) { }
               }
            }
         }

         override fun cancel(): Boolean {
            synchronized(timerLock) { canceled = true }
            return super.cancel()
         }

         private fun updateGeopackageDownloadProgress() {
            myActivity!!.runOnUiThread(Runnable {
               synchronized(adapterLock) {
                  try {
                     val layers = adapter.downloadableLayers
                     for (layer in layers) {
                        synchronized(timerLock) {
                           if (canceled) {
                              return@Runnable
                           }
                        }
                        if (layer.downloadId == null || layer.isLoaded) {
                           continue
                        }
                        for (i in layers.indices) {
                           val view = listView.getChildAt(i)
                           if (view == null || view.tag == null || view.tag != layer.name) {
                              continue
                           }
                           adapter.updateDownloadProgress(view, layer)
                        }
                     }
                  } catch (ignore: Exception) { }
               }
            })
         }
      }
   }

   companion object {
      private val LOG_NAME = TileOverlayPreferenceActivity::class.java.name
   }
}