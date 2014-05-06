package mil.nga.giat.mage.newsfeed;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import mil.nga.giat.mage.LandingActivity;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.map.MapFragment;
import mil.nga.giat.mage.navigation.DrawerItem;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
import com.j256.ormlite.stmt.Where;
import com.vividsolutions.jts.geom.Point;

public class PeopleFeedFragment extends Fragment implements OnSharedPreferenceChangeListener, OnItemClickListener {
	
	private static final String LOG_NAME = PeopleFeedFragment.class.getName();
	
    private PeopleCursorAdapter adapter;
    private PreparedQuery<Location> query;
    private ViewGroup footer;
    private SharedPreferences sp;
    private long requeryTime;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> queryUpdateHandle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_feed_people, container, false);
        setHasOptionsMenu(true);

        ListView lv = (ListView) rootView.findViewById(R.id.people_feed_list);
        footer = (ViewGroup) inflater.inflate(R.layout.feed_footer, lv, false);
        lv.addFooterView(footer, null, false);
        
        sp = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        sp.registerOnSharedPreferenceChangeListener(this);
        
        try {
        	Dao<Location, Long> locationDao = DaoStore.getInstance(getActivity().getApplicationContext()).getLocationDao();
            query = buildQuery(locationDao, getTimeFilterId());
            Cursor c = obtainCursor(query, locationDao);
            adapter = new PeopleCursorAdapter(getActivity().getApplicationContext(), c, query);
            footer = (ViewGroup) inflater.inflate(R.layout.feed_footer, lv, false);
            footer.setVisibility(View.GONE);
            lv.setAdapter(adapter);
            lv.setOnItemClickListener(this);
        } catch (Exception e) {
        	Log.e(LOG_NAME, "Problem getting cursor or setting adapter.", e);
        }
        return rootView;
    }
    
    private int getTimeFilterId() {
        return sp.getInt(getResources().getString(R.string.activeTimeFilterKey), R.id.none_rb);
    }

    private Cursor obtainCursor(PreparedQuery<Location> query, Dao<Location, Long> lDao) throws SQLException {
        Cursor c = null;
        CloseableIterator<Location> iterator = lDao.iterator(query);

        // get the raw results which can be cast under Android
        AndroidDatabaseResults results = (AndroidDatabaseResults) iterator.getRawResults();
        c = results.getRawCursor();
        if (c.moveToLast()) {
            long oldestTime = c.getLong(c.getColumnIndex("timestamp"));
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
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.filter, menu);
    }
    
    @Override
    public void onDestroy() {
        sp.unregisterOnSharedPreferenceChangeListener(this);
        if (queryUpdateHandle != null) {
            queryUpdateHandle.cancel(true);
        }

        super.onDestroy();
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
                	Dao<Location, Long> locationDao = DaoStore.getInstance(getActivity().getApplicationContext()).getLocationDao();
                    adapter.changeCursor(obtainCursor(buildQuery(locationDao, filterId), locationDao));
                } catch (Exception e) {
                    Log.e(LOG_NAME, "Unable to change cursor", e);
                }
            }
        });
    }
    
    private PreparedQuery<Location> buildQuery(Dao<Location, Long> lDao, int filterId) throws SQLException {
        QueryBuilder<Location, Long> qb = lDao.queryBuilder();
        Calendar c = Calendar.getInstance();
		String title = "All People";
		String footerText = "All people have been returned";
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
		User currentUser = null;
		try {
			currentUser = UserHelper.getInstance(getActivity().getApplicationContext()).readCurrentUser();
		} catch (UserException e) {
			e.printStackTrace();
		}
		Where<Location, Long> where = qb.where().gt("timestamp", c.getTime());
		if (currentUser != null) {
			where.and().ne("user_id", currentUser.getId()).query();
		}

		qb.orderBy("timestamp", false);

        return qb.prepare();
    }
    
    /**
     * Zoom to map.
     * 
     */
	@Override
	public void onItemClick(AdapterView<?> adapter, View arg1, int position, long id) {
		HeaderViewListAdapter headerAdapter = (HeaderViewListAdapter)adapter.getAdapter();
		Cursor c = ((PeopleCursorAdapter) headerAdapter.getWrappedAdapter()).getCursor();
		c.moveToPosition(position);
		try {
			Location l = query.mapRow(new AndroidDatabaseResults(c, null));
			Point p = (Point)l.getLocationGeometry().getGeometry();
            Editor e = sp.edit();
            e.putFloat(getResources().getString(R.string.mapZoomLatKey), Double.valueOf(p.getY()).floatValue()).commit();
            e.putFloat(getResources().getString(R.string.mapZoomLonKey), Double.valueOf(p.getX()).floatValue()).commit();
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().remove(this).commit();
            DrawerItem mapItem = ((LandingActivity)getActivity()).getMapItem();
            fragmentManager.beginTransaction().add(R.id.content_frame, ((MapFragment)mapItem.getFragment())).commit();
            ((LandingActivity)getActivity()).setCurrentItem(mapItem);
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem.", e);
		}
	}
}
