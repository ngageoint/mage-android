package mil.nga.giat.mage.map

import android.util.Log
import com.google.android.gms.maps.model.UrlTileProvider
import mil.nga.giat.mage.map.cache.URLCacheOverlay
import java.net.MalformedURLException
import java.net.URL

class XYZTileProvider(
   width: Int,
   height: Int,
   private val myOverlay: URLCacheOverlay
) : UrlTileProvider(width, height) {

   override fun getTileUrl(x: Int, y: Int, z: Int): URL? {
      val path = myOverlay.url.toString()
         .replace("{s}", "")
         .replace("{x}", x.toString())
         .replace("{y}", y.toString())
         .replace("{z}", z.toString())

      return try {
          URL(path)
      } catch (e: MalformedURLException) {
         Log.w(LOG_NAME, "Problem with URL $path", e)
         null
      }
   }

   companion object {
      private val LOG_NAME = XYZTileProvider::class.java.name
   }
}