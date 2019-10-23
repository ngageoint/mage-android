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
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.observation.sync.ObservationServerFetch;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.login.AccountDelegate;
import mil.nga.giat.mage.sdk.login.AccountStatus;
import mil.nga.giat.mage.sdk.login.RecentEventTask;

/**
 * Allows the user to switch events within the app
 */

public class ChangeEventActivity extends AppCompatActivity {

	private static final String LOG_NAME = ChangeEventActivity.class.getName();

	public static String EVENT_ID_EXTRA = "EVENT_ID_EXTRA";

	private List<Event> events = new ArrayList<>();
	private EventListAdapter eventListAdapter;

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

		eventListAdapter = new EventListAdapter(events, recentEvents, new EventListAdapter.OnEventClickListener() {
			@Override
			public void onEventClick(Event event) {
				chooseEvent(event);
			}
		});

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

		EventServerFetch eventFetch = new EventServerFetch(getApplicationContext(), event.getRemoteId());
		eventFetch.setEventFetchListener(new EventServerFetch.EventFetchListener() {
			@Override
			public void onEventFetched(boolean status, Exception e) {
				finishEvent(event);
			}
		});
		eventFetch.execute();
	}

	private void finishEvent(final Event event) {
		// Send chosen event to the server
		List<String> userRecentEventInfo = new ArrayList<>();
		userRecentEventInfo.add(event.getRemoteId());
		new RecentEventTask(new AccountDelegate() {
			@Override
			public void finishAccount(AccountStatus accountStatus) {
				// no-op, don't care if server didn't get event selection
			}
		}, getApplicationContext()).execute(userRecentEventInfo.toArray(new String[userRecentEventInfo.size()]));

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


		AsyncTask.execute(new Runnable() {
			@Override
			public void run() {
				new ObservationServerFetch(getApplicationContext()).fetch(false);
			}
		});

		setResult(RESULT_OK);
		finish();
	}
}