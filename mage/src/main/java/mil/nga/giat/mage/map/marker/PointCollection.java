package mil.nga.giat.mage.map.marker;

import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;

import java.util.Collection;
import java.util.Date;

public interface PointCollection<T> extends OnCameraChangeListener, OnMarkerClickListener, OnInfoWindowClickListener {
	public void add(T point);

	public void addAll(Collection<T> points);

	public void remove(T point);

	public void refreshMarkerIcons();

	public boolean isVisible();

	public void setVisibility(boolean visible);

	public Date getLatestDate();

	public void clear();

}