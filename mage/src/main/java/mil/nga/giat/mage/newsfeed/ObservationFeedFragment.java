package mil.nga.giat.mage.newsfeed;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
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

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.dagger.module.ApplicationContext;
import mil.nga.giat.mage.filter.ObservationFilterActivity;
import mil.nga.giat.mage.location.LocationPolicy;
import mil.nga.giat.mage.observation.AttachmentGallery;
import mil.nga.giat.mage.observation.AttachmentViewerActivity;
import mil.nga.giat.mage.observation.ImportantDialog;
import mil.nga.giat.mage.observation.ImportantRemoveDialog;
import mil.nga.giat.mage.observation.ObservationEditActivity;
import mil.nga.giat.mage.observation.ObservationFormPickerActivity;
import mil.nga.giat.mage.observation.ObservationLocation;
import mil.nga.giat.mage.observation.ObservationViewActivity;
import mil.nga.giat.mage.observation.sync.ObservationServerFetch;
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
import mil.nga.giat.mage.sdk.exceptions.ObservationException;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.sf.Geometry;

public class ObservationFeedFragment extends DaggerFragment implements IObservationEventListener, ObservationListAdapter.ObservationActionListener, Observer<Location> {

	private static final String LOG_NAME = ObservationFeedFragment.class.getName();

	private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

	private PreparedQuery<Observation> query;
	private Dao<Observation, Long> oDao;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> queryUpdateHandle;
	private long requeryTime;
	private RecyclerView recyclerView;
	private ObservationListAdapter adapter;

	Parcelable listState;
	private User currentUser;
	private SwipeRefreshLayout swipeContainer;

	@Inject
	protected @ApplicationContext Context context;

	@Inject
	protected SharedPreferences preferences;

