package mil.nga.giat.mage.map.cache;

/**
 * Enumeration of cache overlay types
 *
 * @author osbornb
 */
public enum CacheOverlayType {

    /**
     * Directory of x,y,z tiles
     */
    XYZ_DIRECTORY,

    /**
     * GeoPackage file
     */
    GEOPACKAGE,

    /**
     * GeoPackage tile table
     */
    GEOPACKAGE_TILE_TABLE,

    /**
     * GeoPackage feature table
     */
    GEOPACKAGE_FEATURE_TABLE;

}
