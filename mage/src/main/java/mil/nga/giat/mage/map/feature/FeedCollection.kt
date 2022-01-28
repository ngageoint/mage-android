package mil.nga.giat.mage.map.feature

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import mil.nga.giat.mage.map.MapViewModel
import mil.nga.giat.mage.map.annotation.MapAnnotation

class FeedCollection(
   private val context: Context,
   private val map: GoogleMap,
   private val iconDimension: Int = 32
) {
   private val feeds = mutableMapOf<String, FeatureCollection<String>>()

   fun add(feedWithItems: MapViewModel.FeedState) {
      val featureCollection = feeds[feedWithItems.feed.id] ?: FeatureCollection(context, map, iconDimension)
      feeds[feedWithItems.feed.id] = featureCollection

      if (!feedWithItems.feed.itemsHaveIdentity) {
         featureCollection.clear()
      }

      featureCollection.add(feedWithItems.items)
   }

   fun clear() {
      feeds.values.forEach { it.clear() }
      feeds.clear()
   }

   fun onMarkerClick(marker: Marker): MapAnnotation<String>? {
      feeds.forEach { (feedId, featureCollection) ->
         featureCollection.mapAnnotation(marker, feedId)?.let { annotation ->
            return annotation
         }
      }

      return null
   }

   fun offMarkerClick() {
      feeds.values.forEach { it.offMarkerClick() }
   }
}