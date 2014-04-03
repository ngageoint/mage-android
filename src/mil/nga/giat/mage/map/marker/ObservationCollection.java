package mil.nga.giat.mage.map.marker;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;


public interface ObservationCollection {
    public void add(Observation o);

    public void remove(Observation o);

}