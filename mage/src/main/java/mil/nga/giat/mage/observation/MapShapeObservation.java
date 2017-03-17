package mil.nga.giat.mage.observation;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import mil.nga.geopackage.map.geom.GoogleMapShape;
import mil.nga.geopackage.map.geom.GoogleMapShapeType;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.wkb.geom.GeometryEnvelope;

/**
 * Observation represented by a shape on the map
 *
 * @author osbornb
 */
public class MapShapeObservation extends MapObservation {

    /**
     * Map shape
     */
    private final GoogleMapShape shape;

    /**
     * Geometry envelope
     */
    private final GeometryEnvelope envelope;

    /**
     * Constructor
     *
     * @param observation observation
     * @param shape       shape
     * @param envelope    geometry envelope
     */
    public MapShapeObservation(Observation observation, GoogleMapShape shape, GeometryEnvelope envelope) {
        super(observation);
        this.shape = shape;
        this.envelope = envelope;
    }

    /**
     * Get the shape
     *
     * @return shape
     */
    public GoogleMapShape getShape() {
        return shape;
    }

    /**
     * Get the geometry envelope
     *
     * @return geometry envelope
     */
    public GeometryEnvelope getEnvelope() {
        return envelope;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() {
        shape.remove();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVisible(boolean visible) {
        shape.setVisible(visible);
    }

    /**
     * Determine if the point is on the shape, either on a polygon or within the distance tolerance of a line
     *
     * @param latLng    point
     * @param tolerance line tolerance
     * @return true if point is on shape
     */
    public boolean pointIsOnShape(LatLng latLng, double tolerance) {

        boolean onShape = false;

        GoogleMapShapeType shapeType = shape.getShapeType();
        switch (shapeType) {
            case POLYLINE:
                Polyline polyline = (Polyline) shape.getShape();
                onShape = pointIsOnPolyline(latLng, polyline, tolerance);
                break;
            case POLYGON:
                Polygon polygon = (Polygon) shape.getShape();
                onShape = pointIsOnPolygon(latLng, polygon);
                break;
        }

        return onShape;
    }

    /**
     * Determine if the point is on the polyline
     *
     * @param latLng    point
     * @param polyline  polyline
     * @param tolerance distance tolerance
     * @return true if point is on polyline within tolerance
     */
    private boolean pointIsOnPolyline(LatLng latLng, Polyline polyline, double tolerance) {

        boolean onShape = false;

        double midX = (envelope.getMaxX() + envelope.getMinX()) / 2.0;
        double midY = (envelope.getMaxY() + envelope.getMinY()) / 2.0;

        LatLng leftCoordinate = SphericalUtil.computeOffset(new LatLng(midY, envelope.getMinX()), tolerance, 270);
        LatLng upCoordinate = SphericalUtil.computeOffset(new LatLng(envelope.getMaxY(), midX), tolerance, 0);
        LatLng rightCoordinate = SphericalUtil.computeOffset(new LatLng(midY, envelope.getMaxX()), tolerance, 90);
        LatLng downCoordinate = SphericalUtil.computeOffset(new LatLng(envelope.getMinY(), midX), tolerance, 180);

        if (latLng.longitude >= leftCoordinate.longitude && latLng.longitude <= rightCoordinate.longitude
                && latLng.latitude >= downCoordinate.latitude && latLng.latitude <= upCoordinate.latitude) {

            onShape = PolyUtil.isLocationOnPath(latLng, polyline.getPoints(), true, tolerance);
        }

        return onShape;
    }

    /**
     * Determine if the point is on the polygon
     *
     * @param latLng  point
     * @param polygon polygon
     * @return true if point is on polygon within tolerance
     */
    private boolean pointIsOnPolygon(LatLng latLng, Polygon polygon) {

        boolean onShape = false;

        if (latLng.longitude >= envelope.getMinX() && latLng.longitude <= envelope.getMaxX()
                && latLng.latitude >= envelope.getMinY() && latLng.latitude <= envelope.getMaxY()) {

            onShape = PolyUtil.containsLocation(latLng, polygon.getPoints(), true);
        }

        return onShape;
    }

}
