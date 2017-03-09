package mil.nga.giat.mage.newsfeed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
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
import com.j256.ormlite.stmt.Where;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.filter.FilterActivity;
import mil.nga.giat.mage.profile.ProfileActivity;
import mil.nga.giat.mage.profile.ProfileFragment;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.event.ILocationEventListener;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.fetch.LocationRefreshIntent;

public class PeopleFeedFragment extends Fragment implements OnSharedPreferenceChangeListener, OnItemClickListener, ILocationEventListener {
	
	private static final String LOG_NAME = PeopleFeedFragment.class.getName();
	
    private PeopleCursorAdapter adapter;
    private PreparedQuery<Location> query;
    private ViewGroup footer;
    private SharedPreferences sp;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> queryUpdateHandle;
    private SwipeRefreshLayout swipeContainer;
    private CoordinatorLayout coordinatorLayout;
    private LocationRefreshReceiver locationRefreshReceiver;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        locationRefreshReceiver = new LocationRefreshReceiver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_feed_people, container, false);
        setHasOptionsMenu(true);

        coordinatorLayout = (CoordinatorLayout) rootView.findViewById(R.id.coordinator_layout);

        swipeContainer = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeContainer);
        swipeContainer.setColorSchemeResources(R.color.md_blue_600, R.color.md_orange_A200);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshLocations();
            }
        });

        ListView lv = (ListView) rootView.findViewById(R.id.people_feed_list);
        footer = (ViewGroup) inflater.inflate(R.layout.feed_footer, lv, false);
        lv.addFooterView(footer, null, false);
        
        sp = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

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

    @Override
    public void onResume() {
    	super.onResume();

        sp.registerOnSharedPreferenceChangeListener(this);
        LocationHelper.getInstance(getActivity()).addListener(this);
        locationRefreshReceiver.register();
    }

    @Override
    public void onPause() {
    	super.onPause();

        sp.unregisterOnSharedPreferenceChangeListener(this);
        LocationHelper.getInstance(getActivity()).removeListener(this);
        locationRefreshReceiver.unregister();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (queryUpdateHandle != null) {
            queryUpdateHandle.cancel(true);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.filter, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.filter_button:
                Intent intent = new Intent(getActivity(), FilterActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getResources().getString(R.string.activeTimeFilterKey).equalsIgnoreCase(key)) {
            updateTimeFilter(sharedPreferences.getInt(key, 0));
        }
    }

    private void refreshLocations() {
        Intent intent = new Intent(getContext(), LocationRefreshIntent.class);
        getActivity().startService(intent);
    }

    private int getTimeFilterId() {
        return sp.getInt(getResources().getString(R.string.activeTimeFilterKey), getResources().getInteger(R.integer.time_filter_none));
    }

    private Cursor obtainCursor(PreparedQuery<Location> query, Dao<Location, Long> lDao) throws SQLException {
        Cursor c = null;
        CloseableIterator<Location> iterator = lDao.iterator(query);

        // get the raw results which can be cast under Android
        AndroidDatabaseResults results = (AndroidDatabaseResults) iterator.getRawResults();
        c = results.getRawCursor();
        if (c.moveToLast()) {
            if (queryUpdateHandle != null) {
                queryUpdateHandle.cancel(true);
            }
            queryUpdateHandle = scheduler.schedule(new Runnable() {
                public void run() {
                    updateTimeFilter(getTimeFilterId());
                }
            }, 30*1000, TimeUnit.MILLISECONDS);
            c.moveToFirst();
        }
        return c;
    }

    private void updateTimeFilter(final int filterId) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                	Dao<Location, Long> locationDao = DaoStore.getInstance(getActivity().getApplicationContext()).getLocationDao();
                    query = buildQuery(locationDao, filterId);
                    adapter.changeCursor(obtainCursor(query, locationDao));
                } catch (Exception e) {
                    Log.e(LOG_NAME, "Unable to change cursor", e);
                }
            }
        });
    }
    
    private PreparedQuery<Location> buildQuery(Dao<Location, Long> lDao, int filterId) throws SQLException {
        QueryBuilder<Location, Long> qb = lDao.queryBuilder();
        Calendar c = Calendar.getInstance();
		String subtitle = "";
		String footerText = "All people have been returned";

        if (filterId == getResources().getInteger(R.integer.time_filter_last_month)) {
            subtitle = "Last Month";
            footerText = "End of results for Last Month filter";
            c.add(Calendar.MONTH, -1);
        } else if (filterId == getResources().getInteger(R.integer.time_filter_last_week)) {
            subtitle = "Last Week";
            footerText = "End of results for Last Week filter";
            c.add(Calendar.DAY_OF_MONTH, -7);
        } else if (filterId == getResources().getInteger(R.integer.time_filter_last_24_hours)) {
            subtitle = "Last 24 Hours";
            footerText = "End of results for Last 24 Hours filter";
            c.add(Calendar.HOUR, -24);
        } else if (filterId == getResources().getInteger(R.integer.time_filter_today)) {
            subtitle = "Since Midnight";
            footerText = "End of results for Today filter";
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
        } else {
            // no filter
            c.setTime(new Date(0));
        }

        TextView footerTextView = (TextView)footer.findViewById(R.id.footer_text);
        footerTextView.setText(footerText);
		User currentUser = null;
		try {
			currentUser = UserHelper.getInstance(getActivity().getApplicationContext()).readCurrentUser();
		} catch (UserException e) {
			e.printStackTrace();
		}
		Where<Location, Long> where = qb.where().gt("timestamp", c.getTime());
		if (currentUser != null) {
			where.and().ne("user_id", currentUser.getId()).and().eq("event_id", currentUser.getUserLocal().getCurrentEvent().getId());
		}

		qb.orderBy("timestamp", false);


        ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(subtitle);

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
			Location l = query.mapRow(new AndroidDatabaseResults(c, null, false));
			Intent profileView = new Intent(getActivity().getApplicationContext(), ProfileActivity.class);
			profileView.putExtra(ProfileFragment.USER_ID, l.getUser().getRemoteId());
			getActivity().startActivityForResult(profileView, 2);
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem.", e);
		}
	}

	@Override
	public void onError(Throwable error) {
		
	}

	@Override
	public void onLocationCreated(Collection<Location> location) {
		updateTimeFilter(getTimeFilterId());
	}

	@Override
	public void onLocationUpdated(Location location) {
		updateTimeFilter(getTimeFilterId());
	}

	@Override
	public void onLocationDeleted(Collection<Location> location) {
		updateTimeFilter(getTimeFilterId());
	}

    public class LocationRefreshReceiver extends BroadcastReceiver {
        public void register() {
            IntentFilter filter = new IntentFilter(LocationRefreshIntent.ACTION_LOCATIONS_REFRESHED);
            filter.addCategory(Intent.CATEGORY_DEFAULT);
            getContext().registerReceiver(locationRefreshReceiver, filter);
        }

        public void unregister() {
            getContext().unregisterReceiver(locationRefreshReceiver);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            swipeContainer.setRefreshing(false);

            String status = intent.getExtras().getString(LocationRefreshIntent.EXTRA_LOCATIONS_REFRESH_STATUS, null);
            if (status != null) {
                final Snackbar snackbar = Snackbar
                        .make(coordinatorLayout, status, Snackbar.LENGTH_LONG)
                        .setAction("RETRY", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                refreshLocations();
                            }
                        });

                snackbar.show();
            }
        }
    }
}
