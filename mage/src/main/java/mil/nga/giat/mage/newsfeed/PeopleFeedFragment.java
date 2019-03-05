package mil.nga.giat.mage.newsfeed;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
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
import com.j256.ormlite.android.apptools.support.OrmLiteCursorLoader;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.filter.LocationFilterActivity;
import mil.nga.giat.mage.location.LocationServerFetch;
import mil.nga.giat.mage.profile.ProfileActivity;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.event.ILocationEventListener;
import mil.nga.giat.mage.sdk.exceptions.UserException;

public class PeopleFeedFragment extends DaggerFragment implements OnItemClickListener, ILocationEventListener, LoaderManager.LoaderCallbacks<Cursor> {
	
	private static final String LOG_NAME = PeopleFeedFragment.class.getName();

	@Inject
    Context context;

	@Inject
    SharedPreferences preferences;

    private PeopleCursorAdapter adapter;
    private PreparedQuery<Location> query;
    private ViewGroup footer;
    private ListView lv;
    private Parcelable listState;
    private SwipeRefreshLayout swipeContainer;
    private PeopleFeedFragment.RefreshUI refreshTask;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_feed_people, container, false);
        setHasOptionsMenu(true);

        swipeContainer = rootView.findViewById(R.id.swipeContainer);
        swipeContainer.setColorSchemeResources(R.color.md_blue_600, R.color.md_orange_A200);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new LocationRefreshTask().execute();
            }
        });

        footer = (ViewGroup) inflater.inflate(R.layout.feed_footer, lv, false);

        try {
            Dao<Location, Long> dao = DaoStore.getInstance(context).getLocationDao();
            query = buildQuery(dao, getTimeFilterId());
            adapter = new PeopleCursorAdapter(context, null, query);
        } catch(SQLException e) {
            Log.e(LOG_NAME, "Could not setup people feed query", e);
        }

        lv = rootView.findViewById(R.id.people_feed_list);
        lv.addFooterView(footer, null, false);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);

        getLoaderManager().initLoader(0, null, this);

        return rootView;
    }

    @Override
    public void onResume() {
    	super.onResume();

        LocationHelper.getInstance(context).addListener(this);

        reload();

        footer.setVisibility(View.GONE);

        if (listState != null) {
            lv.onRestoreInstanceState(listState);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        LocationHelper.getInstance(context).removeListener(this);

        listState = lv.onSaveInstanceState();

        if (refreshTask != null) {
            getView().removeCallbacks(refreshTask);
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
                Intent intent = new Intent(context, LocationFilterActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        try {
            Dao<Location, Long> dao = DaoStore.getInstance(context).getLocationDao();
            query = buildQuery(dao, getTimeFilterId());
            return new OrmLiteCursorLoader(getContext(), dao, query);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null) {
            adapter.swapCursor(cursor);
            adapter.notifyDataSetChanged();

            // setup timer to update UI
            if (refreshTask != null) {
                getView().removeCallbacks(refreshTask);
            }

            refreshTask = new PeopleFeedFragment.RefreshUI();
            scheduleRefresh(refreshTask);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    private int getTimeFilterId() {
        return preferences.getInt(getResources().getString(R.string.activeLocationTimeFilterKey), getResources().getInteger(R.integer.time_filter_none));
    }

	private int getCustomTimeNumber() {
		return preferences.getInt(getResources().getString(R.string.customLocationTimeNumberFilterKey), 0);
	}

	private String getCustomTimeUnit() {
		return preferences.getString(getResources().getString(R.string.customLocationTimeUnitFilterKey), getResources().getStringArray(R.array.timeUnitEntries)[0]);
	}
    
    private PreparedQuery<Location> buildQuery(Dao<Location, Long> dao, int filterId) throws SQLException {
        QueryBuilder<Location, Long> qb = dao.queryBuilder();
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
        }  else if (filterId == getResources().getInteger(R.integer.time_filter_custom)) {
			String customFilterTimeUnit = getCustomTimeUnit();
			int customTimeNumber = getCustomTimeNumber();

			subtitle = "Last " + customTimeNumber + " " + customFilterTimeUnit;
			footerText = "End of results for custom filter";
			switch (customFilterTimeUnit) {
				case "Hours":
					c.add(Calendar.HOUR, -1 * customTimeNumber);
					break;
				case "Days":
					c.add(Calendar.DAY_OF_MONTH, -1 * customTimeNumber);
					break;
				case "Months":
					c.add(Calendar.MONTH, -1 * customTimeNumber);
					break;
				default:
					c.add(Calendar.MINUTE, -1 * customTimeNumber);
					break;
			}

		} else {
            // no filter
            c.setTime(new Date(0));
        }

        TextView footerTextView = footer.findViewById(R.id.footer_text);
        footerTextView.setText(footerText);
		User currentUser = null;
		try {
			currentUser = UserHelper.getInstance(context).readCurrentUser();
		} catch (UserException e) {
			e.printStackTrace();
		}

		Where<Location, Long> where = qb.where().gt("timestamp", c.getTime());
		if (currentUser != null) {
			where
                .and()
                .ne("user_id", currentUser.getId())
                .and()
                .eq("event_id", currentUser.getUserLocal().getCurrentEvent().getId());
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
			Intent profileView = new Intent(context, ProfileActivity.class);
			profileView.putExtra(ProfileActivity.USER_ID, l.getUser().getRemoteId());
			getActivity().startActivity(profileView);
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem.", e);
		}
	}

	@Override
	public void onError(Throwable error) {
		
	}

	@Override
	public void onLocationCreated(Collection<Location> location) {
        reload();
	}

	@Override
	public void onLocationUpdated(Location location) {
        reload();
	}

	@Override
	public void onLocationDeleted(Collection<Location> location) {
	    reload();
	}

    private void scheduleRefresh(PeopleFeedFragment.RefreshUI task) {
        getView().postDelayed(task, 60 * 1000);
    }

    private void reload() {
	    getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getLoaderManager().restartLoader(0, null, PeopleFeedFragment.this);
            }
        });
    }

    private class LocationRefreshTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            new LocationServerFetch(context).fetch();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            swipeContainer.setRefreshing(false);
        }
    }

    private class RefreshUI implements Runnable {
        public void run() {
            if (PeopleFeedFragment.this.isDetached()) return;

            reload();
        }
    }
}
