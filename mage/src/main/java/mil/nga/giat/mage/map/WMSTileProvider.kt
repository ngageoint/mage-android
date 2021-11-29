package mil.nga.giat.mage.map

import android.util.Log
import com.google.android.gms.maps.model.UrlTileProvider
import mil.nga.giat.mage.map.cache.URLCacheOverlay
import mil.nga.giat.mage.map.cache.WMSCacheOverlay
import java.net.MalformedURLException
import java.net.URL
import kotlin.math.*

class WMSTileProvider(
   private val myWidth: Int,
   private val myHeight: Int,
   overlay: URLCacheOverlay
) : UrlTileProvider(myWidth, myHeight) {

   val overlay: WMSCacheOverlay = overlay as WMSCacheOverlay

   override fun getTileUrl(x: Int, y: Int, z: Int): URL? {
      val version = overlay.wmsVersion
      var epsgKey = "SRS"
      if (version != null && (version == "1.3" || version == "1.3.0")) {
         epsgKey = "CRS"
      }

      var transparentValue = "false"
      if (overlay.wmsTransparent != null) {
         val transparent = overlay.wmsTransparent.toBoolean()
         if (transparent) {
            transparentValue = "true"
         }
      }

      val path = StringBuilder(overlay.url.toString())
      path.append("?request=GetMap&service=WMS")
      if (overlay.wmsStyles != null) {
         path.append("&styles=" + overlay.wmsStyles)
      }
      if (overlay.wmsLayers != null) {
         path.append("&layers=" + overlay.wmsLayers)
      }
      if (version != null) {
         path.append("&version=$version")
      }
      path.append("&$epsgKey=EPSG:3857")
      path.append("&width=$myWidth")
      path.append("&height=$myHeight")
      path.append("&format=" + overlay.wmsFormat)
      path.append("&transparent=$transparentValue")
      path.append(buildBBox(x, y, z))

      return try {
          URL(path.toString())
      } catch (e: MalformedURLException) {
         Log.w(LOG_NAME, "Problem with URL $path", e)
         null
      }
   }

   private fun getX(x: Int, z: Int): Double {
      return x / 2.0.pow(z.toDouble()) * 360.0 - 180
   }

   private fun getY(y: Int, z: Int): Double {
      val n = Math.PI - 2.0 * Math.PI * y / 2.0.pow(z.toDouble())
      return 180.0 / Math.PI * atan(0.5 * (exp(n) - exp(-n)))
   }

   private fun mercatorXOfLongitude(lon: Double): Double {
      return lon * 20037508.34 / 180
   }

   private fun mercatorYOfLatitude(lat: Double): Double {
      var y = ln(tan((90 + lat) * Math.PI / 360)) / (Math.PI / 180)
      y = y * 20037508.34 / 180
      return y
   }

   private fun buildBBox(x: Int, y: Int, z: Int): String {
      val left = mercatorXOfLongitude(getX(x, z))
      val right = mercatorXOfLongitude(getX(x + 1, z))
      val bottom = mercatorYOfLatitude(getY(y + 1, z))
      val top = mercatorYOfLatitude(getY(y, z))
      return "&BBOX=$left,$bottom,$right,$top"
   }

   companion object {
      private val LOG_NAME = WMSTileProvider::class.java.name
   }

}