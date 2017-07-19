package mil.nga.giat.mage.map.marker;

import android.content.Context;
import android.util.Pair;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.common.collect.MinMaxPriorityQueue;
import com.vividsolutions.jts.geom.Geometry;

import java.util.Comparator;
import java.util.Date;

import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.sdk.Temporal;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.push.LocationPushIntentService;

/**
 * Class uses a queue like structure to limit the Collection size. Size determined 
 * by LocationPushIntentService.minNumberOfLocationsToKeep
 * 
 * @author wiedemanns
 * 
 */
public class MyHistoricalLocationMarkerCollection extends LocationMarkerCollection {

	// use a queue like structure to limit the Collection size
	protected MinMaxPriorityQueue<Pair<Location, User>> locationQueue = MinMaxPriorityQueue.orderedBy(new Comparator<Pair<Location, User>>() {
		@Override
		public int compare(Pair<Location, User> lhs, Pair<Location, User> rhs) {
			return lhs.first.getTimestamp().compareTo(rhs.first.getTimestamp());
		}
	}).expectedSize(LocationPushIntentService.minNumberOfLocationsToKeep).create();

	public MyHistoricalLocationMarkerCollection(Context context, GoogleMap map) {
		super(context, map);
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		Location l = markerIdToPair.get(marker.getId()).first;
        return (l != null);
	}

	@Override
	public void add(MarkerOptions options, Pair<Location, User> pair) {
		Location location = pair.first;
		User user = pair.second;

		final Geometry g = location.getGeometry();
		if (g != null) {
			// If I got an observation that I already have in my list
			// remove it from the map and clean-up my collections
			Marker marker = userIdToMarker.remove(user.getId());
			if (marker != null) {
				markerIdToPair.remove(marker.getId());
				marker.remove();
			}

			marker = map.addMarker(options);

			userIdToMarker.put(user.getId(), marker);
			markerIdToPair.put(marker.getId(), pair);
			locationQueue.add(pair);

			while (locationQueue.size() > LocationPushIntentService.minNumberOfLocationsToKeep) {
				remove(locationQueue.poll());
			}
		}
	}
	
	@Override
	public Date getLatestDate() {
		return locationQueue.peekLast().first.getTimestamp();
	}

	@Override
	public void refreshMarkerIcons(Filter<Temporal> filter) {
		for (Marker m : userIdToMarker.values()) {
			Pair<Location, User> pair = markerIdToPair.get(m.getId());
			Location location = pair.first;
			User user = pair.second;

			if (user != null && location != null) {
				boolean showWindow = m.isInfoWindowShown();
				// make sure to set the Anchor after this call as well, because the size of the icon might have changed
				m.setIcon(LocationBitmapFactory.dotBitmapDescriptor(context, location, user));
				m.setAnchor(0.5f, 1.0f);
				if (showWindow) {
					m.showInfoWindow();
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
