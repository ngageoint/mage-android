package mil.nga.giat.mage.map

import android.util.Log
import com.google.android.gms.maps.model.UrlTileProvider
import mil.nga.giat.mage.map.cache.URLCacheOverlay
import java.net.MalformedURLException
import java.net.URL
import kotlin.math.pow

class TMSTileProvider(
   width: Int,
   height: Int,
   private val myOverlay: URLCacheOverlay
) : UrlTileProvider(width, height) {

   override fun getTileUrl(x: Int, y: Int, z: Int): URL? {
      val yTranslation = 2.0.pow(z.toDouble()).toLong() - y - 1

      val path = myOverlay.url.toString()
         .replace("{s}", "")
         .replace("{x}", x.toString())
         .replace("{y}", yTranslation.toString())
         .replace("{z}", z.toString())

      return try {
          URL(path)
      } catch (e: MalformedURLException) {
         Log.w(LOG_NAME, "Problem with URL $path", e)
         null
      }
   }

   companion object {
      private val LOG_NAME = TMSTileProvider::class.java.name
   }
}