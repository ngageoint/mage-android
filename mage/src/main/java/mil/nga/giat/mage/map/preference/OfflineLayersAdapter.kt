package mil.nga.giat.mage.map.preference

import android.content.Context
import android.os.Environment
import android.text.format.Formatter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.Checkable
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.datasource.layer.LayerLocalDataSource
import mil.nga.giat.mage.data.repository.layer.LayerRepository
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.database.model.layer.Layer
import mil.nga.giat.mage.map.cache.CacheOverlay
import mil.nga.giat.mage.map.cache.CacheProvider
import mil.nga.giat.mage.map.cache.GeoPackageCacheOverlay
import mil.nga.giat.mage.map.cache.StaticFeatureCacheOverlay
import mil.nga.giat.mage.map.download.GeoPackageDownloadManager
import mil.nga.giat.mage.utils.ByteUtils
import org.apache.commons.lang3.StringUtils


class OfflineLayersAdapter(
   private val context: Context,
   private val cacheProvider: CacheProvider,
   private val downloadManager: GeoPackageDownloadManager,
   private val layerRepository: LayerRepository,
   private val layerLocalDataSource: LayerLocalDataSource,
   private val event: Event?
) : BaseExpandableListAdapter() {

   val overlays: MutableList<CacheOverlay> = ArrayList()
   val sideloadedOverlays: MutableList<CacheOverlay> = ArrayList()
   val downloadableLayers: MutableList<Layer> = ArrayList()

   fun addOverlay(overlay: CacheOverlay?, layer: Layer) {
      if (overlay is GeoPackageCacheOverlay || overlay is StaticFeatureCacheOverlay) {
         if (layer.isLoaded) {
            downloadableLayers.remove(layer)
            overlays.add(overlay)
         }
      }
   }

   fun updateDownloadProgress(view: View, layer: Layer) {
      val progress = downloadManager.getProgress(layer)
      val size = layer.fileSize
      val progressBar = view.findViewById<LinearProgressIndicator>(R.id.layer_progress)
      val download = view.findViewById<View>(R.id.layer_download)
      if (progress <= 0) {
         val reason = downloadManager.isFailed(layer)
         if (!StringUtils.isEmpty(reason)) {
            Toast.makeText(context, reason, Toast.LENGTH_LONG).show()
            progressBar.visibility = View.GONE
            download.visibility = View.VISIBLE
         }
         return
      }
      val currentProgress = (progress / size.toFloat() * 100).toInt()
      progressBar.isIndeterminate = false
      progressBar.progress = currentProgress
      val layerSize = view.findViewById<TextView>(R.id.layer_size)
      layerSize.text = String.format(
         "Downloading: %s of %s",
         Formatter.formatFileSize(context, progress.toLong()),
         Formatter.formatFileSize(context, size)
      )
   }

   override fun getGroupCount(): Int {
      return overlays.size + sideloadedOverlays.size + downloadableLayers.size
   }

   override fun getChildrenCount(i: Int): Int {
      var children = 0
      if (overlays.isNotEmpty() && i < overlays.size) {
         children = overlays[i].children.size
      } else if (sideloadedOverlays.isNotEmpty() && i - overlays.size < sideloadedOverlays.size) {
         children = sideloadedOverlays[i - overlays.size].children.size
      }
      for (layer: Layer in downloadableLayers) {
         if (layer.type.equals("geopackage", ignoreCase = true)) {
            if (layer.isLoaded) {
               children++
            }
         }
      }
      return children
   }

   override fun getGroup(i: Int): Any {
      val group = if (overlays.isNotEmpty() && i < overlays.size) {
         overlays[i]
      } else if (sideloadedOverlays.isNotEmpty() && i - overlays.size < sideloadedOverlays.size) {
         sideloadedOverlays[i - overlays.size]
      } else {
         downloadableLayers[i - overlays.size - sideloadedOverlays.size]
      }
      return group
   }

   override fun getChild(i: Int, j: Int): Any {
      var child: Any? = null
      if (overlays.isNotEmpty() && i < overlays.size) {
         child = overlays[i].children[j]
      } else if (sideloadedOverlays.isNotEmpty() && i - overlays.size < sideloadedOverlays.size) {
         child = sideloadedOverlays[i - overlays.size].children[j]
      }
      return (child)!!
   }

   override fun getGroupId(i: Int): Long {
      return i.toLong()
   }

   override fun getChildId(i: Int, j: Int): Long {
      return j.toLong()
   }

   override fun hasStableIds(): Boolean {
      return false
   }

   override fun getGroupView(
      groupPosition: Int,
      isExpanded: Boolean,
      convertView: View?,
      parent: ViewGroup?
   ): View {
      if (overlays.isNotEmpty() && groupPosition < overlays.size) {
         return getOverlayView(groupPosition, parent)
      } else return if (sideloadedOverlays.isNotEmpty() && groupPosition - overlays.size < sideloadedOverlays.size) {
         getOverlaySideloadedView(groupPosition, parent)
      } else {
         getDownloadableLayerView(groupPosition, parent)
      }
   }

   private fun getOverlayView(
      i: Int,
      viewGroup: ViewGroup?
   ): View {
      val inflater = LayoutInflater.from(context)
      val view = inflater.inflate(R.layout.offline_layer_group, viewGroup, false)
      val overlay = overlays[i]
      val groupView = view.findViewById<TextView>(R.id.cache_over_group_text)
      groupView.text = event?.name + " Layers"
      view.findViewById<View>(R.id.section_header).visibility = if (i == 0) View.VISIBLE else View.GONE
      val imageView = view.findViewById<ImageView>(R.id.cache_overlay_group_image)
      val cacheName = view.findViewById<TextView>(R.id.cache_overlay_group_name)
      val childCount = view.findViewById<TextView>(R.id.cache_overlay_group_count)
      val checkable = view.findViewById<View>(R.id.cache_overlay_group_checkbox)
      checkable.setOnClickListener { v ->
         val checked = (v as Checkable).isChecked
         overlay.isEnabled = checked
         var modified = false
         for (childCache: CacheOverlay in overlay.children) {
            if (childCache.isEnabled != checked) {
               childCache.isEnabled = checked
               modified = true
            }
         }
         if (modified) {
            notifyDataSetChanged()
         }
      }
      val imageResource = overlay.iconImageResourceId
      if (imageResource != null) {
         imageView.setImageResource(imageResource)
      }
      var layer: Layer? = null
      if (overlay is GeoPackageCacheOverlay) {
         val filePath = overlay.filePath
         if (filePath.startsWith(
               String.format(
                  "%s/MAGE/geopackages",
                  context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
               )
            )
         ) {
            try {
               val relativePath = filePath.split(
                  (context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
                     .absolutePath + "/").toRegex()
               ).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
               layer = layerLocalDataSource.getByRelativePath(relativePath)
            } catch (e: Exception) {
               Log.e(LOG_NAME, "Error getting layer by relative path", e)
            }
         }
      }
      cacheName.text = if (layer != null) layer.name else overlay.name
      if (overlay.isSupportsChildren) {
         childCount.text = "(" + getChildrenCount(i) + " layers)"
      } else {
         childCount.text = ""
      }
      (checkable as Checkable).isChecked = overlay.isEnabled
      return view
   }

   private fun getOverlaySideloadedView(
      i: Int,
      viewGroup: ViewGroup?
   ): View {
      val inflater = LayoutInflater.from(context)
      val view = inflater.inflate(R.layout.offline_layer_sideloaded, viewGroup, false)
      val overlay = sideloadedOverlays[i - overlays.size]
      val groupView = view.findViewById<TextView>(R.id.cache_overlay_side_group_text)
      groupView.text = "My Layers"
      view.findViewById<View>(R.id.section_header).visibility =
         if (i - overlays.size == 0) View.VISIBLE else View.GONE
      val imageView = view.findViewById<ImageView>(R.id.cache_overlay_side_group_image)
      val cacheName = view.findViewById<TextView>(R.id.cache_overlay_side_group_name)
      val childCount = view.findViewById<TextView>(R.id.cache_overlay_side_group_count)
      val checkable = view.findViewById<View>(R.id.cache_overlay_side_group_checkbox)
      checkable.setOnClickListener { v ->
         val checked = (v as Checkable).isChecked
         overlay.isEnabled = checked
         var modified = false
         for (childCache: CacheOverlay in overlay.children) {
            if (childCache.isEnabled != checked) {
               childCache.isEnabled = checked
               modified = true
            }
         }
         if (modified) {
            notifyDataSetChanged()
         }
      }
      val imageResource = overlay.iconImageResourceId
      if (imageResource != null) {
         imageView.setImageResource(imageResource)
      }
      var layer: Layer? = null
      if (overlay is GeoPackageCacheOverlay) {
         val filePath = overlay.filePath
         if (filePath.startsWith(
               String.format(
                  "%s/MAGE/geopackages",
                  context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
               )
            )
         ) {
            try {
               val relativePath = filePath.split(
                  (context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
                     .absolutePath + "/").toRegex()
               ).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
               layer = layerLocalDataSource.getByRelativePath(relativePath)
            } catch (e: Exception) {
               Log.e(LOG_NAME, "Error getting layer by relative path", e)
            }
         }
      }
      cacheName.text = if (layer != null) layer.name else overlay.name
      if (overlay.isSupportsChildren) {
         childCount.text = "(" + getChildrenCount(i) + " layers)"
      } else {
         childCount.text = ""
      }
      (checkable as Checkable).isChecked = overlay.isEnabled
      return view
   }

   private fun getDownloadableLayerView(
      i: Int,
      viewGroup: ViewGroup?
   ): View {
      val inflater = LayoutInflater.from(context)
      val view = inflater.inflate(R.layout.layer_overlay, viewGroup, false)
      val layer = downloadableLayers[i - overlays.size - sideloadedOverlays.size]
      view.findViewById<View>(R.id.section_header).visibility =
         if (i - overlays.size - sideloadedOverlays.size == 0) View.VISIBLE else View.GONE
      val cacheName = view.findViewById<TextView>(R.id.layer_name)
      cacheName.text = layer.name
      val description = view.findViewById<TextView>(R.id.layer_description)
      if (layer.type.equals("geopackage", ignoreCase = true)) {
         description.text = ByteUtils.getInstance().getDisplay(layer.fileSize, true)
      } else {
         description.text = "Static feature data"
      }
      val progressBar = view.findViewById<LinearProgressIndicator>(R.id.layer_progress)
      val download = view.findViewById<View>(R.id.layer_download)
      if (layer.type.equals("geopackage", ignoreCase = true)) {
         if (downloadManager.isDownloading(layer)) {
            val progress = downloadManager.getProgress(layer)
            val fileSize = layer.fileSize
            progressBar.visibility = View.VISIBLE
            download.visibility = View.GONE
            view.isEnabled = false
            view.setOnClickListener(null)
            val currentProgress = (progress / layer.fileSize.toFloat() * 100).toInt()
            progressBar.isIndeterminate = false
            progressBar.progress = currentProgress
            val layerSize = view.findViewById<TextView>(R.id.layer_size)
            layerSize.visibility = View.VISIBLE
            layerSize.text = String.format(
               "Downloading: %s of %s",
               Formatter.formatFileSize(context, progress.toLong()),
               Formatter.formatFileSize(context, fileSize)
            )
         } else {
            val reason = downloadManager.isFailed(layer)
            if (!StringUtils.isEmpty(reason)) {
               Toast.makeText(context, reason, Toast.LENGTH_LONG).show()
            }
            progressBar.visibility = View.GONE
            download.visibility = View.VISIBLE
         }
      } else if (layer.type.equals("feature", ignoreCase = true)) {
         if (!layer.isLoaded && layer.downloadId == null) {
            download.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
         } else {
            download.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            progressBar.isIndeterminate = true
         }
      }
      download.setOnClickListener {
         download.visibility = View.GONE
         progressBar.isIndeterminate = true
         progressBar.visibility = View.VISIBLE
         if (layer.type.equals("geopackage", ignoreCase = true)) {
            downloadManager.downloadGeoPackage(event, layer)
         } else if (layer.type.equals("feature", ignoreCase = true)) {
            CoroutineScope(Dispatchers.IO).launch {
               try {
                  cacheProvider.refreshTileOverlays()
                  layerRepository.loadFeatures(layer)

                  CoroutineScope(Dispatchers.Main).launch {
                     downloadableLayers.remove(layer)
                     overlays.clear()
                     sideloadedOverlays.clear()
                     notifyDataSetChanged()
                  }
               } catch (e: Exception) {
                  Log.w(LOG_NAME, "Error fetching static layers", e)
               }
            }
         }
      }
      view.tag = layer.name
      return view
   }

   override fun getChildView(
      groupPosition: Int,
      childPosition: Int,
      isLastChild: Boolean,
      convertView: View?,
      parent: ViewGroup
   ): View {
      val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.offline_layer_child, parent, false)

      val overlay = if (groupPosition < overlays.size) overlays[groupPosition] else sideloadedOverlays[groupPosition - overlays.size]
      val childCache = overlay.children[childPosition]
      val imageView = view?.findViewById<ImageView>(R.id.cache_overlay_child_image)
      val tableName = view?.findViewById<TextView>(R.id.cache_overlay_child_name)
      val info = view?.findViewById<TextView>(R.id.cache_overlay_child_info)
      val checkBox = view?.findViewById<View>(R.id.cache_overlay_child_checkbox)
      view?.findViewById<View>(R.id.divider)?.visibility = if (isLastChild) View.VISIBLE else View.INVISIBLE
      checkBox?.setOnClickListener { v ->
         val checked = (v as Checkable).isChecked
         childCache.isEnabled = checked
         var modified = false
         if (checked) {
            if (!overlay.isEnabled) {
               overlay.isEnabled = true
               modified = true
            }
         } else if (overlay.isEnabled) {
            modified = true
            for (childCache: CacheOverlay in overlay.children) {
               if (childCache.isEnabled) {
                  modified = false
                  break
               }
            }
            if (modified) {
               overlay.isEnabled = false
            }
         }
         if (modified) {
            notifyDataSetChanged()
         }
      }
      tableName?.text = childCache.name
      info?.text = childCache.info
      (checkBox as Checkable).isChecked = childCache.isEnabled
      val imageResource = childCache.iconImageResourceId
      if (imageResource != null) {
         imageView?.setImageResource(imageResource)
      }
      return view
   }

   override fun isChildSelectable(i: Int, j: Int): Boolean {
      return true
   }

   companion object {
      private val LOG_NAME = OfflineLayersAdapter::class.java.name
   }
}