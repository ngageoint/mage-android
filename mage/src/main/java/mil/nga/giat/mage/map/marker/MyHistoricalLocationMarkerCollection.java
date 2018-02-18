package mil.nga.giat.mage.map.marker;

import android.content.Context;
import android.util.Pair;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.common.collect.MinMaxPriorityQueue;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.sdk.Temporal;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.push.LocationPushIntentService;
import mil.nga.wkb.geom.Geometry;

/**
 * Class uses a queue like structure to limit the Collection size. Size determined
 * by LocationPushIntentService.minNumberOfLocationsToKeep
 *
 * @author wiedemanns
 *
 */
public class MyHistoricalLocationMarkerCollection implements PointCollection<Pair<Location, User>> {

	private Context context;
	private GoogleMap map;
	private boolean visible = false;
	private Map<Long, Marker> locationIdToMarker = new HashMap<>();
	private Map<String, Pair<Location, User>> markerIdToLocation = new HashMap<>();

	// Use a queue like structure to limit the Collection size
	protected MinMaxPriorityQueue<Location> locationQueue = MinMaxPriorityQueue.orderedBy(new Comparator<Location>() {
		@Override
		public int compare(Location lhs, Location rhs) {
			return lhs.getTimestamp().compareTo(rhs.getTimestamp());
		}
	}).expectedSize(LocationPushIntentService.minNumberOfLocationsToKeep).create();

	public MyHistoricalLocationMarkerCollection(Context context, GoogleMap map) {
		this.context = context;
		this.map = map;
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		Pair<Location, User> pair = markerIdToLocation.get(marker.getId());

		if (pair == null) {
			return false;
		}

		marker.showInfoWindow();

		return true;
	}

	@Override
	public void add(MarkerOptions options, Pair<Location, User> pair) {
		Location location = pair.first;
		final Geometry gometry = location.getGeometry();

		if (gometry != null) {
			options.visible(visible);
			Marker marker = map.addMarker(options);
			markerIdToLocation.put(marker.getId(), pair);
			locationIdToMarker.put(location.getId(), marker);
			locationQueue.add(location);

			while (locationQueue.size() > LocationPushIntentService.minNumberOfLocationsToKeep) {
				Location locationToRemove = locationQueue.poll();

				Marker markerToRemove = locationIdToMarker.remove(locationToRemove.getId());
				if (markerToRemove != null) {
					markerIdToLocation.remove(markerToRemove.getId());
				}
			}
		}
	}

	@Override
	public void remove(Pair<Location, User> pair) {
		// no-op rolling window based on number of points, remove happens in add if overflow
	}

	@Override
	public Date getLatestDate() {
		return locationQueue.peekLast().getTimestamp();
	}

	@Override
	public void refresh(Pair<Location, User> point) {

	}

	@Override
	public void refreshMarkerIcons(Filter<Temporal> filter) {
		for (Marker m : locationIdToMarker.values()) {
			Pair<Location, User> pair = markerIdToLocation.get(m.getId());
			Location location = pair.first;
			User user = pair.second;

			if (location != null) {
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
	public int count() {
		return locationQueue.size();
	}

	@Override
	public boolean isVisible() {
		return false;
	}

	@Override
	public void setVisibility(boolean visible) {
		if (this.visible == visible)
			return;

		this.visible = visible;
		for (Marker m : locationIdToMarker.values()) {
			m.setVisible(visible);
		}
	}

	@Override
	public void clear() {
		for (Marker marker : locationIdToMarker.values()) {
			marker.remove();
		}

		locationQueue.clear();
		locationIdToMarker.clear();
		markerIdToLocation.clear();
	}

	@Override
	public void offMarkerClick() {
		// no-op, nothing extra shown on marker click that we need to hide
	}

	@Override
	public void onCameraIdle() {

	}

	@Override
	public Pair<Location, User> pointForMarker(Marker marker) {
		return null;
	}

	@Override
	public void onMapClick(LatLng latLng) {

	}
}
