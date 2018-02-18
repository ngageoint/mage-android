package mil.nga.giat.mage.map.marker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.sdk.Temporal;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.location.LocationProperty;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserLocal;
import mil.nga.giat.mage.sdk.fetch.DownloadImageTask;
import mil.nga.wkb.geom.Geometry;
import mil.nga.wkb.geom.Point;
import mil.nga.wkb.util.GeometryUtils;

public class LocationMarkerCollection implements PointCollection<Pair<Location, User>>, OnMarkerClickListener {

	private static final String LOG_NAME = LocationMarkerCollection.class.getName();

	protected GoogleMap map;
	protected Context context;
	protected Date latestLocationDate = new Date(0);

	protected Long clickedAccuracyCircleUserId;
	protected Circle clickedAccuracyCircle;
	protected InfoWindowAdapter infoWindowAdapter = new LocationInfoWindowAdapter();

	protected boolean visible = true;

	protected Map<Long, Marker> userIdToMarker = new HashMap<>();
	protected Map<String, Pair<Location, User>> markerIdToPair = new HashMap<>();

	public LocationMarkerCollection(Context context, GoogleMap map) {
		this.context = context;
		this.map = map;
	}

	@Override
	public void add(MarkerOptions options, Pair<Location, User> pair) {
		Location location = pair.first;
		User user = pair.second;

		final Geometry g = location.getGeometry();
		if (g != null) {

			// one user has one location
			Marker marker = userIdToMarker.get(user.getId());
			if (marker != null) {
				markerIdToPair.remove(marker.getId());
				marker.remove();

				if (clickedAccuracyCircleUserId != null && clickedAccuracyCircleUserId.equals(user.getId())) {
					if (clickedAccuracyCircle != null) {
						clickedAccuracyCircle.remove();
						clickedAccuracyCircle = null;
					}
				}
			}

			options.visible(visible);

			marker = map.addMarker(options);
			userIdToMarker.put(user.getId(), marker);
			markerIdToPair.put(marker.getId(), pair);

			if (location.getTimestamp().after(latestLocationDate)) {
				latestLocationDate = location.getTimestamp();
			}
		}
	}

	// TODO: this should preserve latestLocationDate
	@Override
	public void remove(Pair<Location, User> pair) {
		Marker marker = userIdToMarker.remove(pair.second.getId());
		if (marker != null) {
			markerIdToPair.remove(marker.getId());
			marker.remove();
		}
	}

	@Override
	public Pair<Location, User> pointForMarker(Marker marker) {
		return markerIdToPair.get(marker.getId());
 	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		Pair<Location, User> pair = markerIdToPair.get(marker.getId());
		if (pair == null) {
			return false;
		}

		Location location = pair.first;
		User user = pair.second;

		final Geometry g = location.getGeometry();
		if (g != null) {
			Point point = GeometryUtils.getCentroid(g);
			LatLng latLng = new LatLng(point.getY(), point.getX());
			LocationProperty accuracyProperty = location.getPropertiesMap().get("accuracy");
			if (accuracyProperty != null && !accuracyProperty.getValue().toString().trim().isEmpty()) {
				try {
					Float accuracy = Float.valueOf(accuracyProperty.getValue().toString());
					if (clickedAccuracyCircle != null) {
						clickedAccuracyCircle.remove();
					}
					clickedAccuracyCircle = map.addCircle(new CircleOptions().center(latLng).radius(accuracy).fillColor(0x1D43b0ff).strokeColor(0x620069cc).strokeWidth(1.0f));
					clickedAccuracyCircleUserId = user.getId();
				} catch (NumberFormatException nfe) {
					Log.e(LOG_NAME, "Problem adding accuracy circle to the map.", nfe);
				}
			}
		}

		map.setInfoWindowAdapter(infoWindowAdapter);
		// make sure to set the Anchor after this call as well, because the size of the icon might have changed
		marker.setIcon(LocationBitmapFactory.bitmapDescriptor(context, location, user));
		marker.setAnchor(0.5f, 1.0f);
		marker.showInfoWindow();
		return true;
	}

	@Override
	public void offMarkerClick() {
		if (clickedAccuracyCircle != null) {
			clickedAccuracyCircle.remove();
			clickedAccuracyCircle = null;
		}
	}

