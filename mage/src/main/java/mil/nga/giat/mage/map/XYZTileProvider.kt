package mil.nga.giat.mage.map

import mil.nga.giat.mage.map.cache.URLCacheOverlay

class XYZTileProvider(
   width: Int,
   height: Int,
   private val myOverlay: URLCacheOverlay,
   userAgent: String
) : HttpTileProvider(width, height, userAgent) {

   override fun getTilePath(x: Int, y: Int, z: Int): String {
      return myOverlay.url.toString()
         .replace("{x}", x.toString())
         .replace("{y}", y.toString())
         .replace("{z}", z.toString())
   }
}
