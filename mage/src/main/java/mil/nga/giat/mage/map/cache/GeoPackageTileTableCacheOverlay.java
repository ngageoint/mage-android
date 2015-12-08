package mil.nga.giat.mage.map.cache;

import com.google.android.gms.maps.model.TileOverlay;

import mil.nga.giat.mage.R;

/**
 * GeoPackage Tile Table cache overlay
 *
 * @author osbornb
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
     * @param count      count
     * @param minZoom    min zoom level
     * @param maxZoom    max zoom level
     */
    public GeoPackageTileTableCacheOverlay(String name, String geoPackage, String cacheName, int count, int minZoom, int maxZoom) {
        super(name, geoPackage, cacheName, CacheOverlayType.GEOPACKAGE_TILE_TABLE, count, minZoom, maxZoom);
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
        return R.drawable.ic_layers_black_24dp;
    }

    @Override
    public String getInfo() {
        return "tiles: " + getCount() + ", zoom: " + getMinZoom() + " - " + getMaxZoom();
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
