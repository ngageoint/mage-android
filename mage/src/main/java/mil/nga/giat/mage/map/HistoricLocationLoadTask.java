package mil.nga.giat.mage.map;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Locale;

import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.map.marker.LocationBitmapFactory;
import mil.nga.giat.mage.map.marker.PointCollection;
import mil.nga.giat.mage.sdk.Temporal;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.utils.DateFormatFactory;
import mil.nga.sf.Point;
import mil.nga.sf.util.GeometryUtils;

public class HistoricLocationLoadTask extends AsyncTask<Void, Pair<MarkerOptions, Pair<Location, User>>, Void> {

	private Context context;
	private Filter<Temporal> filter;
	private final  PointCollection<Pair<Location, User>> historicLocationCollection;
	private DateFormat dateFormat;

	public HistoricLocationLoadTask(Context context, PointCollection<Pair<Location, User>> historicLocationCollection) {
		this.context = context.getApplicationContext();
		this.historicLocationCollection = historicLocationCollection;
		dateFormat = DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), context);

	}

	public void setFilter(Filter<Temporal> filter) {
		this.filter = filter;
	}

	@Override
	protected Void doInBackground(Void... params) {
		try {
			User currentUser = null;
			try {
				currentUser = UserHelper.getInstance(context.getApplicationContext()).readCurrentUser();
			} catch (UserException e) {
				e.printStackTrace();
			}

			for (Location location : getQuery(currentUser).query()) {
				Point point = GeometryUtils.getCentroid(location.getGeometry());
				LatLng latLng = new LatLng(point.getY(), point.getX());
				MarkerOptions options = new MarkerOptions()
						.position(latLng)
						.icon(LocationBitmapFactory.dotBitmapDescriptor(context, location, currentUser))
						.title("My Location")
						.snippet(dateFormat.format(location.getTimestamp()));

				publishProgress(new Pair<>(options, new Pair<>(location, location.getUser())));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	protected void onProgressUpdate(Pair<MarkerOptions, Pair<Location, User>>... pairs) {
		historicLocationCollection.add(pairs[0].first, pairs[0].second);
	}

	private QueryBuilder<Location, Long> getQuery(User user) throws SQLException {
		Dao<Location, Long> dao = DaoStore.getInstance(context).getLocationDao();
		QueryBuilder<Location, Long> query = dao.queryBuilder();

		Where<? extends Temporal, Long> where = query.where();
		if (user != null) {
			where.eq("user_id", user.getId()).and().eq("event_id", user.getUserLocal().getCurrentEvent().getId());
		}

		if (filter != null) {
			filter.and(where);
		}

		query.orderBy("timestamp", false);

		return query;
	}
}