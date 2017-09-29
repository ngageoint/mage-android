package mil.nga.giat.mage.observation;

import com.google.android.gms.maps.model.Marker;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;

/**
 * Observation represented by a single Marker on the map
 *
 * @author osbornb
 */
public class MapMarkerObservation extends MapObservation {

    /**
     * Observation marker
     */
    private final Marker marker;

    /**
     * Constructor
     *
     * @param observation observation
     * @param marker      marker
     */
    public MapMarkerObservation(Observation observation, Marker marker) {
        super(observation);
        this.marker = marker;
    }

    /**
     * Get the marker
     *
     * @return marker
     */
    public Marker getMarker() {
        return marker;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() {
        marker.remove();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVisible(boolean visible) {
        marker.setVisible(visible);
    }

}
