package mil.nga.giat.mage.map.cache;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;

import java.util.ArrayList;
import java.util.List;

import mil.nga.geopackage.map.tiles.overlay.FeatureOverlayQuery;
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
     * Used to query the backing feature tables
     */
    private List<FeatureOverlayQuery> featureOverlayQueries = new ArrayList<>();

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
        return R.drawable.ic_layers_gray_24dp;
    }

    @Override
    public String getInfo() {
        return "tiles: " + getCount() + ", zoom: " + getMinZoom() + " - " + getMaxZoom();
    }

    @Override
    public String onMapClick(LatLng latLng, MapView mapView, GoogleMap map) {
        StringBuilder message = new StringBuilder();

        for(FeatureOverlayQuery featureOverlayQuery: featureOverlayQueries){
            String overlayMessage = featureOverlayQuery.buildMapClickMessage(latLng, mapView, map);
            if(overlayMessage != null){
                if(message.length() > 0){
                    message.append("\n\n");
                }
                message.append(overlayMessage);
            }
        }

        return message.length() > 0 ? message.toString() : null;
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

    /**
     * Add a feature overlay query
     *
     * @param featureOverlayQuery
     */
    public void addFeatureOverlayQuery(FeatureOverlayQuery featureOverlayQuery){
        featureOverlayQueries.add(featureOverlayQuery);
    }

    /**
     * Clear the feature overlay queries
     */
    public void clearFeatureOverlayQueries(){
        featureOverlayQueries.clear();
    }

}
