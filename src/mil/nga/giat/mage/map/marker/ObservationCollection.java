package mil.nga.giat.mage.map.marker;

import java.util.Collection;
import java.util.Date;

import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;

import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;

public interface ObservationCollection extends OnCameraChangeListener, OnMarkerClickListener {
    public void add(Observation observation);
    
    public void addAll(Collection<Observation> observations);
    
    public Collection<Observation> getObservations();

    public void remove(Observation observation);
    
    public void setVisible(boolean visible);
    public void setObservationVisibility(Observation observation, boolean visible);
    
    public Date getLatestObservationDate();
    
    public void clear();
    
    public void setFilters(Collection<Filter<Observation>> filters);
}