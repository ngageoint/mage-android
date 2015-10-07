package mil.nga.giat.mage.map.cache;

import android.util.TypedValue;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;

import java.util.HashMap;
import java.util.Map;

import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.features.index.FeatureIndexManager;
import mil.nga.geopackage.features.index.FeatureIndexResults;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.geom.map.GoogleMapShape;
import mil.nga.geopackage.geom.map.GoogleMapShapeConverter;
import mil.nga.geopackage.projection.ProjectionConstants;
import mil.nga.geopackage.projection.ProjectionFactory;
import mil.nga.geopackage.tiles.TileBoundingBoxUtils;
import mil.nga.geopackage.tiles.TileGrid;
import mil.nga.geopackage.tiles.features.FeatureTiles;
import mil.nga.giat.mage.R;
import mil.nga.wkb.geom.GeometryType;
import mil.nga.wkb.geom.Point;
import mil.nga.wkb.util.GeometryPrinter;

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
     * Feature Tiles
     */
    private FeatureTiles featureTiles;

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

        boolean maxFeaturesEnabled = mapView.getResources().getBoolean(R.bool.geopackage_feature_tiles_map_click_max_features);
        boolean featuresEnabled = mapView.getResources().getBoolean(R.bool.geopackage_feature_tiles_map_click_features);

        if ((maxFeaturesEnabled || featuresEnabled) && tileOverlay != null && featureTiles != null) {
            float zoom = map.getCameraPosition().zoom;
            if (zoom >= getMinZoom() && featureTiles.getMaxFeaturesPerTile() != null) {
                int zoomValue = (int) zoom;
                Point point = new Point(latLng.longitude, latLng.latitude);
                TileGrid tileGrid = TileBoundingBoxUtils.getTileGridFromWGS84(point, zoomValue);
                long tileFeaturesCount = featureTiles.queryIndexedFeaturesCount((int) tileGrid.getMinX(), (int) tileGrid.getMinY(), zoomValue);
                if (tileFeaturesCount > featureTiles.getMaxFeaturesPerTile().intValue()) {
                    if (maxFeaturesEnabled) {
                        message = getName() + " - " + tileFeaturesCount + " features";
                    }
                } else if (featuresEnabled) {
                    message = handleFeatureClick(latLng, mapView, map);
                }
            }
        }

        return message;
    }

    /**
     * Handle feature clicks on nearby features
     *
     * @param latLng  click location
     * @param mapView map view
     * @param map     Google map
     * @return string message or null
     */
    private String handleFeatureClick(LatLng latLng, MapView mapView, GoogleMap map) {
        String message = null;

        // Get the screen percentage to determine when a feature is clicked
        TypedValue screenPercentage = new TypedValue();
        mapView.getResources().getValue(R.dimen.geopackage_feature_tiles_map_click_screen_percentage, screenPercentage, true);
        float screenClickPercentage = screenPercentage.getFloat();

        // Get the screen width and height a click occurs from a feature
        int width = (int) Math.round(mapView.getWidth() * screenClickPercentage);
        int height = (int) Math.round(mapView.getHeight() * screenClickPercentage);

        // Get the screen click location
        Projection projection = map.getProjection();
        android.graphics.Point clickLocation = projection.toScreenLocation(latLng);

        // Get the screen click locations in each width or height direction
        android.graphics.Point left = new android.graphics.Point(clickLocation);
        android.graphics.Point up = new android.graphics.Point(clickLocation);
        android.graphics.Point right = new android.graphics.Point(clickLocation);
        android.graphics.Point down = new android.graphics.Point(clickLocation);
        left.offset(-width, 0);
        up.offset(0, -height);
        right.offset(width, 0);
        down.offset(0, height);

        // Get the coordinates of the bounding box points
        LatLng leftCoordinate = projection.fromScreenLocation(left);
        LatLng upCoordinate = projection.fromScreenLocation(up);
        LatLng rightCoordinate = projection.fromScreenLocation(right);
        LatLng downCoordinate = projection.fromScreenLocation(down);

        // Create the bounding box to query for features
        BoundingBox boundingBox = new BoundingBox(
                leftCoordinate.longitude,
                rightCoordinate.longitude,
                downCoordinate.latitude,
                upCoordinate.latitude);

        // Query features
        message = queryFeatures(boundingBox, mapView);

        return message;
    }

    /**
     * Query for features in the bounding box
     *
     * @param boundingBox
     * @param mapView
     * @return string message or null
     */
    private String queryFeatures(BoundingBox boundingBox, MapView mapView) {

        String message = null;

        // Query for features
        FeatureIndexManager indexManager = featureTiles.getIndexManager();
        FeatureIndexResults results = indexManager.query(boundingBox, ProjectionFactory.getProjection(ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM));
        try {
            long featureCount = results.count();
            if (featureCount > 0) {

                int maxFeatureInfo = 0;
                if (geometryType == GeometryType.POINT) {
                    maxFeatureInfo = mapView.getResources().getInteger(R.integer.geopackage_feature_tiles_map_click_max_point_info);
                } else {
                    maxFeatureInfo = mapView.getResources().getInteger(R.integer.geopackage_feature_tiles_map_click_max_feature_info);
                }

                if (featureCount <= maxFeatureInfo) {
                    StringBuilder messageBuilder = new StringBuilder();
                    messageBuilder.append(getName())
                            .append("\n");

                    int featureNumber = 0;

                    boolean printFeatures = false;
                    if (geometryType == GeometryType.POINT) {
                        printFeatures = mapView.getResources().getBoolean(R.bool.geopackage_feature_tiles_map_click_print_points);
                    } else {
                        printFeatures = mapView.getResources().getBoolean(R.bool.geopackage_feature_tiles_map_click_print_features);
                    }

                    for (FeatureRow featureRow : results) {

                        featureNumber++;
                        if (featureNumber > maxFeatureInfo) {
                            break;
                        }

                        if (featureCount > 1) {
                            if (featureNumber > 1) {
                                messageBuilder.append("\n");
                            } else {
                                messageBuilder.append("\n")
                                        .append(featureCount)
                                        .append(" Features")
                                        .append("\n");
                            }
                            messageBuilder.append("\n")
                                    .append("Feature ")
                                    .append(featureNumber)
                                    .append(":")
                                    .append("\n");
                        }

                        GeoPackageGeometryData geomData = featureRow.getGeometry();
                        int geometryColumn = featureRow.getGeometryColumnIndex();
                        for (int i = 0; i < featureRow.columnCount(); i++) {
                            if (i != geometryColumn) {
                                Object value = featureRow.getValue(i);
                                if (value != null) {
                                    messageBuilder.append("\n")
                                            .append(featureRow.getColumnName(i))
                                            .append(": ")
                                            .append(value);
                                }
                            }
                        }

                        if (printFeatures) {
                            messageBuilder.append("\n\n");
                            messageBuilder.append(GeometryPrinter.getGeometryString(geomData
                                    .getGeometry()));
                        }

                    }

                    message = messageBuilder.toString();
                } else {
                    message = getName() + " - " + featureCount + " features";
                }
            }
        } finally {
            results.close();
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
     * Get the feature tiles
     *
     * @return feature tiles or null
     */
    public FeatureTiles getFeatureTiles() {
        return featureTiles;
    }

    /**
     * Set the tile overlay
     *
     * @param tileOverlay
     * @param featureTiles
     */
    public void setTileOverlay(TileOverlay tileOverlay, FeatureTiles featureTiles) {
        this.tileOverlay = tileOverlay;
        this.featureTiles = featureTiles;
    }

}
