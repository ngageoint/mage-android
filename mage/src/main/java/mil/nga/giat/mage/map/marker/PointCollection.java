package mil.nga.giat.mage.map.marker;

import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.Marker;

import java.util.Date;

public interface PointCollection<T> extends OnCameraIdleListener, OnMarkerClickListener, OnMapClickListener {
	public void add(T point);

	public void remove(T point);

	public void refresh(T point);

	public void refreshMarkerIcons();

	public int count();

	public boolean isVisible();

	public void setVisibility(boolean visible);

	public Date getLatestDate();

	public void clear();

	public T pointForMarker(Marker marker);
	public void offMarkerClick();

}
