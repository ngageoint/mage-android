package mil.nga.giat.mage.map.cache;

/**
 * GeoPackage Table cache overlay
 *
 * @author osbornb
 */
public abstract class GeoPackageTableCacheOverlay extends CacheOverlay {

    /**
     * GeoPackage name
     */
    private final String geoPackage;

    /**
     * Count of data in the table
     */
    private final int count;

    /**
     * Min zoom level of the data
     */
    private final int minZoom;

    /**
     * Max zoom level of the data
     */
    private final int maxZoom;

    /**
     * Cache overlay parent
     */
    private CacheOverlay parent;

    /**
     * Constructor
     *
     * @param name       GeoPackage table name
     * @param geoPackage GeoPackage name
     * @param cacheName  Cache name
     * @param type       cache type
     * @param count      count
     * @param minZoom    min zoom level
     * @param maxZoom    max zoom level
     */
    protected GeoPackageTableCacheOverlay(String name, String geoPackage, String cacheName, CacheOverlayType type, int count, int minZoom, Integer maxZoom) {
        super(name, cacheName, type, false);

        this.geoPackage = geoPackage;
        this.count = count;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
    }

    @Override
    public boolean isChild(){
        return true;
    }

    @Override
    public CacheOverlay getParent(){
        return parent;
    }

    /**
     * Set the parent cache overlay
     *
     * @param parent
     */
    public void setParent(CacheOverlay parent) {
        this.parent = parent;
    }

    /**
     * Get the GeoPackage name
     *
     * @return
     */
    public String getGeoPackage() {
        return geoPackage;
    }

    /**
     * Get the count
     *
     * @return
     */
    public int getCount() {
        return count;
    }

    /**
     * Get the min zoom
     *
     * @return
     */
    public int getMinZoom() {
        return minZoom;
    }

    /**
     * Get the max zoom
     *
     * @return
     */
    public int getMaxZoom() {
        return maxZoom;
    }

}
