package mil.nga.giat.mage.cache;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import java.io.File;

import mil.nga.geopackage.GeoPackageConstants;
import mil.nga.geopackage.io.GeoPackageIOUtils;
import mil.nga.geopackage.validate.GeoPackageValidate;
import mil.nga.giat.mage.map.cache.CacheProvider;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

/**
 * Cache File Utilities
 */
public class CacheUtils {

    public static String CACHE_DIRECTORY = "caches";

    /**
     * Copy the Uri to the cache directory in a background task
     *
     * @param context
     * @param uri
     * @param path bn
     */
    public static void copyToCache(Context context, Uri uri, String path) {

        // Get a cache directory to write to
        File cacheDirectory = CacheUtils.getApplicationCacheDirectory(context);
        if (cacheDirectory != null) {

            // Get the Uri display name, which should be the file name with extension
            String name = MediaUtility.getDisplayName(context, uri, path);

            // If no extension, add a GeoPackage extension
            if(GeoPackageIOUtils.getFileExtension(new File(name)) == null){
                name += "." + GeoPackageConstants.GEOPACKAGE_EXTENSION;
            }

            // Verify that the file is a cache file by its extension
            File cacheFile = new File(cacheDirectory, name);
            if (isCacheFile(cacheFile)) {

                if(cacheFile.exists()) {
                    cacheFile.delete();
                }
                String cacheName = MediaUtility.getFileNameWithoutExtension(cacheFile);
                CacheProvider.getInstance(context).removeCacheOverlay(cacheName);

                // Copy the file in a background task
                CopyCacheStreamTask task = new CopyCacheStreamTask(context, uri, cacheFile, cacheName);
                task.execute();
            }
        }
    }

    /**
     * Determine if the file is a cache file based upon its extension
     *
     * @param file potential cache file
     * @return true if a cache file
     */
    public static boolean isCacheFile(File file) {
        return GeoPackageValidate.hasGeoPackageExtension(file);
    }

    /**
     * Get a writeable cache directory for saving cache files
     *
     * @param context
     * @return file directory or null
     */
    public static File getApplicationCacheDirectory(Context context) {
        File directory = context.getFilesDir();

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File externalDirectory = context.getExternalFilesDir(null);
            if (externalDirectory != null) {
                directory = externalDirectory;
            }
        }

        File cacheDirectory = new File(directory, CACHE_DIRECTORY);
        if (!cacheDirectory.exists()) {
            cacheDirectory.mkdir();
        }

        return cacheDirectory;
    }

}
