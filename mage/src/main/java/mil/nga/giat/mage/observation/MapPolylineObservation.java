package mil.nga.giat.mage.observation;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.maps.android.PolyUtil;

import mil.nga.geopackage.map.geom.GoogleMapShape;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;

/**
 * Observation represented by a polyline on the map
 *
 * @author osbornb
 */
public class MapPolylineObservation extends MapShapeObservation {

    /**
     * Polyline
     */
    private final Polyline polyline;

    /**
     * Constructor
     *
     * @param observation observation
     * @param shape       shape
     * @param envelope    geometry envelope
     */
    public MapPolylineObservation(Observation observation, GoogleMapShape shape) {
        super(observation, shape);
        polyline = (Polyline) shape.getShape();
    }

    /**
     * {@inheritDoc}
     *
     * @Override
     */
    public boolean pointIsOnShape(LatLng latLng, double tolerance) {

        boolean onShape = PolyUtil.isLocationOnPath(latLng, polyline.getPoints(), polyline.isGeodesic(), tolerance);

        return onShape;
    }

}
