package mil.nga.giat.mage.map;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.vividsolutions.jts.geom.Point;

import java.sql.SQLException;

import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.map.marker.LocationBitmapFactory;
import mil.nga.giat.mage.map.marker.PointCollection;
import mil.nga.giat.mage.sdk.Temporal;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;

public class LocationLoadTask extends AsyncTask<Void, Pair<MarkerOptions, Pair<Location, User>>, Void> {

	private Context context;
	private Filter<Temporal> filter;
	private final PointCollection<Pair<Location, User>> locationCollection;

	public LocationLoadTask(Context context, PointCollection<Pair<Location, User>> locationCollection) {
		this.context = context.getApplicationContext();
		this.locationCollection = locationCollection;
	}

	public void setFilter(Filter<Temporal> filter) {
		this.filter = filter;
	}

	@Override
	protected Void doInBackground(Void... params) {
		CloseableIterator<Location> iterator = null;
		try {
			iterator = iterator();
			while (iterator.hasNext()) {
				Location location = iterator.current();
				User user = location.getUser();
				if (user == null) {
					continue;
				}

				Point point = location.getGeometry().getCentroid();
				LatLng latLng = new LatLng(point.getY(), point.getX());
				MarkerOptions options = new MarkerOptions().position(latLng).icon(LocationBitmapFactory.bitmapDescriptor(context, location, user));

				publishProgress(new Pair<>(options, new Pair<>(location, user)));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (iterator != null) {
				iterator.closeQuietly();
			}
		}

		return null;
	}

	@Override
	protected void onProgressUpdate(Pair<MarkerOptions, Pair<Location, User>>... pairs) {
		locationCollection.add(pairs[0].first, pairs[0].second);
	}

	private CloseableIterator<Location> iterator() throws SQLException {
		Dao<Location, Long> dao = DaoStore.getInstance(context).getLocationDao();
		QueryBuilder<Location, Long> query = dao.queryBuilder();
		Where<? extends Temporal, Long> where = query.where().ge("timestamp", locationCollection.getLatestDate());
		User currentUser = null;
		try {
			currentUser = UserHelper.getInstance(context.getApplicationContext()).readCurrentUser();
		} catch (UserException e) {
			e.printStackTrace();
		}
		if (currentUser != null) {
			where.and().ne("user_id", currentUser.getId()).and().eq("event_id", currentUser.getUserLocal().getCurrentEvent().getId());
		}
		if (filter != null) {
			filter.and(where);
		}
		query.orderBy("timestamp", false);


		return dao.iterator(query.prepare());
	}
}