package mil.nga.giat.mage.map

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import mil.nga.proj.ProjectionConstants
import mil.nga.sf.Geometry
import mil.nga.sf.GeometryType
import mil.nga.sf.Point
import mil.nga.sf.Polygon
import mil.nga.sf.util.GeometryEnvelopeBuilder
import mil.nga.sf.util.GeometryUtils
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Center and zoom map to fit geometry
 *
 * @return
 */
fun GoogleMap.center(geometry: Geometry, zoom: Float? = null) {
   if (geometry.geometryType == GeometryType.POINT) {
      val point = geometry.centroid
      animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(point.y, point.x), zoom ?: this.cameraPosition.zoom))
   } else {
      val copy = geometry.copy()
      GeometryUtils.minimizeGeometry(copy, ProjectionConstants.WGS84_HALF_WORLD_LON_WIDTH)
      val envelope = GeometryEnvelopeBuilder.buildEnvelope(copy)
      val boundsBuilder = LatLngBounds.Builder()
      boundsBuilder.include(LatLng(envelope.minY, envelope.minX))
      boundsBuilder.include(LatLng(envelope.minY, envelope.maxX))
      boundsBuilder.include(LatLng(envelope.maxY, envelope.minX))
      boundsBuilder.include(LatLng(envelope.maxY, envelope.maxX))

      animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
   }
}

/**
 * Get the map point to line distance tolerance
 *
 * @return tolerance
 */
fun GoogleMap.lineTolerance(): Double {
   // how many meters away form the click can the geometry be?
   val circumferenceOfEarthInMeters = 2 * Math.PI * 6371000
   val pixelSizeInMetersAtLatitude =
      circumferenceOfEarthInMeters * cos(cameraPosition.target.latitude * (Math.PI / 180.0)) / 2.0.pow(
         cameraPosition.zoom + 8.0
      )
   return pixelSizeInMetersAtLatitude * sqrt(2.0) * 10.0
}

fun Polygon.hasKinks(): Boolean {
   for (line1 in rings) {
      val lastPoint = line1.points[line1.numPoints() - 1]
      for (line2 in rings) {
         for (i in 0 until line1.numPoints() - 1) {
            val point1 = line1.points[i]
            val nextPoint1 = line1.points[i + 1]
            for (k in i until line2.numPoints() - 1) {
               val point2 = line2.points[k]
               val nextPoint2 = line2.points[k + 1]
               if (line1 !== line2) {
                  continue
               }
               if (abs(i - k) == 1) {
                  continue
               }
               if (i == 0 && k == line1.numPoints() - 2 && point1.x == lastPoint.x && point1.y == lastPoint.y) {
                  continue
               }

               val intersects = intersects(point1, nextPoint1, point2, nextPoint2)

               if (intersects) {
                  return true
               }
            }
         }
      }
   }

   return false
}

private fun intersects(
   point1Start: Point,
   point1End: Point,
   point2Start: Point,
   point2End: Point
): Boolean {
   var q = ( //Distance between the lines' starting rows times line2's horizontal length
           (point1Start.y - point2Start.y) * (point2End.x - point2Start.x) //Distance between the lines' starting columns times line2's vertical length
                   - (point1Start.x - point2Start.x) * (point2End.y - point2Start.y))
   val d = ( //Line 1's horizontal length times line 2's vertical length
           (point1End.x - point1Start.x) * (point2End.y - point2Start.y) //Line 1's vertical length times line 2's horizontal length
                   - (point1End.y - point1Start.y) * (point2End.x - point2Start.x))

   if (d == 0.0) {
      return false
   }

   val r = q / d
   q = ( //Distance between the lines' starting rows times line 1's horizontal length
           (point1Start.y - point2Start.y) * (point1End.x - point1Start.x) //Distance between the lines' starting columns times line 1's vertical length
                   - (point1Start.x - point2Start.x) * (point1End.y - point1Start.y))
   val s = q / d

   return !(r < 0 || r > 1 || s < 0 || s > 1)
}



