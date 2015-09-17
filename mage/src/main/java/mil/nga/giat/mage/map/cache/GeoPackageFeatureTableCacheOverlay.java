package mil.nga.giat.mage.map.cache;

import mil.nga.giat.mage.R;

/**
 * GeoPackage Feature Table cache overlay
 */
public class GeoPackageFeatureTableCacheOverlay extends GeoPackageTableCacheOverlay {

    /**
     * Constructor
     *
     * @param name       GeoPackage table name
     * @param geoPackage GeoPackage name
     * @param cacheName  Cache name
     */
    public GeoPackageFeatureTableCacheOverlay(String name, String geoPackage, String cacheName) {
        super(name, geoPackage, cacheName, CacheOverlayType.GEOPACKAGE_FEATURE_TABLE);
    }

    public Integer getIconImageResourceId() {
        return R.drawable.ic_place;
    }

}
