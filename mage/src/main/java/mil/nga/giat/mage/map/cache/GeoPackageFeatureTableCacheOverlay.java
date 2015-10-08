package mil.nga.giat.mage.map.cache;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;

import java.util.HashMap;
import java.util.Map;

import mil.nga.geopackage.geom.map.GoogleMapShape;
import mil.nga.geopackage.geom.map.GoogleMapShapeConverter;
import mil.nga.geopackage.tiles.overlay.FeatureOverlayQuery;
import mil.nga.giat.mage.R;
import mil.nga.wkb.geom.GeometryType;

/**
 * GeoPackage Feature Table cache overlay
 *
 * @author osbornb
 */
public class GeoPackageFeatureTableCacheOverlay extends GeoPackageTableCacheOverlay {

    /**
     * Max zoom for features
     */
    public static final int MAX_ZOOM = 21;

    /**
     * Indexed flag, true when the feature table is indexed
     */
    private final boolean indexed;

    /**
     * Geometry type
     */
    private final GeometryType geometryType;

    /**
     * Mapping between feature ids and shapes
     */
    private Map<Long, GoogleMapShape> shapes = new HashMap<>();

    /**
     * Tile Overlay
     */
    private TileOverlay tileOverlay;

    /**
     * Feature Overlay Query
     */
    private FeatureOverlayQuery featureOverlayQuery;

    /**
     * Constructor
     *
     * @param name         GeoPackage table name
     * @param geoPackage   GeoPackage name
     * @param cacheName    Cache name
     * @param count        count
     * @param minZoom      min zoom level
     * @param indexed      indexed flag
     * @param geometryType geometry type
     */
    public GeoPackageFeatureTableCacheOverlay(String name, String geoPackage, String cacheName, int count, int minZoom, boolean indexed, GeometryType geometryType) {
        super(name, geoPackage, cacheName, CacheOverlayType.GEOPACKAGE_FEATURE_TABLE, count, minZoom, MAX_ZOOM);
        this.indexed = indexed;
        this.geometryType = geometryType;
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

    @Override
    public String getInfo() {
        return "features: " + getCount() + ", zoom: " + getMinZoom() + " - " + getMaxZoom();
    }

    @Override
    public String onMapClick(LatLng latLng, MapView mapView, GoogleMap map) {
        String message = null;

        if (featureOverlayQuery != null) {
            message = featureOverlayQuery.buildMapClickMessage(latLng, mapView, map);
        }

        return message;
    }

    /**
     * Determine if the feature table is indexed
     *
     * @return true if indexed
     */
    public boolean isIndexed() {
        return indexed;
    }

    /**
     * Get the geometry type
     *
     * @return geometry type
     */
    public GeometryType getGeometryType() {
        return geometryType;
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
     * @return tile overlay or null
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
     * Get the feature overlay query
     *
     * @return feature overlay query
     */
    public FeatureOverlayQuery getFeatureOverlayQuery() {
        return featureOverlayQuery;
    }

    /**
     * Set the feature overlay query
     *
     * @param featureOverlayQuery
     */
    public void setFeatureOverlayQuery(FeatureOverlayQuery featureOverlayQuery) {
        this.featureOverlayQuery = featureOverlayQuery;
    }

}
