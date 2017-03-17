package mil.nga.giat.mage.observation;

import mil.nga.geopackage.map.geom.GoogleMapShape;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;

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
     * Constructor
     *
     * @param observation observation
     * @param shape       shape
     */
    public MapShapeObservation(Observation observation, GoogleMapShape shape) {
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

}
