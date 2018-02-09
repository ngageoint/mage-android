package mil.nga.giat.mage.newsfeed;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;

import org.apache.commons.lang3.StringUtils;

import java.sql.SQLException;
import java.util.ArrayList;
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
import mil.nga.giat.mage.filter.ObservationFilterActivity;
import mil.nga.giat.mage.observation.AttachmentGallery;
import mil.nga.giat.mage.observation.AttachmentViewerActivity;
import mil.nga.giat.mage.observation.ObservationEditActivity;
import mil.nga.giat.mage.observation.ObservationFormPickerActivity;
import mil.nga.giat.mage.observation.ObservationLocation;
import mil.nga.giat.mage.observation.ObservationViewActivity;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.location.LocationProperty;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationFavorite;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationImportant;
import mil.nga.giat.mage.sdk.datastore.observation.State;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.event.IObservationEventListener;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.fetch.ObservationRefreshIntent;
import mil.nga.giat.mage.sdk.location.LocationService;
import mil.nga.wkb.geom.Geometry;

public class ObservationFeedFragment extends Fragment implements IObservationEventListener, ObservationListAdapter.ObservationActionListener {

	private static final String LOG_NAME = ObservationFeedFragment.class.getName();

	private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
	private static final String BUNDLE_RECYCLER_LAYOUT = "BUNDLE_RECYCLER_LAYOUT";

	private PreparedQuery<Observation> query;
	private Dao<Observation, Long> oDao;
	private SharedPreferences sp;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> queryUpdateHandle;
	private long requeryTime;
	private ViewGroup footer;
	private ListView lv;
	private RecyclerView recyclerView;
	private ObservationListAdapter adapter;

