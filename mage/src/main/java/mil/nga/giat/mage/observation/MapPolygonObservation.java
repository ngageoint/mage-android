package mil.nga.giat.mage.observation;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.maps.android.PolyUtil;

import mil.nga.geopackage.map.geom.GoogleMapShape;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;

/**
 * Observation represented by a polygon on the map
 *
 * @author osbornb
 */
public class MapPolygonObservation extends MapShapeObservation {

    /**
     * Polygon
     */
    private final Polygon polygon;

    /**
     * Constructor
     *
     * @param observation observation
     * @param shape       shape
     * @param envelope    geometry envelope
     */
    public MapPolygonObservation(Observation observation, GoogleMapShape shape) {
        super(observation, shape);
        polygon = (Polygon) shape.getShape();
    }

    /**
     * {@inheritDoc}
     *
     * @Override
     */
    public boolean pointIsOnShape(LatLng latLng, double tolerance) {

        boolean onShape = PolyUtil.containsLocation(latLng, polygon.getPoints(), polygon.isGeodesic());

        return onShape;
    }

}
