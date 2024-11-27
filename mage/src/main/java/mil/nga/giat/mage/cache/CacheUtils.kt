package mil.nga.giat.mage.cache

import android.content.Context
import android.net.Uri
import android.os.Environment
import mil.nga.geopackage.GeoPackageConstants
import mil.nga.geopackage.io.GeoPackageIOUtils
import mil.nga.geopackage.validate.GeoPackageValidate
import mil.nga.giat.mage.map.cache.CacheProvider
import mil.nga.giat.mage.sdk.utils.MediaUtility
import java.io.File

class CacheUtils(
   private val context: Context,
   private val cacheProvider: CacheProvider
) {

   /**
    * Copy the Uri to the cache directory in a background task
    *
    * @param uri
    * @param path
    */
   suspend fun copyToCache(uri: Uri, path: String?) {

      // Get a cache directory to write to
      val cacheDirectory = getApplicationCacheDirectory(context)

      // Get the Uri display name, which should be the file name with extension
      var name = MediaUtility.getDisplayName(context, uri, path)

      // If no extension, add a GeoPackage extension
      if (GeoPackageIOUtils.getFileExtension(File(name)) == null) {
         name += "." + GeoPackageConstants.EXTENSION
      }

      // Verify that the file is a cache file by its extension
      val cacheFile = File(cacheDirectory, name)
      if (isCacheFile(cacheFile)) {
         if (cacheFile.exists()) {
            cacheFile.delete()
         }
         val cacheName = MediaUtility.getFileNameWithoutExtension(cacheFile)
         cacheProvider.removeCacheOverlay(cacheName)

         copyCacheStream(uri, cacheFile, cacheName)
      }
   }

   /**
    * Determine if the file is a cache file based upon its extension
    *
    * @param file potential cache file
    * @return true if a cache file
    */
   private fun isCacheFile(file: File?): Boolean {
      return GeoPackageValidate.hasGeoPackageExtension(file)
   }

   private suspend fun copyCacheStream(uri: Uri, cacheFile: File, cacheName: String) {
      context.contentResolver.openInputStream(uri)?.use { input ->
         cacheFile.outputStream().use { output ->
            input.copyTo(output)
         }
      }

      cacheProvider.enableAndRefreshTileOverlays(cacheName)
   }

   companion object {
      private var CACHE_DIRECTORY = "caches"

      /**
       * Get a writeable cache directory for saving cache files
       *
       * @param context
       * @return file directory or null
       */
       fun getApplicationCacheDirectory(context: Context): File {
         var directory = context.filesDir
         val state = Environment.getExternalStorageState()
         if (Environment.MEDIA_MOUNTED == state) {
            val externalDirectory = context.getExternalFilesDir(null)
            if (externalDirectory != null) {
               directory = externalDirectory
            }
         }
         val cacheDirectory = File(directory, CACHE_DIRECTORY)
         if (!cacheDirectory.exists()) {
            cacheDirectory.mkdir()
         }
         return cacheDirectory
      }
   }
}