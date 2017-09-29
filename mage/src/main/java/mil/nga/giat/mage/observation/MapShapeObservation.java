package mil.nga.giat.mage.observation;

import com.google.android.gms.maps.model.LatLng;

import mil.nga.geopackage.map.geom.GoogleMapShape;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;

/**
 * Observation represented by a shape on the map
 *
 * @author osbornb
 */
public abstract class MapShapeObservation extends MapObservation {

    /**
     * Flag indicating whether to use geodesic observation shapes
     */
    public static final boolean GEODESIC = false;

    /**
     * Map shape
     */
    protected final GoogleMapShape shape;

    /**
     * Create a map shape observation
     *
     * @param observation observation
     * @param shape       map shape
     * @return map shape observation
     */
    public static MapShapeObservation create(Observation observation, GoogleMapShape shape) {
        MapShapeObservation observationShape = null;
        switch (shape.getShapeType()) {
            case POLYLINE:
                observationShape = new MapPolylineObservation(observation, shape);
                break;
            case POLYGON:
                observationShape = new MapPolygonObservation(observation, shape);
                break;
            default:
                throw new IllegalArgumentException("Illegal shape type: " + shape.getShapeType());
        }
        return observationShape;
    }

    /**
     * Constructor
     *
     * @param observation observation
     * @param shape       shape
     */
    protected MapShapeObservation(Observation observation, GoogleMapShape shape) {
        super(observation);
        this.shape = shape;
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
    public abstract boolean pointIsOnShape(LatLng latLng, double tolerance);

}
