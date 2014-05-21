package mil.nga.giat.mage.map.marker;

import java.util.Comparator;
import java.util.Date;
import java.util.Set;

import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.location.LocationGeometry;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.push.LocationPushIntentService;
import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.common.collect.MinMaxPriorityQueue;
import com.vividsolutions.jts.geom.Point;

/**
 * Class uses a queue like structure to limit the Collection size. Size determined 
 * by LocationPushIntentService.minNumberOfLocationsToKeep
 * 
 * @author wiedemanns
 * 
 */
public class MyHistoricalLocationMarkerCollection extends LocationMarkerCollection {

	private static final String ASSET = "dots/maps_dav_bw_dot.png";
	private static final String DEFAULT_ASSET = "dots/maps_dav_blue_dot.png";

	// use a queue like structure to limit the Collection size
	protected MinMaxPriorityQueue<Location> locationQueue = MinMaxPriorityQueue.orderedBy(new Comparator<Location>() {
		@Override
		public int compare(Location lhs, Location rhs) {
			return lhs.getTimestamp().compareTo(rhs.getTimestamp());
		}
	}).expectedSize(LocationPushIntentService.minNumberOfLocationsToKeep).create();

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
		if (lg != null) {
			// If I got an observation that I already have in my list
			// remove it from the map and clean-up my collections
			Marker marker = locationIdToMarker.remove(l.getId());
			if (marker != null) {
				markerIdToLocation.remove(marker.getId());
				marker.remove();
			}

			Point point = lg.getGeometry().getCentroid();
			MarkerOptions options = new MarkerOptions().position(new LatLng(point.getY(), point.getX())).icon(LocationBitmapFactory.bitmapDescriptor(context, l, ASSET, DEFAULT_ASSET)).visible(visible);

			marker = markerCollection.addMarker(options);

			locationIdToMarker.put(l.getId(), marker);
			markerIdToLocation.put(marker.getId(), l);
			locationQueue.add(l);

			while (locationQueue.size() > LocationPushIntentService.minNumberOfLocationsToKeep) {
				remove(locationQueue.poll());
			}
			
			removeOldMarkers();
		}
	}
	
	@Override
	public Date getLatestDate() {
		return locationQueue.peekLast().getTimestamp();
	}

	@Override
	public void refreshMarkerIcons() {
		for (Marker m : markerCollection.getMarkers()) {
			Location tl = markerIdToLocation.get(m.getId());
			if (tl != null) {
				boolean showWindow = m.isInfoWindowShown();
				m.setIcon(LocationBitmapFactory.bitmapDescriptor(context, tl, ASSET, DEFAULT_ASSET));
				if (showWindow) {
					m.showInfoWindow();
				}
			}
		}
	}

	/**
	 * Used to remove markers for locations that have been removed from the local datastore.
	 */
	@Override
	public void removeOldMarkers() {
		LocationHelper lh = LocationHelper.getInstance(context.getApplicationContext());
		Set<Long> locationIds = locationIdToMarker.keySet();
		for (Long locationId : locationIds) {
			Location locationExists = new Location();
			locationExists.setId(locationId);
			if (!lh.exists(locationExists)) {
				Marker marker = locationIdToMarker.remove(locationId);
				if (marker != null) {
					Location l = markerIdToLocation.remove(marker.getId());
					locationQueue.remove(l);
					marker.remove();
				}
			}
		}
	}

	@Override
	public void clear() {
		super.clear();
		locationQueue.clear();
	}
}