	@Inject
	protected LocationPolicy locationPolicy;
	private LiveData<Location> locationProvider;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			currentUser = UserHelper.getInstance(context).readCurrentUser();
		} catch (UserException e) {
			Log.e(LOG_NAME, "Error reading current user", e);
		}

		locationProvider = locationPolicy.getBestLocationProvider();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_news_feed, container, false);
		setHasOptionsMenu(true);

		swipeContainer = rootView.findViewById(R.id.swipeContainer);
		swipeContainer.setColorSchemeResources(R.color.md_blue_600, R.color.md_orange_A200);
		swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				new ObservationRefreshTask().execute();
			}
		});

		rootView.findViewById(R.id.new_observation_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onNewObservation();
			}
		});

		AttachmentGallery attachmentGallery = new AttachmentGallery(getContext(), 200, 200);
        attachmentGallery.addOnAttachmentClickListener(new AttachmentGallery.OnAttachmentClickListener() {
            @Override
            public void onAttachmentClick(Attachment attachment) {
                Intent intent = new Intent(context, AttachmentViewerActivity.class);
                intent.putExtra(AttachmentViewerActivity.ATTACHMENT_ID, attachment.getId());
                intent.putExtra(AttachmentViewerActivity.EDITABLE, false);
                startActivity(intent);
            }
        });

		recyclerView = rootView.findViewById(R.id.recycler_view);

		RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
		recyclerView.setLayoutManager(mLayoutManager);
		recyclerView.setItemAnimator(new DefaultItemAnimator());

		adapter = new ObservationListAdapter(getActivity(), attachmentGallery, this);

		return rootView;
	}

	@Override
	public void onStart() {
		super.onStart();

		locationProvider.observe(this, this);
	}

	@Override
	public void onStop() {
		super.onStop();

		locationProvider.removeObserver(this);
	}

	@Override
	public void onResume() {
		super.onResume();

		try {
			oDao = DaoStore.getInstance(context).getObservationDao();
			query = buildQuery(oDao, getTimeFilterId());
			Cursor cusor = obtainCursor(query, oDao);
			adapter.setCursor(cusor, query);

			recyclerView.setAdapter(adapter);

			if (listState != null) {
				recyclerView.getLayoutManager().onRestoreInstanceState(listState);
			}

			ObservationHelper.getInstance(context).addListener(this);
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem getting cursor or setting adapter.", e);
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		listState = recyclerView.getLayoutManager().onSaveInstanceState();
		recyclerView.setAdapter(null);

		ObservationHelper.getInstance(context).removeListener(this);

		if (queryUpdateHandle != null) {
			queryUpdateHandle.cancel(true);
		}

		adapter.closeCursor();
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
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
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

	private void onNewObservation() {
		ObservationLocation location = getLocation();

		if(!UserHelper.getInstance(context).isCurrentUserPartOfCurrentEvent()) {
			new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle)
					.setTitle(getActivity().getResources().getString(R.string.location_no_event_title))
					.setMessage(getActivity().getResources().getString(R.string.location_no_event_message))
					.setPositiveButton(android.R.string.ok, null)
					.show();
		} else if (location != null) {
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
			if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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

	private ObservationLocation getLocation() {
		ObservationLocation location = null;

		// if there is not a location from the location service, then try to pull one from the database.
		if (locationProvider.getValue() == null) {
			List<mil.nga.giat.mage.sdk.datastore.location.Location> locations = LocationHelper.getInstance(context).getCurrentUserLocations(1, true);
			if (!locations.isEmpty()) {
				mil.nga.giat.mage.sdk.datastore.location.Location tLocation = locations.get(0);
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
			location = new ObservationLocation(locationProvider.getValue());
		}

		return location;
	}

	private int getCustomTimeNumber() {
		return preferences.getInt(getResources().getString(R.string.customObservationTimeNumberFilterKey), 0);
	}

	private String getCustomTimeUnit() {
		return preferences.getString(getResources().getString(R.string.customObservationTimeUnitFilterKey), getResources().getStringArray(R.array.timeUnitEntries)[0]);
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
			.eq("event_id", EventHelper.getInstance(context).getCurrentEvent().getId());

		List<String> actionFilters = new ArrayList<>();

		boolean favorites = preferences.getBoolean(getResources().getString(R.string.activeFavoritesFilterKey), false);
		if (favorites && currentUser != null) {
			Dao<ObservationFavorite, Long> observationFavoriteDao = DaoStore.getInstance(context).getObservationFavoriteDao();
			QueryBuilder<ObservationFavorite, Long> favoriteQb = observationFavoriteDao.queryBuilder();
			favoriteQb.where()
				.eq("user_id", currentUser.getRemoteId())
				.and()
				.eq("is_favorite", true);

			qb.join(favoriteQb);

			actionFilters.add("Favorites");
		}

		boolean important = preferences.getBoolean(getResources().getString(R.string.activeImportantFilterKey), false);
		if (important) {
			Dao<ObservationImportant, Long> observationImportantDao = DaoStore.getInstance(context).getObservationImportantDao();
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
		return preferences.getInt(getResources().getString(R.string.activeTimeFilterKey), getResources().getInteger(R.integer.time_filter_none));
	}

	@Override
	public void onError(Throwable error) {
	}

	@Override
	public void onObservationCreated(final Collection<Observation> observations, Boolean sendUserNotifcations) {
		getActivity().runOnUiThread(() -> {
			try {
				query = buildQuery(oDao, getTimeFilterId());
				adapter.setCursor(obtainCursor(query, oDao), query);
			} catch (Exception e) {
				Log.e(LOG_NAME, "Unable to change cursor", e);
			}
		});

	}

	@Override
	public void onObservationDeleted(final Observation observation) {
		getActivity().runOnUiThread(() -> {
			try {
				query = buildQuery(oDao, getTimeFilterId());
				adapter.setCursor(obtainCursor(query, oDao), query);
			} catch (Exception e) {
				Log.e(LOG_NAME, "Unable to change cursor", e);
			}
		});
	}

	@Override
	public void onObservationUpdated(final Observation observation) {
		getActivity().runOnUiThread(() -> {
			try {
				query = buildQuery(oDao, getTimeFilterId());
				adapter.setCursor(obtainCursor(query, oDao), query);
			} catch (Exception e) {
				Log.e(LOG_NAME, "Unable to change cursor", e);
			}
		});
	}

	private void updateFilter() {
		getActivity().runOnUiThread(() -> {
			try {
				query = buildQuery(oDao, getTimeFilterId());
				adapter.setCursor(obtainCursor(query, oDao), query);
			} catch (Exception e) {
				Log.e(LOG_NAME, "Unable to change cursor", e);
			}
		});
	}

	public void onObservationImportant(Observation observation) {
		ObservationImportant important = observation.getImportant();
		boolean isImportant = important != null && important.isImportant();
		if (isImportant) {
			BottomSheetDialog dialog = new BottomSheetDialog(requireActivity());
			View view = getLayoutInflater().inflate(R.layout.view_important_bottom_sheet, null);
			view.findViewById(R.id.update_button).setOnClickListener(v -> {
				onUpdateImportantClick(observation);
				dialog.dismiss();
			});
			view.findViewById(R.id.remove_button).setOnClickListener(v -> {
				onRemoveImportantClick(observation);
				dialog.dismiss();
			});
			dialog.setContentView(view);
			dialog.show();
		} else {
			onUpdateImportantClick(observation);
		}
	}

	public void onUpdateImportantClick(Observation observation) {
		ImportantDialog dialog = ImportantDialog.newInstance(observation.getImportant());
		dialog.setOnImportantListener(description -> {
			ObservationHelper observationHelper = ObservationHelper.getInstance(context);
			try {
				ObservationImportant important = observation.getImportant();
				if (important == null) {
					important = new ObservationImportant();
					observation.setImportant(important);
				}

				if (currentUser != null) {
					important.setUserId(currentUser.getRemoteId());
				}

				important.setTimestamp(new Date());
				important.setDescription(description);
				observationHelper.addImportant(observation);
			} catch (ObservationException e) {
				Log.e(LOG_NAME, "Error updating important flag for observation: " + observation.getRemoteId());
			}
		});

		dialog.show(getFragmentManager(), "important");
	}


	public void onRemoveImportantClick(Observation o) {
		ImportantRemoveDialog dialog = new ImportantRemoveDialog();
		dialog.setOnRemoveImportantListener(() -> {
			ObservationHelper observationHelper = ObservationHelper.getInstance(context);
			try {
				observationHelper.removeImportant(o);
			} catch (ObservationException e) {
				Log.e(LOG_NAME, "Error removing important flag for observation: " + o.getRemoteId());
			}
		});

		dialog.show(getFragmentManager(), "remove_important");
	}

	@Override
	public void onObservationDirections(Observation observation) {
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW, observation.getGoogleMapsUri());
		startActivity(intent);
	}

	@Override
	public void onObservationClick(Observation observation) {
		Intent observationView = new Intent(context, ObservationViewActivity.class);
		observationView.putExtra(ObservationViewActivity.OBSERVATION_ID, observation.getId());
		startActivity(observationView);
	}

	@Override
	public void onChanged(@Nullable Location location) {

	}

	private class ObservationRefreshTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... voids) {
			new ObservationServerFetch(context).fetch(false);
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			swipeContainer.setRefreshing(false);
		}
	}
}