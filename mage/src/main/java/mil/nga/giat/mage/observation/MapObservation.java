package mil.nga.giat.mage.observation;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;

/**
 * Observation that has been added to the map
 *
 * @author osbornb
 */
public abstract class MapObservation {

    /**
     * Observation
     */
    private final Observation observation;

    /**
     * Constructor
     *
     * @param observation observation
     */
    protected MapObservation(Observation observation) {
        this.observation = observation;
    }

    /**
     * Get the observation
     *
     * @return observation
     */
    public Observation getObservation() {
        return observation;
    }

    /**
     * Remove the observation from the map
     */
    public abstract void remove();

    /**
     * Set the observation visibility on the map
     *
     * @param visible visible flag
     */
    public abstract void setVisible(boolean visible);

}