	Parcelable listState;
	private User currentUser;
	private LocationService locationService;
	private SwipeRefreshLayout swipeContainer;
	private AttachmentGallery attachmentGallery;
	private CoordinatorLayout coordinatorLayout;
	private ObservationRefreshReceiver observationRefreshReceiver;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			currentUser= UserHelper.getInstance(getActivity().getApplicationContext()).readCurrentUser();
		} catch (UserException e) {
			Log.e(LOG_NAME, "Error reading current user", e);
		}

		locationService = ((MAGE) getActivity().getApplication()).getLocationService();
		observationRefreshReceiver = new ObservationRefreshReceiver();

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_news_feed, container, false);
		setHasOptionsMenu(true);

		coordinatorLayout = (CoordinatorLayout) rootView.findViewById(R.id.coordinator_layout);

		swipeContainer = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeContainer);
		swipeContainer.setColorSchemeResources(R.color.md_blue_600, R.color.md_orange_A200);
		swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				refreshObservations();
			}
		});

		rootView.findViewById(R.id.new_observation_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onNewObservation();
			}
		});

        attachmentGallery = new AttachmentGallery(getActivity().getApplicationContext(), 200, 200);
        attachmentGallery.addOnAttachmentClickListener(new AttachmentGallery.OnAttachmentClickListener() {
            @Override
            public void onAttachmentClick(Attachment attachment) {
                Intent intent = new Intent(getActivity().getApplicationContext(), AttachmentViewerActivity.class);
                intent.putExtra(AttachmentViewerActivity.ATTACHMENT_ID, attachment.getId());
                intent.putExtra(AttachmentViewerActivity.EDITABLE, false);
                startActivity(intent);
            }
        });

		recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);

		RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
		recyclerView.setLayoutManager(mLayoutManager);
		recyclerView.setItemAnimator(new DefaultItemAnimator());
		recyclerView.addItemDecoration(new ObservationItemDecorator());

		adapter = new ObservationListAdapter(getActivity(), attachmentGallery, this);

		sp = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();

		observationRefreshReceiver.register();

		try {
			oDao = DaoStore.getInstance(getActivity().getApplicationContext()).getObservationDao();
			query = buildQuery(oDao, getTimeFilterId());
			Cursor cusor = obtainCursor(query, oDao);
			adapter.setCursor(cusor, query);

			recyclerView.setAdapter(adapter);

			if (listState != null) {
				recyclerView.getLayoutManager().onRestoreInstanceState(listState);
			}

			ObservationHelper.getInstance(getActivity().getApplicationContext()).addListener(this);
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem getting cursor or setting adapter.", e);
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		observationRefreshReceiver.unregister();

		listState = recyclerView.getLayoutManager().onSaveInstanceState();

		ObservationHelper.getInstance(getActivity().getApplicationContext()).removeListener(this);

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
				Intent intent = new Intent(getActivity(), ObservationFilterActivity.class);
				startActivity(intent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					onNewObservation();
				}
				break;
			}
		}
	}

	private void refreshObservations() {
		Intent intent = new Intent(getContext(), ObservationRefreshIntent.class);
		getActivity().startService(intent);
	}

	private void onNewObservation() {
		ObservationLocation location = null;

		// if there is not a location from the location service, then try to pull one from the database.
		if (locationService.getLocation() == null) {
			List<mil.nga.giat.mage.sdk.datastore.location.Location> tLocations = LocationHelper.getInstance(getActivity().getApplicationContext()).getCurrentUserLocations(1, true);
			if (!tLocations.isEmpty()) {
				mil.nga.giat.mage.sdk.datastore.location.Location tLocation = tLocations.get(0);
				Geometry geo = tLocation.getGeometry();
				Map<String, LocationProperty> propertiesMap = tLocation.getPropertiesMap();
				String provider = ObservationLocation.MANUAL_PROVIDER;
				if (propertiesMap.get("provider").getValue() != null) {
					provider = propertiesMap.get("provider").getValue().toString();
				}
				location = new ObservationLocation(provider, geo);
				location.setTime(tLocation.getTimestamp().getTime());
				if (propertiesMap.get("accuracy").getValue() != null) {
					location.setAccuracy(Float.valueOf(propertiesMap.get("accuracy").getValue().toString()));
				}
			}
		} else {
			location = new ObservationLocation(locationService.getLocation());
		}

		if(!UserHelper.getInstance(getActivity().getApplicationContext()).isCurrentUserPartOfCurrentEvent()) {
			new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle)
					.setTitle(getActivity().getResources().getString(R.string.location_no_event_title))
					.setMessage(getActivity().getResources().getString(R.string.location_no_event_message))
					.setPositiveButton(android.R.string.ok, null)
					.show();
		} else if(location != null) {
			Intent intent;

			// show form picker or go to
			JsonArray formDefinitions = EventHelper.getInstance(getActivity()).getCurrentEvent().getNonArchivedForms();
			if (formDefinitions.size() == 0) {
				intent = new Intent(getActivity(), ObservationEditActivity.class);
			} else if (formDefinitions.size() == 1) {
				JsonObject form = (JsonObject) formDefinitions.iterator().next();
				intent = new Intent(getActivity(), ObservationEditActivity.class);
				intent.putExtra(ObservationEditActivity.OBSERVATION_FORM_ID, form.get("id").getAsLong());
			} else {
				intent = new Intent(getActivity(), ObservationFormPickerActivity.class);
			}

			intent.putExtra(ObservationEditActivity.LOCATION, location);
			startActivity(intent);

		} else {
			if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle)
						.setTitle(getActivity().getResources().getString(R.string.location_missing_title))
						.setMessage(getActivity().getResources().getString(R.string.location_missing_message))
						.setPositiveButton(android.R.string.ok, null)
						.show();
			} else {
				new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle)
						.setTitle(getActivity().getResources().getString(R.string.location_access_observation_title))
						.setMessage(getActivity().getResources().getString(R.string.location_access_observation_message))
						.setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
							}
						})
						.show();
			}
		}
	}

	private int getCustomTimeNumber() {
		return sp.getInt(getResources().getString(R.string.customObservationTimeNumberFilterKey), 0);
	}

	private String getCustomTimeUnit() {
		return sp.getString(getResources().getString(R.string.customObservationTimeUnitFilterKey), getResources().getStringArray(R.array.timeUnitEntries)[0]);
	}

	private PreparedQuery<Observation> buildQuery(Dao<Observation, Long> oDao, int filterId) throws SQLException {
		QueryBuilder<Observation, Long> qb = oDao.queryBuilder();
		Calendar c = Calendar.getInstance();
		List<String> filters = new ArrayList<>();
		String footerText = "All observations have been returned";

		if (filterId == getResources().getInteger(R.integer.time_filter_last_month)) {
			filters.add("Last Month");
			footerText = "End of results for Last Month filter";
			c.add(Calendar.MONTH, -1);
		} else if (filterId == getResources().getInteger(R.integer.time_filter_last_week)) {
			filters.add("Last Week");
			footerText = "End of results for Last Week filter";
			c.add(Calendar.DAY_OF_MONTH, -7);
		} else if (filterId == getResources().getInteger(R.integer.time_filter_last_24_hours)) {
			filters.add("Last 24 Hours");
			footerText = "End of results for Last 24 Hours filter";
			c.add(Calendar.HOUR, -24);
		} else if (filterId == getResources().getInteger(R.integer.time_filter_today)) {
			filters.add("Since Midnight");
			footerText = "End of results for Today filter";
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
		}  else if (filterId == getResources().getInteger(R.integer.time_filter_custom)) {
			String customFilterTimeUnit = getCustomTimeUnit();
			int customTimeNumber = getCustomTimeNumber();

			filters.add("Last " + customTimeNumber + " " + customFilterTimeUnit);
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

		requeryTime = c.getTimeInMillis();
		adapter.setFooterText(footerText);
		qb.where()
			.ne("state", State.ARCHIVE)
			.and()
			.ge("timestamp", c.getTime())
			.and()
			.eq("event_id", EventHelper.getInstance(getActivity().getApplicationContext()).getCurrentEvent().getId());

		List<String> actionFilters = new ArrayList<>();

		boolean favorites = sp.getBoolean(getResources().getString(R.string.activeFavoritesFilterKey), false);
		if (favorites && currentUser != null) {
			Dao<ObservationFavorite, Long> observationFavoriteDao = DaoStore.getInstance(getActivity().getApplicationContext()).getObservationFavoriteDao();
			QueryBuilder<ObservationFavorite, Long> favoriteQb = observationFavoriteDao.queryBuilder();
			favoriteQb.where()
				.eq("user_id", currentUser.getRemoteId())
				.and()
				.eq("is_favorite", true);

			qb.join(favoriteQb);

			actionFilters.add("Favorites");
		}

		boolean important = sp.getBoolean(getResources().getString(R.string.activeImportantFilterKey), false);
		if (important) {
			Dao<ObservationImportant, Long> observationImportantDao = DaoStore.getInstance(getActivity().getApplicationContext()).getObservationImportantDao();
			QueryBuilder<ObservationImportant, Long> importantQb = observationImportantDao.queryBuilder();
			importantQb.where().eq("is_important", true);

			qb.join(importantQb);

			actionFilters.add("Important");
		}

		qb.orderBy("timestamp", false);

		if (!actionFilters.isEmpty()) {
			filters.add(StringUtils.join(actionFilters, " & "));
		}

		((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(StringUtils.join(filters, ", "));

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
			Log.i(LOG_NAME, "last modified is: " + c.getLong(c.getColumnIndex("last_modified")));
			Log.i(LOG_NAME, "querying again in: " + (oldestTime - requeryTime)/60000 + " minutes");
			if (queryUpdateHandle != null) {
				queryUpdateHandle.cancel(true);
			}
			queryUpdateHandle = scheduler.schedule(new Runnable() {
				public void run() {
					updateFilter();
				}
			}, oldestTime - requeryTime, TimeUnit.MILLISECONDS);
			c.moveToFirst();
		}
		return c;
	}
	
	private int getTimeFilterId() {
		return sp.getInt(getResources().getString(R.string.activeTimeFilterKey), getResources().getInteger(R.integer.time_filter_none));
	}

	@Override
	public void onError(Throwable error) {
	}

	@Override
	public void onObservationCreated(final Collection<Observation> observations, Boolean sendUserNotifcations) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					query = buildQuery(oDao, getTimeFilterId());
					adapter.setCursor(obtainCursor(query, oDao), query);
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
					adapter.setCursor(obtainCursor(query, oDao), query);
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
					adapter.setCursor(obtainCursor(query, oDao), query);
				} catch (Exception e) {
					Log.e(LOG_NAME, "Unable to change cursor", e);
				}
			}
		});
	}

	private void updateFilter() {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					query = buildQuery(oDao, getTimeFilterId());
					adapter.setCursor(obtainCursor(query, oDao), query);
				} catch (Exception e) {
					Log.e(LOG_NAME, "Unable to change cursor", e);
				}
			}
		});
	}

	@Override
	public void onObservationDirections(Observation observation) {
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW, observation.getGoogleMapsUri());
		startActivity(intent);
	}

	@Override
	public void onObservationClick(Observation observation) {
		Intent observationView = new Intent(getActivity().getApplicationContext(), ObservationViewActivity.class);
		observationView.putExtra(ObservationViewActivity.OBSERVATION_ID, observation.getId());
		getActivity().startActivity(observationView);
	}

	public class ObservationRefreshReceiver extends BroadcastReceiver {
		public void register() {
			IntentFilter filter = new IntentFilter(ObservationRefreshIntent.ACTION_OBSERVATIONS_REFRESHED);
			filter.addCategory(Intent.CATEGORY_DEFAULT);
			getContext().registerReceiver(observationRefreshReceiver, filter);
		}

		public void unregister() {
			getContext().unregisterReceiver(observationRefreshReceiver);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			swipeContainer.setRefreshing(false);

			String status = intent.getExtras().getString(ObservationRefreshIntent.EXTRA_OBSERVATIONS_REFRESH_STATUS, null);
			if (status != null) {
				final Snackbar snackbar = Snackbar
					.make(coordinatorLayout, status, Snackbar.LENGTH_LONG)
					.setAction("RETRY", new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							refreshObservations();
						}
					});

				snackbar.show();
			}
		}
	}

	private class ObservationItemDecorator extends RecyclerView.ItemDecoration {
		private Drawable divider;

		public ObservationItemDecorator() {
			divider = ContextCompat.getDrawable(getActivity(), R.drawable.people_feed_divider);
		}

		@Override
		@SuppressLint("NewApi")
		public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
			c.save();
			final int left;
			final int right;
			if (parent.getClipToPadding()) {
				left = parent.getPaddingLeft();
				right = parent.getWidth() - parent.getPaddingRight();
				c.clipRect(left, parent.getPaddingTop(), right, parent.getHeight() - parent.getPaddingBottom());
			} else {
				left = 0;
				right = parent.getWidth();
			}

			final int childCount = parent.getChildCount();
			Rect outBounds = new Rect();

			// Go to childCount - 1 to skip drawing a divider after the last view
			for (int i = 0; i < childCount - 1; i++) {
				final View child = parent.getChildAt(i);
				parent.getDecoratedBoundsWithMargins(child, outBounds);
				final int bottom = outBounds.bottom + Math.round(ViewCompat.getTranslationY(child));
				final int top = bottom - divider.getIntrinsicHeight();
				divider.setBounds(left, top, right, bottom);
				divider.draw(c);
			}
			c.restore();
		}
	}
}