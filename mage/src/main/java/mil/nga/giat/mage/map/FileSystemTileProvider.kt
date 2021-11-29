package mil.nga.giat.mage.map

import android.util.Log
import com.google.android.gms.maps.model.UrlTileProvider
import java.net.MalformedURLException
import java.net.URL

class FileSystemTileProvider(
   width: Int,
   height: Int,
   private val baseDirectory: String
) : UrlTileProvider(width, height) {

   override fun getTileUrl(x: Int, y: Int, z: Int): URL? {
      return try {
          URL("file://${baseDirectory}/${z}/${x}/${y}.png")
      } catch (e: MalformedURLException) {
         Log.e(LOG_NAME, "Could not form tile URL", e)
         null
      }
   }

   companion object {
      private val LOG_NAME = FileSystemTileProvider::class.java.name
   }
}