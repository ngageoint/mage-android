package mil.nga.giat.mage.map.feature

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import mil.nga.giat.mage.map.annotation.MapAnnotation

class StaticFeatureCollection(
   private val context: Context,
   private val map: GoogleMap,
   private val iconDimension: Int = 32,
) {
   private val layers = mutableMapOf<String, FeatureCollection<Long>>()

   fun add(layerId: String, annotations: List<MapAnnotation<Long>>) {
      val featureCollection = layers[layerId] ?: FeatureCollection(context, map, iconDimension)
      layers[layerId] = featureCollection

      featureCollection.clear() // TODO ids should map, maybe we don't need to clear here?

      featureCollection.add(annotations)
   }

   fun clear() {
      layers.values.forEach { it.clear() }
      layers.clear()
   }

   fun onMarkerClick(marker: Marker): MapAnnotation<Long>? {
      layers.forEach { (layerName, featureCollection) ->
         featureCollection.mapAnnotation(marker, layerName)?.let { annotation ->
            return annotation
         }
      }

      return null
   }

   fun offMarkerClick() {
      layers.values.forEach { it.offMarkerClick() }
   }

   fun onMapClick(latLng: LatLng): MapAnnotation<Long>? {
      layers.forEach { (layerName, featureCollection) ->
         featureCollection.onMapClick(latLng)?.let { annotation ->
            return annotation
         }
      }

      return null
   }
}