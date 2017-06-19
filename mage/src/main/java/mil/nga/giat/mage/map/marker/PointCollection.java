package mil.nga.giat.mage.map.marker;

import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Date;

public interface PointCollection<T> extends OnCameraIdleListener, OnMarkerClickListener, OnMapClickListener, OnInfoWindowClickListener {
	public void add(MarkerOptions options, T point);

	public void remove(T point);

	public void refreshMarkerIcons();

	public boolean isVisible();

	public void setVisibility(boolean visible);

	public Date getLatestDate();

	public void clear();

	public void offMarkerClick();

}
