package mil.nga.giat.mage.data.layer

import android.app.Application
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper
import mil.nga.sf.GeometryType
import mil.nga.sf.LineString
import mil.nga.sf.Point
import mil.nga.sf.Polygon
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.*
import javax.inject.Inject

class LayerRepository @Inject constructor(
   private val application: Application,
) {

   sealed class StaticFeatureEvent {
      data class Point(val layerId: Long, val content: String, val options: MarkerOptions): StaticFeatureEvent()
      data class Polyline(val layerId: Long, val content: String, val options: PolylineOptions): StaticFeatureEvent()
      data class Polygon(val layerId: Long, val content: String, val options: PolygonOptions): StaticFeatureEvent()
   }

   private val layerHelper = LayerHelper.getInstance(application)

   fun getStaticFeatureEvents(layerId: Long): Flow<StaticFeatureEvent> = channelFlow {
      val layer = layerHelper.read(layerId)

      for (feature in layer.staticFeatures) {
         val geometry = feature.geometry
         val properties = feature.propertiesMap
         val content = StringBuilder()

         if (properties["name"] != null) {
            content.append("<h5>").append(properties["name"]?.value).append("</h5>")
         }

         if (properties["description"] != null) {
            content.append("<div>").append(properties["description"]?.value).append("</div>")
         }

         val type = geometry.geometryType
         if (type == GeometryType.POINT) {
            val point = geometry as Point
            val options = MarkerOptions().position(LatLng(point.y, point.x)).snippet(content.toString())

            // check to see if there's an icon
            val iconPath = feature.localPath
            if (iconPath != null) {
               val iconFile = File(iconPath)
               if (iconFile.exists()) {
                  val bitmapOptions = BitmapFactory.Options()
                  bitmapOptions.inDensity = 480
                  bitmapOptions.inTargetDensity = application.resources.displayMetrics.densityDpi
                  try {
                     val bitmap = BitmapFactory.decodeStream(FileInputStream(iconFile), null, bitmapOptions)
                     if (bitmap != null) {
                        options.icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                     }
                  } catch (e: FileNotFoundException) {
                     Log.e(LOG_NAME, "Could not set icon.", e)
                  }
               }
            }

            send(StaticFeatureEvent.Point(layerId, content.toString(), options))
         } else if (type == GeometryType.LINESTRING) {
            val options = PolylineOptions()
            val property = properties["stylelinestylecolorrgb"]
            if (property != null) {
               val color = property.value
               options.color(Color.parseColor(color))
            }
            val lineString = geometry as LineString
            for (point in lineString.points) {
               options.add(LatLng(point.y, point.x))
            }

            send(StaticFeatureEvent.Polyline(layerId, content.toString(), options))
         } else if (type == GeometryType.POLYGON) {
            val options = PolygonOptions()
            var color: Int? = null
            var property = properties["stylelinestylecolorrgb"]
            if (property != null) {
               val colorProperty = property.value
               color = Color.parseColor(colorProperty)
               options.strokeColor(color)
            } else {
               property = properties["stylepolystylecolorrgb"]
               if (property != null) {
                  val colorProperty = property.value
                  color = Color.parseColor(colorProperty)
                  options.strokeColor(color)
               }
            }
            property = properties["stylepolystylefill"]
            if (property != null) {
               val fill = property.value
               if ("1" == fill && color != null) {
                  options.fillColor(color)
               }
            }
            val polygon = geometry as Polygon
            val rings = polygon.rings
            val polygonLineString = rings[0]
            for (point in polygonLineString.points) {
               val latLng = LatLng(point.y, point.x)
               options.add(latLng)
            }
            for (i in 1 until rings.size) {
               val hole = rings[i]
               val holeLatLngs: MutableList<LatLng> = ArrayList()
               for (point in hole.points) {
                  val latLng = LatLng(point.y, point.x)
                  holeLatLngs.add(latLng)
               }
               options.addHole(holeLatLngs)
            }

            send(StaticFeatureEvent.Polygon(layerId, content.toString(), options))
         }
      }
   }.buffer(capacity = Channel.UNLIMITED)

   companion object {
      private val LOG_NAME = LayerRepository::class.java.simpleName
   }
}