package mil.nga.giat.mage.newsfeed;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.observation.ObservationEditActivity;
import mil.nga.giat.mage.observation.ObservationViewActivity;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.location.LocationProperty;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.event.IObservationEventListener;
import mil.nga.giat.mage.sdk.location.LocationService;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class ObservationFeedFragment extends Fragment implements IObservationEventListener, OnItemClickListener, OnSharedPreferenceChangeListener {

	private static final String LOG_NAME = ObservationFeedFragment.class.getName();
	
	private ObservationFeedCursorAdapter adapter;
	private PreparedQuery<Observation> query;
	private Dao<Observation, Long> oDao;
	private SharedPreferences sp;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> queryUpdateHandle;
	private long requeryTime;
	private ViewGroup footer;
	private View rootView;
	private ListView lv;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.i("Test", "On create view called in news feed fragment");
		rootView = inflater.inflate(R.layout.fragment_news_feed, container, false);
		setHasOptionsMenu(true);
		lv = (ListView) rootView.findViewById(R.id.news_feed_list);
		footer = (ViewGroup) inflater.inflate(R.layout.feed_footer, lv, false);
        lv.addFooterView(footer, null, false);
		
		sp = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
		sp.registerOnSharedPreferenceChangeListener(this);
		
		try {
			oDao = DaoStore.getInstance(getActivity().getApplicationContext()).getObservationDao();
			query = buildQuery(oDao, getTimeFilterId());
			Cursor c = obtainCursor(query, oDao);
			adapter = new ObservationFeedCursorAdapter(getActivity().getApplicationContext(), c, query, getActivity());
			lv.setAdapter(adapter);
			lv.setOnItemClickListener(this);

			ObservationHelper.getInstance(getActivity().getApplicationContext()).addListener(this);

        } catch (Exception e) {
        	Log.e(LOG_NAME, "Problem getting cursor or setting adapter.", e);
        }
		return rootView;
	}

	@Override
	public void onDestroy() {
		sp.unregisterOnSharedPreferenceChangeListener(this);
		if (queryUpdateHandle != null) {
			queryUpdateHandle.cancel(true);
		}
		ObservationHelper.getInstance(getActivity().getApplicationContext()).removeListener(this);
		super.onDestroy();
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View arg1, int position, long id) {
		HeaderViewListAdapter headerAdapter = (HeaderViewListAdapter)adapter.getAdapter();
		Cursor c = ((ObservationFeedCursorAdapter) headerAdapter.getWrappedAdapter()).getCursor();
		c.moveToPosition(position);
		try {
			Observation o = query.mapRow(new AndroidDatabaseResults(c, null));
			Intent observationView = new Intent(getActivity().getApplicationContext(), ObservationViewActivity.class);
			observationView.putExtra(ObservationViewActivity.OBSERVATION_ID, o.getId());
			getActivity().startActivityForResult(observationView, 2);
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem.", e);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		inflater.inflate(R.menu.observation_new, menu);
	    inflater.inflate(R.menu.filter, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.observation_new:
			Intent intent = new Intent(getActivity(), ObservationEditActivity.class);
			LocationService ls = ((MAGE) getActivity().getApplication()).getLocationService();
			Location l = ls.getLocation();
			if (l == null) {
				List<mil.nga.giat.mage.sdk.datastore.location.Location> tLocations = LocationHelper.getInstance(getActivity().getApplicationContext()).getCurrentUserLocations(getActivity().getApplicationContext(), 1);
				if (!tLocations.isEmpty()) {
					mil.nga.giat.mage.sdk.datastore.location.Location tLocation = tLocations.get(0);
					Geometry geo = tLocation.getLocationGeometry().getGeometry();
					Map<String, LocationProperty> propertiesMap = tLocation.getPropertiesMap();
					if (geo instanceof Point) {
						Point point = (Point) geo;
						String provider = "manual";
						if (propertiesMap.get("provider").getValue() != null) {
							provider = propertiesMap.get("provider").getValue().toString();
						}
						l = new Location(provider);
						l.setTime(tLocation.getTimestamp().getTime());
						if (propertiesMap.get("accuracy").getValue() != null) {
							l.setAccuracy((Float) propertiesMap.get("accuracy").getValue());
						}
						l.setLatitude(point.getY());
						l.setLongitude(point.getX());
					}
				}
			}
			if(l != null) {
				intent.putExtra(ObservationEditActivity.LOCATION, l);
				startActivity(intent);
			}
		}

		return super.onOptionsItemSelected(item);
	}

	private PreparedQuery<Observation> buildQuery(Dao<Observation, Long> oDao, int filterId) throws SQLException {
		QueryBuilder<Observation, Long> qb = oDao.queryBuilder();
		Calendar c = Calendar.getInstance();
		String title = "All Observations";
		String footerText = "All observations have been returned";
		switch (filterId) {
		default:
		case R.id.none_rb:
			// no filter
			c.setTime(new Date(0));
			break;
		case R.id.last_hour_rb:
			title = "Last Hour";
			footerText = "End of results for Last Hour filter";
			c.add(Calendar.HOUR, -1);
			break;
		case R.id.last_six_hours_rb:
			title = "Last 6 Hours";
			footerText = "End of results for Last 6 Hours filter";
			c.add(Calendar.HOUR, -6);
			break;
		case R.id.last_twelve_hours_rb:
			title = "Last 12 Hours";
			footerText = "End of results for Last 12 Hours filter";
			c.add(Calendar.HOUR, -12);
			break;
		case R.id.last_24_hours_rb:
			title = "Last 24 Hours";
			footerText = "End of results for Last 24 Hours filter";
			c.add(Calendar.HOUR, -24);
			break;
		case R.id.since_midnight_rb:
			title = "Since Midnight";
			footerText = "End of results for Today filter";
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			break;
		}
		requeryTime = c.getTimeInMillis();
		TextView footerTextView = (TextView)footer.findViewById(R.id.footer_text);
		footerTextView.setText(footerText);
		getActivity().getActionBar().setTitle(title);
		qb.where().gt("last_modified", c.getTime());
		qb.orderBy("last_modified", false);

		return qb.prepare();
	}

	private Cursor obtainCursor(PreparedQuery<Observation> query, Dao<Observation, Long> oDao) throws SQLException {
		Cursor c = null;
		CloseableIterator<Observation> iterator = oDao.iterator(query);

		// get the raw results which can be cast under Android
		AndroidDatabaseResults results = (AndroidDatabaseResults) iterator.getRawResults();
		c = results.getRawCursor();
		if (c.moveToLast()) {
			long oldestTime = c.getLong(c.getColumnIndex("last_modified"));
			Log.i("test", "last modified is: " + c.getLong(c.getColumnIndex("last_modified")));
			Log.i("test", "querying again in: " + (oldestTime - requeryTime)/60000 + " minutes");
			if (queryUpdateHandle != null) {
				queryUpdateHandle.cancel(true);
			}
			queryUpdateHandle = scheduler.schedule(new Runnable() {
				public void run() {
					updateTimeFilter(getTimeFilterId());
				}
			}, oldestTime - requeryTime, TimeUnit.MILLISECONDS);
			c.moveToFirst();
		}
		return c;
	}
	
	private int getTimeFilterId() {
		return sp.getInt(getResources().getString(R.string.activeTimeFilterKey), R.id.none_rb);
	}

	@Override
	public void onError(Throwable error) {
	}

	@Override
	public void onObservationCreated(final Collection<Observation> observations) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					query = buildQuery(oDao, getTimeFilterId());
					adapter.changeCursor(obtainCursor(query, oDao));
				} catch (Exception e) {
					Log.e(LOG_NAME, "Unable to change cursor", e);
				}
			}
		});

	}

	@Override
	public void onObservationDeleted(final Observation observation) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					query = buildQuery(oDao, getTimeFilterId());
					adapter.changeCursor(obtainCursor(query, oDao));
				} catch (Exception e) {
					Log.e(LOG_NAME, "Unable to change cursor", e);
				}
			}
		});
	}

	@Override
	public void onObservationUpdated(final Observation observation) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					query = buildQuery(oDao, getTimeFilterId());
					adapter.changeCursor(obtainCursor(query, oDao));
				} catch (Exception e) {
					Log.e(LOG_NAME, "Unable to change cursor", e);
				}
			}
		});
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (getResources().getString(R.string.activeTimeFilterKey).equalsIgnoreCase(key)) {
			updateTimeFilter(sharedPreferences.getInt(key, 0));
		}
	}

	private void updateTimeFilter(final int filterId) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					query = buildQuery(oDao, filterId);
					adapter.changeCursor(obtainCursor(query, oDao));
				} catch (Exception e) {
					Log.e(LOG_NAME, "Unable to change cursor", e);
				}
			}
		});
	}
}