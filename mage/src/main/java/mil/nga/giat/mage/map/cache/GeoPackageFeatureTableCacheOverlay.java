package mil.nga.giat.mage.map.cache;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.TileOverlay;

import java.util.HashMap;
import java.util.Map;

import mil.nga.geopackage.geom.map.GoogleMapShape;
import mil.nga.geopackage.geom.map.GoogleMapShapeConverter;
import mil.nga.giat.mage.R;

/**
 * GeoPackage Feature Table cache overlay
 */
public class GeoPackageFeatureTableCacheOverlay extends GeoPackageTableCacheOverlay {

    /**
     * Mapping between feature ids and shapes
     */
    private Map<Long, GoogleMapShape> shapes = new HashMap<>();

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
    public GeoPackageFeatureTableCacheOverlay(String name, String geoPackage, String cacheName) {
        super(name, geoPackage, cacheName, CacheOverlayType.GEOPACKAGE_FEATURE_TABLE);
    }

    @Override
    public void removeFromMap() {
        for (GoogleMapShape shape : shapes.values()) {
            shape.remove();
        }
        shapes.clear();
        if (tileOverlay != null) {
            tileOverlay.remove();
            tileOverlay = null;
        }
    }

    @Override
    public Integer getIconImageResourceId() {
        return R.drawable.ic_place;
    }

    /**
     * Add a shape
     *
     * @param id
     * @param shape
     */
    public void addShape(long id, GoogleMapShape shape) {
        shapes.put(id, shape);
    }

    /**
     * Remove a shape
     *
     * @param id
     * @return
     */
    public GoogleMapShape removeShape(long id) {
        return shapes.remove(id);
    }

    /**
     * Add a shape to the map
     *
     * @param id
     * @param shape
     * @return added map shape
     */
    public GoogleMapShape addShapeToMap(long id, GoogleMapShape shape, GoogleMap map) {
        GoogleMapShape mapShape = GoogleMapShapeConverter.addShapeToMap(map, shape);
        addShape(id, mapShape);
        return mapShape;
    }

    /**
     * Remove a shape from the map
     *
     * @param id
     * @return
     */
    public GoogleMapShape removeShapeFromMap(long id) {
        GoogleMapShape shape = removeShape(id);
        if (shape != null) {
            shape.remove();
        }
        return shape;
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
