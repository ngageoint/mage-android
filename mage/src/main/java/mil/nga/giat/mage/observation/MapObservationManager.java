package mil.nga.giat.mage.observation;

import android.content.Context;
import android.graphics.Bitmap;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

import mil.nga.geopackage.map.geom.GoogleMapShape;
import mil.nga.geopackage.map.geom.GoogleMapShapeConverter;
import mil.nga.geopackage.map.geom.GoogleMapShapeType;
import mil.nga.geopackage.map.geom.MultiLatLng;
import mil.nga.geopackage.map.geom.MultiPolygonOptions;
import mil.nga.geopackage.map.geom.MultiPolylineOptions;
import mil.nga.giat.mage.map.marker.ObservationBitmapFactory;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.wkb.geom.Geometry;
import mil.nga.wkb.geom.GeometryType;
import mil.nga.wkb.geom.Point;
import mil.nga.wkb.util.GeometryUtils;

/**
 * Handles adding Observations to the map as markers or shapes
 *
 * @author osbornb
 */
public class MapObservationManager {

    /**
     * App context
     */
    private final Context context;

    /**
     * Google map
     */
    private final GoogleMap map;

    /**
     * Constructor
     *
     * @param context context
     * @param map     map
     */
    public MapObservationManager(Context context, GoogleMap map) {
        this.context = context;
        this.map = map;
    }

    /**
     * Add an observation to the map as a marker or shape
     *
     * @param observation observation
     * @return map observation
     */
    public MapObservation addToMap(Observation observation) {
        return addToMap(observation, true);
    }

    /**
     * Add an observation to the map as a marker or shape
     *
     * @param observation observation
     * @param visible     visible state
     * @return map observation
     */
    public MapObservation addToMap(Observation observation, boolean visible) {

        MapObservation observationShape = null;

        Geometry geometry = observation.getGeometry();

        if (geometry.getGeometryType() == GeometryType.POINT) {
            Point point = GeometryUtils.getCentroid(geometry);
            MarkerOptions markerOptions = getMarkerOptions(observation, visible);
            markerOptions.position(new LatLng(point.getY(), point.getX()));
            Marker marker = map.addMarker(markerOptions);

            observationShape = new MapMarkerObservation(observation, marker);
        } else {

            GoogleMapShapeConverter shapeConverter = new GoogleMapShapeConverter();
            GoogleMapShape shape = shapeConverter.toShape(geometry);
            prepareShapeOptions(observation, shape, visible);
            GoogleMapShape mapShape = GoogleMapShapeConverter.addShapeToMap(map, shape);

            observationShape = MapShapeObservation.create(observation, mapShape);
        }

        return observationShape;
    }

    /**
     * Add a shape marker to the map at the location.  A shape marker is a transparent icon for allowing shape info windows.
     *
     * @param latLng  lat lng location
     * @param visible visible state
     * @return shape marker
     */
    public Marker addShapeMarker(LatLng latLng, boolean visible) {

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)));
        markerOptions.visible(visible);
        markerOptions.anchor(0.5f, 0.5f);
        markerOptions.position(latLng);
        Marker marker = map.addMarker(markerOptions);

        return marker;
    }

    /**
     * Prepare the marker options for an observation point
     *
     * @param observation observation
     * @param visible     visible flag
     * @return marker options
     */
    private MarkerOptions getMarkerOptions(Observation observation, boolean visible) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.icon(ObservationBitmapFactory.bitmapDescriptor(context, observation));
        markerOptions.visible(visible);
        return markerOptions;
    }

    /**
     * Prepare the shape options for an observation shape
     *
     * @param observation observation
     * @param shape       shape
     * @param visible     visible flag
     */
    private void prepareShapeOptions(Observation observation, GoogleMapShape shape, boolean visible) {

        ObservationShapeStyle style = MapShapeObservation.style(context, observation);

        GoogleMapShapeType shapeType = shape.getShapeType();
        switch (shapeType) {

            case LAT_LNG:
                LatLng latLng = (LatLng) shape.getShape();
                MarkerOptions markerOptions = getMarkerOptions(observation, visible);
                markerOptions.position(latLng);
                shape.setShape(markerOptions);
                shape.setShapeType(GoogleMapShapeType.MARKER_OPTIONS);
                break;

            case POLYLINE_OPTIONS:
                PolylineOptions polylineOptions = (PolylineOptions) shape
                        .getShape();
                setPolylineOptions(style, polylineOptions, visible);
                break;

            case POLYGON_OPTIONS:
                PolygonOptions polygonOptions = (PolygonOptions) shape.getShape();
                setPolygonOptions(style, polygonOptions, visible);
                break;

            case MULTI_LAT_LNG:
                MultiLatLng multiLatLng = (MultiLatLng) shape.getShape();
                MarkerOptions sharedMarkerOptions = getMarkerOptions(observation, visible);
                multiLatLng.setMarkerOptions(sharedMarkerOptions);
                break;

            case MULTI_POLYLINE_OPTIONS:
                MultiPolylineOptions multiPolylineOptions = (MultiPolylineOptions) shape
                        .getShape();
                PolylineOptions sharedPolylineOptions = new PolylineOptions();
                setPolylineOptions(style, sharedPolylineOptions, visible);
                multiPolylineOptions.setOptions(sharedPolylineOptions);
                break;

            case MULTI_POLYGON_OPTIONS:
                MultiPolygonOptions multiPolygonOptions = (MultiPolygonOptions) shape
                        .getShape();
                PolygonOptions sharedPolygonOptions = new PolygonOptions();
                setPolygonOptions(style, sharedPolygonOptions, visible);
                multiPolygonOptions.setOptions(sharedPolygonOptions);
                break;

            case COLLECTION:
                @SuppressWarnings("unchecked")
                List<GoogleMapShape> shapes = (List<GoogleMapShape>) shape
                        .getShape();
                for (int i = 0; i < shapes.size(); i++) {
                    prepareShapeOptions(observation, shapes.get(i), visible);
                }
                break;
            default:
        }
    }

    /**
     * Set the polyline options
     *
     * @param style           shape style
     * @param polylineOptions polyline options
     * @param visible         visible flag
     */
    private void setPolylineOptions(ObservationShapeStyle style, PolylineOptions polylineOptions, boolean visible) {
        polylineOptions.width(style.getStrokeWidth());
        polylineOptions.color(style.getStrokeColor());
        polylineOptions.visible(visible);
        polylineOptions.geodesic(MapShapeObservation.GEODESIC);
    }

    /**
     * Set the polygon options
     *
     * @param style          shape style
     * @param polygonOptions polygon options
     * @param visible        visible flag
     */
    private void setPolygonOptions(ObservationShapeStyle style, PolygonOptions polygonOptions, boolean visible) {
        polygonOptions.strokeWidth(style.getStrokeWidth());
        polygonOptions.strokeColor(style.getStrokeColor());
        polygonOptions.fillColor(style.getFillColor());
        polygonOptions.visible(visible);
        polygonOptions.geodesic(MapShapeObservation.GEODESIC);
    }

}
