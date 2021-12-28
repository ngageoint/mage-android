package mil.nga.giat.mage.utils

import android.net.Uri
import mil.nga.sf.Geometry
import mil.nga.sf.util.GeometryUtils
import java.text.DecimalFormat

fun Geometry.googleMapsUri(): Uri? {
   val latLngFormat = DecimalFormat("###.#####")
   val point = GeometryUtils.getCentroid(this)
   val uriString = "http://maps.google.com/maps?daddr=${latLngFormat.format(point.y)},${latLngFormat.format(point.x)}"
   return Uri.parse(uriString)
}