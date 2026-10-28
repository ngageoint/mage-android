package mil.nga.giat.mage.map

import android.util.Log
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

// UrlTileProvider.getTile() is final and fetches tiles without a User-Agent header,
// which gets the app blocked by openstreetmap.org's tile usage policy. Implement
// TileProvider directly so we can identify the app on every tile request.
abstract class HttpTileProvider(
   private val width: Int,
   private val height: Int,
   private val userAgent: String,
   // Common multi-host CDN shard tokens used by {s} in XYZ/TMS templates
   private val subdomains: List<String> = listOf("a", "b", "c")
) : TileProvider {

   // Returns the tile path/URL with {x}/{y}/{z} resolved but {s} left intact, since
   // subdomain selection and fallback are handled centrally below.
   protected abstract fun getTilePath(x: Int, y: Int, z: Int): String

   override fun getTile(x: Int, y: Int, z: Int): Tile {
      val path = getTilePath(x, y, z)
      if (subdomains.isEmpty() || !path.contains("{s}")) {
         return fetchTile(path)
      }
      val startIndex = ((x + y) % subdomains.size + subdomains.size) % subdomains.size
      return fetchTile(path.replace("{s}", subdomains[startIndex]))
   }

   private fun fetchTile(path: String): Tile {
      val url = toUrl(path) ?: return TileProvider.NO_TILE

      return try {
         val connection = url.openConnection() as HttpURLConnection
         connection.setRequestProperty("User-Agent", userAgent)
         connection.connect()
         if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            connection.disconnect()
            return TileProvider.NO_TILE
         }

         val data = connection.inputStream.use { it.readBytes() }
         connection.disconnect()
         Tile(width, height, data)
      } catch (e: IOException) {
         Log.w(LOG_NAME, "Error fetching tile from $url", e)
         TileProvider.NO_TILE
      }
   }

   private fun toUrl(path: String): URL? {
      return try {
         URL(path)
      } catch (e: MalformedURLException) {
         Log.w(LOG_NAME, "Problem with URL $path", e)
         null
      }
   }

   companion object {
      private val LOG_NAME = HttpTileProvider::class.java.name
   }
}
