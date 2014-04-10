package mil.nga.giat.mage.newsfeed;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.observation.ObservationEditActivity;
import mil.nga.giat.mage.observation.ObservationViewActivity;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.event.IObservationEventListener;
import mil.nga.giat.mage.sdk.exceptions.ObservationException;
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
import android.widget.ListView;

import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;

public class NewsFeedFragment extends Fragment implements IObservationEventListener, OnItemClickListener, OnSharedPreferenceChangeListener {
	private NewsFeedCursorAdapter adapter;
	private PreparedQuery<Observation> query;
	private Dao<Observation, Long> oDao;
	private SharedPreferences sp;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_news_feed, container, false);
		setHasOptionsMenu(true);

		sp = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
		sp.registerOnSharedPreferenceChangeListener(this);
		ListView lv = (ListView) rootView.findViewById(R.id.news_feed_list);
		try {
			oDao = DaoStore.getInstance(getActivity().getApplicationContext()).getObservationDao();
			query = buildQuery(oDao, sp.getInt("activeTimeFilter", R.id.none_rb));
			Cursor c = obtainCursor(query, oDao);
			adapter = new NewsFeedCursorAdapter(getActivity().getApplicationContext(), c, query, getActivity());
			lv.setAdapter(adapter);
			lv.setOnItemClickListener(this);
			
			try {
				ObservationHelper.getInstance(getActivity().getApplicationContext()).addListener(this);
			} catch (ObservationException oe) {
				oe.printStackTrace();
			}
			// iterator.closeQuietly();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rootView;
	}
	
	@Override
	public void onDestroy() {
		sp.unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}
	
	@Override
	public void onItemClick(AdapterView<?> adapter, View arg1, int position, long id) {
		Cursor c = ((NewsFeedCursorAdapter) adapter.getAdapter()).getCursor();
		c.moveToPosition(position);
		try {
			Observation o = query.mapRow(new AndroidDatabaseResults(c, null));
			Intent observationView = new Intent(getActivity().getApplicationContext(), ObservationViewActivity.class);
	        observationView.putExtra(ObservationViewActivity.OBSERVATION_ID, o.getId());
	        getActivity().startActivityForResult(observationView, 2);
		} catch (Exception e) {
			
		}
	}
	
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // TODO Add your menu entries here
        super.onCreateOptionsMenu(menu, inflater);
        
        inflater.inflate(R.menu.landing, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.observation_new:
                 Intent intent = new Intent(getActivity(), ObservationEditActivity.class);
                 LocationService ls = ((MAGE) getActivity().getApplication()).getLocationService();
                 Location l = ls.getLocation();
                 intent.putExtra(ObservationEditActivity.LOCATION, l);
                 startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

	private PreparedQuery<Observation> buildQuery(Dao<Observation, Long> oDao, int filterId) throws SQLException {
		Log.i("test", "build query filter id: " + filterId);
		QueryBuilder<Observation, Long> qb = oDao.queryBuilder();
		Calendar c = Calendar.getInstance();
		switch(filterId) {
		case R.id.none_rb:
			Log.i("test", "no filter");
			// no filter
			c.setTime(new Date(0));
			break;
		case R.id.last_hour_rb:
			c.add(Calendar.HOUR, -1);
			break;
		case R.id.last_six_hours_rb:
			c.add(Calendar.HOUR, -6);
			break;
		case R.id.last_twelve_hours_rb:
			c.add(Calendar.HOUR, -12);
			break;
		case R.id.last_24_hours_rb:
			c.add(Calendar.HOUR, -24);
			break;
		case R.id.since_midnight_rb:
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			break;
		default:
			// just set no filter
			c.setTime(new Date(0));
			break;
		}
		Log.i("Test", "C.get time is: " + c.getTime());
		qb.where().gt("last_modified", c.getTime());
		qb.orderBy("last_modified", false);

		return qb.prepare();
	}

	private Cursor obtainCursor(PreparedQuery<Observation> query, Dao<Observation, Long> oDao) throws SQLException {
		// build your query
		QueryBuilder<Observation, Long> qb = oDao.queryBuilder();
		qb.where().gt("_id", 0);
		// this is wrong.  need to figure out how to order on nested table or move the correct field up
		qb.orderBy("last_modified", false);

		Cursor c = null;
		CloseableIterator<Observation> iterator = oDao.iterator(query);

		// get the raw results which can be cast under Android
		AndroidDatabaseResults results = (AndroidDatabaseResults) iterator.getRawResults();
		c = results.getRawCursor();
		return c;
	}

	@Override
	public void onError(Throwable error) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onObservationCreated(final Collection<Observation> observations) {
		 getActivity().runOnUiThread(new Runnable() {
		 @Override
		 public void run() {
			 try {
			 adapter.changeCursor(obtainCursor(query, oDao));
			 } catch (Exception e) {
				 Log.e("NewsFeedFragment", "Unable to change cursor", e);
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
			 adapter.changeCursor(obtainCursor(query, oDao));
			 } catch (Exception e) {
				 Log.e("NewsFeedFragment", "Unable to change cursor", e);
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
    			 adapter.changeCursor(obtainCursor(query, oDao));
    			 } catch (Exception e) {
    				 Log.e("NewsFeedFragment", "Unable to change cursor", e);
    			 }
    		 }
		 });
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.i("test", "the key is: " + key);
		if ("activeTimeFilter".equalsIgnoreCase(key)) { 
			Log.i("map test", "Active filter changed to: " + sharedPreferences.getInt(key, R.id.none_rb));
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
					 Log.e("NewsFeedFragment", "Unable to change cursor", e);
				 }
			 }
		 });
	}
}