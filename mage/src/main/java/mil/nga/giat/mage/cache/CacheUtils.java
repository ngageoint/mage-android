package mil.nga.giat.mage.cache;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
     * Copy the Uri to the cache directory
     *
     * @param context
     * @param uri
     * @param path
     */
    public static void copyToCache(Context context, Uri uri, String path) {

        String name = MediaUtility.getDisplayName(context, uri, path);
        File cacheDirectory = CacheUtils.getWritableCacheDirectory(context);
        if (cacheDirectory != null) {
            File cacheFile = new File(cacheDirectory, name);
            if (isCacheFile(cacheFile)) {
                // TODO Do this as a background task
                final ContentResolver resolver = context.getContentResolver();
                try {
                    InputStream stream = resolver.openInputStream(uri);
                    MediaUtility.copyStream(stream, cacheFile);
                } catch (IOException e) {
                    // TODO
                    String TODO = "TODO";
                }
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
