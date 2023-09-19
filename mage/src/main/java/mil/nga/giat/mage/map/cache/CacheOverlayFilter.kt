package mil.nga.giat.mage.map.cache

import android.content.Context
import android.os.Environment
import com.google.common.base.Predicate
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import mil.nga.giat.mage.database.model.layer.Layer

class CacheOverlayFilter(
   private val context: Context,
   private val layers: List<Layer>
) {
   private val eventPredicate: Predicate<CacheOverlay> = Predicate { overlay ->
      if (overlay is GeoPackageCacheOverlay) {
         val filePath = overlay.filePath
         val downloadDirectory = "${context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)}/MAGE/geopackages"
         if (filePath.startsWith(downloadDirectory)) {
            layers.any { layer ->
               filePath.endsWith("geopackages/${layer.remoteId}/${layer.fileName}")
            }
         } else {
            true
         }
      } else {
         true
      }
   }

   fun filter(overlays: List<CacheOverlay>): List<CacheOverlay> {
      return Lists.newArrayList(Iterables.filter(overlays, eventPredicate))
   }
}