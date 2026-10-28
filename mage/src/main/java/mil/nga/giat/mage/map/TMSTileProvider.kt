package mil.nga.giat.mage.map

import mil.nga.giat.mage.map.cache.URLCacheOverlay
import kotlin.math.pow

class TMSTileProvider(
   width: Int,
   height: Int,
   private val myOverlay: URLCacheOverlay,
   userAgent: String
) : HttpTileProvider(width, height, userAgent) {

   override fun getTilePath(x: Int, y: Int, z: Int): String {
      val yTranslation = 2.0.pow(z.toDouble()).toLong() - y - 1

      return myOverlay.url.toString()
         .replace("{x}", x.toString())
         .replace("{y}", yTranslation.toString())
         .replace("{z}", z.toString())
   }
}
