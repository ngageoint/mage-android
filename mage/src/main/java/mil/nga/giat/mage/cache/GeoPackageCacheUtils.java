package mil.nga.giat.mage.cache;


import android.content.Context;
import android.util.Log;

import java.io.File;

import mil.nga.geopackage.GeoPackageFactory;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

/**
 * GeoPackage cache utils
 */
public class GeoPackageCacheUtils {

    /**
     * Import the GeoPackage file as an external link if it does not exist
     *
     * @param context
     * @param cacheFile
     * @return cache name when imported, null when not imported
     */
    public static String importGeoPackage(Context context, File cacheFile) {
        GeoPackageManager manager = GeoPackageFactory.getManager(context);
        return importGeoPackage(manager, cacheFile);
    }

    /**
     * Import the GeoPackage file as an external link if it does not exist
     *
     * @param manager
     * @param cacheFile
     * @return cache name when imported, null when not imported
     */
    public static String importGeoPackage(GeoPackageManager manager, File cacheFile) {

        String importedCacheName = null;

        if (!manager.existsAtExternalFile(cacheFile)) {

            // Get the cache name
            String cacheName = getCacheName(manager, cacheFile);

            // Import the GeoPackage as a linked file
            try{
                if (manager.importGeoPackageAsExternalLink(cacheFile, cacheName)) {
                    importedCacheName = cacheName;
                }
            }catch(Exception e){
                Log.e(GeoPackageCacheUtils.class.getSimpleName(), "Failed to import file as GeoPackage. path: " + cacheFile.getAbsolutePath() + ", name: " + cacheName + ", error: " +e.getMessage());

                // Delete the cache file
                if(cacheFile.canWrite()){
                    try {
                        cacheFile.delete();
                    }catch(Exception error){
                        Log.e(GeoPackageCacheUtils.class.getSimpleName(), "Failed to delete file: " + cacheFile.getAbsolutePath() + ", error: " + error.getMessage());
                    }
                }

            }
        }

        return importedCacheName;
    }

    /**
     * Get a cache name for the cache file
     *
     * @param manager
     * @param cacheFile
     * @return cache name
     */
    public static String getCacheName(GeoPackageManager manager, File cacheFile) {

        // Get the cache name
        String cacheName = MediaUtility.getFileNameWithoutExtension(cacheFile);

        // Adjust the name until it is unique
        final String baseCacheName = cacheName;
        int nameCount = 0;
        while (manager.exists(cacheName)) {
            cacheName = baseCacheName + "_" + (++nameCount);
        }

        return cacheName;
    }

}
