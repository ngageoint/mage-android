package mil.nga.giat.mage.map.marker;

import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.location.LocationGeometry;
import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.vividsolutions.jts.geom.Point;

// FIXME : use a queue like structure to limit the Collection size
public class MyHistoricalLocationMarkerCollection extends LocationMarkerCollection {

	private static final String ASSET = "dots/maps_dav_bw_dot.png";
	private static final String DEFAULT_ASSET = "dots/maps_dav_blue_dot.png";
	
	public MyHistoricalLocationMarkerCollection(Context context, GoogleMap map) {
		super(context, map);
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		return true;
	}

	@Override
	public void add(Location l) {
		final LocationGeometry lg = l.getLocationGeometry();
		if(lg != null) {
			// If I got an observation that I already have in my list
			// remove it from the map and clean-up my collections
			Marker marker = locationIdToMarker.remove(l.getId());
			if (marker != null) {
				markerIdToLocation.remove(marker.getId());
				marker.remove();
			}
	
			removeOldMarkers();
			
			Point point = lg.getGeometry().getCentroid();
			MarkerOptions options = new MarkerOptions().position(new LatLng(point.getY(), point.getX())).icon(LocationBitmapFactory.bitmapDescriptor(context, l, ASSET, DEFAULT_ASSET)).visible(visible);
	
			marker = markerCollection.addMarker(options);
	
			locationIdToMarker.put(l.getId(), marker);
			markerIdToLocation.put(marker.getId(), l);
	
			if (l.getTimestamp().after(latestLocationDate)) {
				latestLocationDate = l.getTimestamp();
			}
		}
	}
	
	@Override
	public void refreshMarkerIcons() {
		for (Marker m : markerCollection.getMarkers()) {
			Location tl = markerIdToLocation.get(m.getId());
			if (tl != null) {
				boolean showWindow = m.isInfoWindowShown();
				m.setIcon(LocationBitmapFactory.bitmapDescriptor(context, tl, ASSET, DEFAULT_ASSET));
				if(showWindow) {
					m.showInfoWindow();
				}
			}
		}
	}

}
