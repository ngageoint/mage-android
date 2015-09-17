package mil.nga.giat.mage.map.cache;

import mil.nga.giat.mage.R;

/**
 * GeoPackage Tile Table cache overlay
 */
public class GeoPackageTileTableCacheOverlay extends GeoPackageTableCacheOverlay {

    /**
     * Constructor
     *
     * @param name       GeoPackage table name
     * @param geoPackage GeoPackage name
     * @param cacheName  Cache name
     */
    public GeoPackageTileTableCacheOverlay(String name, String geoPackage, String cacheName) {
        super(name, geoPackage, cacheName, CacheOverlayType.GEOPACKAGE_TILE_TABLE);
    }

    public Integer getIconImageResourceId() {
        return R.drawable.ic_layers;
    }

}
