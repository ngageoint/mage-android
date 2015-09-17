package mil.nga.giat.mage.map.cache;

/**
 * GeoPackage Table cache overlay
 */
public abstract class GeoPackageTableCacheOverlay extends CacheOverlay {

    /**
     * GeoPackage name
     */
    private final String geoPackage;

    /**
     * Constructor
     *
     * @param name       GeoPackage table name
     * @param geoPackage GeoPackage name
     * @param cacheName  Cache name
     * @param type       cache type
     */
    protected GeoPackageTableCacheOverlay(String name, String geoPackage, String cacheName, CacheOverlayType type) {
        super(name, cacheName, type, false);
        this.geoPackage = geoPackage;
    }

}
