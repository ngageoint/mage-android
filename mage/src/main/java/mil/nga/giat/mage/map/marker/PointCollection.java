package mil.nga.giat.mage.map.marker;

import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Date;
import java.util.Iterator;

import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.sdk.Temporal;

public interface PointCollection<T> extends OnMarkerClickListener, OnInfoWindowClickListener {
	public void add(MarkerOptions options, T point);

	public void remove(T point);

	public void refreshMarkerIcons(Filter<Temporal> filter);

	public int count();

	public Iterator<T> iterator();

	public boolean isVisible();

	public void setVisibility(boolean visible);

	public Date getLatestDate();

	public void clear();

}