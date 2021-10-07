package mil.nga.giat.mage.event;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.network.Resource;
import mil.nga.giat.mage.observation.sync.ObservationServerFetch;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.login.RecentEventTask;

/**
 * Allows the user to switch events within the app
 */

@AndroidEntryPoint
public class ChangeEventActivity extends AppCompatActivity {

	private static final String LOG_NAME = ChangeEventActivity.class.getName();

	public static String EVENT_ID_EXTRA = "EVENT_ID_EXTRA";

	private List<Event> events = new ArrayList<>();
	private EventListAdapter eventListAdapter;

	private EventViewModel viewModel;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.fragment_events);

		Toolbar toolbar = findViewById(R.id.toolbar);
		toolbar.setTitle("Events");
		setSupportActionBar(toolbar);
		getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		EventHelper eventHelper = EventHelper.getInstance(this);

		List<Event> recentEvents = Collections.emptyList();
		try {
			events = eventHelper.readAll();
			recentEvents = eventHelper.getRecentEvents();
		} catch (Exception e) {
			Log.e(LOG_NAME, "Could not get current events!");
		}

		RecyclerView recyclerView = findViewById(R.id.recycler_view);

		eventListAdapter = new EventListAdapter(events, recentEvents, event -> chooseEvent(event));

		RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
		recyclerView.setLayoutManager(mLayoutManager);
		recyclerView.setItemAnimator(new DefaultItemAnimator());
		recyclerView.addItemDecoration(new EventItemDecorator(getApplicationContext()));
		recyclerView.setAdapter(eventListAdapter);

		SearchView searchView = findViewById(R.id.search_view);
		searchView.setIconified(false);
		searchView.setIconifiedByDefault(false);
		searchView.clearFocus();
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				return false;
			}

			@Override
			public boolean onQueryTextChange(String text) {
				onSearchTextChanged(text);
				return true;
			}
		});

		viewModel = new ViewModelProvider(this).get(EventViewModel.class);
		viewModel.getSyncStatus().observe(this, resource -> finishEvent(resource));

		try {
			Long eventId = getIntent().getLongExtra(EVENT_ID_EXTRA, -1);
			Event event = eventHelper.read(eventId);

			if (event != null) {
				chooseEvent(event);
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, "Could not read event", e);
		}
	}

	private void onSearchTextChanged(String text) {
		eventListAdapter.filter(text);
	}

	public void chooseEvent(final Event event) {
		findViewById(R.id.app_bar).setVisibility(View.GONE);
		findViewById(R.id.event_content).setVisibility(View.GONE);
		findViewById(R.id.event_status).setVisibility(View.VISIBLE);

		TextView message = findViewById(R.id.event_message);
		message.setText("Loading " + event.getName());

		viewModel.syncEvent(event);
	}

	private void finishEvent(Resource<? extends Event> resource) {
		// Send chosen event to the server
		Event event = resource.getData();
		List<String> userRecentEventInfo = new ArrayList<>();
		userRecentEventInfo.add(event.getRemoteId());
		new RecentEventTask(getApplicationContext(), status -> {
			// no-op, don't care if server didn't get event selection
		}).execute(userRecentEventInfo.toArray(new String[userRecentEventInfo.size()]));

		try {
			UserHelper userHelper = UserHelper.getInstance(getApplicationContext());
			User user = userHelper.readCurrentUser();
			userHelper.setCurrentEvent(user, event);
		} catch (UserException e) {
			Log.e(LOG_NAME, "Could not set current event.", e);
		}

		// disable pushing locations
		if (!UserHelper.getInstance(getApplicationContext()).isCurrentUserPartOfEvent(event)) {
			SharedPreferences.Editor sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
			sp.putBoolean(getString(R.string.reportLocationKey), false).apply();
		}

		AsyncTask.execute(() -> new ObservationServerFetch(getApplicationContext()).fetch(false));

		setResult(RESULT_OK);
		finish();
	}
}