	@Override
	public void refresh(Pair<Location, User> pair) {
		// TODO Maybe a different generic for this case
		// TODO implementing room might help solve this problem
		// In this case I know just the user is coming in
		// grab the location based on that
		User user = pair.second;
		Marker marker = userIdToMarker.get(pair.second.getId());

		if (marker != null) {
			Location location = markerIdToPair.get(marker.getId()).first;
			markerIdToPair.put(marker.getId(), new Pair(location, user));
			marker.setIcon(LocationBitmapFactory.bitmapDescriptor(context, location, user));
		}
	}

	@Override
	public void refreshMarkerIcons(Filter<Temporal> filter) {
		for (Marker m : userIdToMarker.values()) {
			Pair<Location, User> pair = markerIdToPair.get(m.getId());
			Location location = pair.first;
			User user = pair.second;
			if (location != null) {
				if (filter != null && !filter.passesFilter(location)) {
					remove(pair);
				} else {
					boolean showWindow = m.isInfoWindowShown();
					try {
						// make sure to set the Anchor after this call as well, because the size of the icon might have changed
						m.setIcon(LocationBitmapFactory.bitmapDescriptor(context, location, user));
						m.setAnchor(0.5f, 1.0f);
					} catch (Exception ue) {
						Log.e(LOG_NAME, "Error refreshing the icon for user: " + user.getId(), ue);
					}

					if (showWindow) {
						m.showInfoWindow();
					}
				}
			}
		}
	}

	@Override
	public void onMapClick(LatLng latLng) {
	}

	@Override
	public int count() {
		return userIdToMarker.size();
	}

	@Override
	public void clear() {
		for (Marker marker : userIdToMarker.values()) {
			marker.remove();
		}

		clickedAccuracyCircle = null;
		userIdToMarker.clear();
		markerIdToPair.clear();
		latestLocationDate = new Date(0);
	}

	@Override
	public void onCameraIdle() {
		// Don't care about this, I am not clustered
	}

	@Override
	public void setVisibility(boolean visible) {
		if (this.visible == visible)
			return;

		this.visible = visible;
		for (Marker m : userIdToMarker.values()) {
			m.setVisible(visible);
		}
		if (clickedAccuracyCircle != null) {
			clickedAccuracyCircle.setVisible(visible);
		}
	}

	@Override
	public boolean isVisible() {
		return this.visible;
	}

	@Override
	public Date getLatestDate() {
		return latestLocationDate;
	}

	private class LocationInfoWindowAdapter implements InfoWindowAdapter {
		private final Map<Marker, Drawable> avatars = new HashMap<>();

		@Override
		public View getInfoContents(final Marker marker) {
			Pair<Location, User> pair = markerIdToPair.get(marker.getId());
			final Location location = pair.first;
			final User user = pair.second;
			if (user == null || location == null) {
				return null;
			}

			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = inflater.inflate(R.layout.people_info_window, null);

			final ImageView avatarView = (ImageView) v.findViewById(R.id.avatarImageView);
			UserLocal userLocal = user.getUserLocal();
			if (userLocal.getLocalAvatarPath() != null) {
				final Drawable avatar = avatars.get(marker);
				if (avatar == null) {
					Glide.with(context)
							.load(userLocal.getLocalAvatarPath())
							.asBitmap()
							.dontAnimate()
							.centerCrop()
							.into(new BitmapImageViewTarget(avatarView) {
								@Override
								protected void setResource(Bitmap resource) {
									RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), resource);
									circularBitmapDrawable.setCircular(true);

									avatars.put(marker, circularBitmapDrawable);
									marker.showInfoWindow();
								}

								@Override public void onLoadCleared(Drawable placeholder) {
									avatars.remove(marker);
								}
							});
				} else {
					avatarView.setImageDrawable(avatar);
				}
			} else if (user.getAvatarUrl() != null) {
				new DownloadImageTask(context, Collections.singletonList(user), DownloadImageTask.ImageType.AVATAR, false).execute();
			}

			TextView name = (TextView) v.findViewById(R.id.name);
			name.setText(user.getDisplayName());

			TextView date = (TextView) v.findViewById(R.id.date);
			date.setText(new PrettyTime().format(location.getTimestamp()));

			return v;
		}

		@Override
		public View getInfoWindow(Marker marker) {
			return null; // Use default info window
		}
	}
}
