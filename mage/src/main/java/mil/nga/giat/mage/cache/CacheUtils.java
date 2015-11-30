package mil.nga.giat.mage.cache;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.util.Map;

import mil.nga.geopackage.validate.GeoPackageValidate;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.utils.MediaUtility;
import mil.nga.giat.mage.sdk.utils.StorageUtility;

/**
 * Cache File Utilities
 */
public class CacheUtils {

    /**
     * Copy the Uri to the cache directory in a background task
     *
     * @param activity
     * @param uri
     * @param path bn
     */
    public static void copyToCache(Activity activity, Uri uri, String path) {

        // Get the Uri display name, which should be the file name with extension
        String name = MediaUtility.getDisplayName(activity, uri, path);

        // Get a cache directory to write to
        File cacheDirectory = CacheUtils.getWritableCacheDirectory(activity);
        if (cacheDirectory != null) {

            // Verify that the file is a cache file by its extension
            File cacheFile = new File(cacheDirectory, name);
            if (isCacheFile(cacheFile)) {

                // Copy the file in a background task
                CopyCacheStreamTask task = new CopyCacheStreamTask(activity, uri, cacheFile);
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
    public static File getWritableCacheDirectory(Context context) {

        File directory = null;

        Map<StorageUtility.StorageType, File> storageLocations = StorageUtility.getAllStorageLocations();
        for (File storageLocation : storageLocations.values()) {
            File temp = new File(storageLocation, context.getString(R.string.overlay_cache_directory));

            if (temp.exists()) {
                if (temp.canWrite()) {
                    directory = temp;
                }
            } else if (temp.mkdirs()) {
                directory = temp;
            }

            if (directory != null) {
                break;
            }
        }

        return directory;
    }

}
