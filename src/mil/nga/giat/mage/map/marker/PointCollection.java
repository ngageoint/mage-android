package mil.nga.giat.mage.map.marker;

import java.util.Collection;
import java.util.Date;

import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;

public interface PointCollection<T> extends OnCameraChangeListener, OnMarkerClickListener {
    public void add(T point);
    public void addAll(Collection<T> points);
    public void remove(T point);
        
    public void setVisibility(boolean visible);
    
    public Date getLatestDate();
    
    public void clear();
    
}