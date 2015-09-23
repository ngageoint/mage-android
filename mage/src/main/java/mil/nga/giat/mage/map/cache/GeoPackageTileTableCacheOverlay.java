package mil.nga.giat.mage.map.cache;

import com.google.android.gms.maps.model.TileOverlay;

import mil.nga.giat.mage.R;

/**
 * GeoPackage Tile Table cache overlay
 */
public class GeoPackageTileTableCacheOverlay extends GeoPackageTableCacheOverlay {

    /**
     * Tile Overlay
     */
    private TileOverlay tileOverlay;

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

    @Override
    public void removeFromMap() {
        if (tileOverlay != null) {
            tileOverlay.remove();
            tileOverlay = null;
        }
    }

    @Override
    public Integer getIconImageResourceId() {
        return R.drawable.ic_layers;
    }

    /**
     * Get the tile overlay
     *
     * @return
     */
    public TileOverlay getTileOverlay() {
        return tileOverlay;
    }

    /**
     * Set the tile overlay
     *
     * @param tileOverlay
     */
    public void setTileOverlay(TileOverlay tileOverlay) {
        this.tileOverlay = tileOverlay;
    }

}
