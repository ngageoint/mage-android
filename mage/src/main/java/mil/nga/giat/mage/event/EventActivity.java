package mil.nga.giat.mage.event;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mil.nga.giat.mage.LandingActivity;
import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.login.LoginActivity;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.fetch.EventServerFetch;
import mil.nga.giat.mage.sdk.login.AccountDelegate;
import mil.nga.giat.mage.sdk.login.AccountStatus;
import mil.nga.giat.mage.sdk.login.RecentEventTask;

public class EventActivity extends AppCompatActivity implements EventsFetchFragment.EventsFetchListener {

	private static final String LOG_NAME = EventActivity.class.getName();

	private static final String EVENTS_FETCH_FRAGMENT_TAG = "EVENTS_FETCH_FRAGMENT_TAG";
	EventsFetchFragment eventsFetchFragment;

    private List<Event> events = Collections.emptyList();
	private RecyclerView recyclerView;
	private SearchView searchView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_event);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		toolbar.setTitle("Welcome to MAGE");
		setSupportActionBar(toolbar);

		findViewById(R.id.event_status).setVisibility(View.VISIBLE);

		recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

		searchView = (SearchView) findViewById(R.id.search_view);
		searchView.setIconified(false);
		searchView.setIconifiedByDefault(false);
		searchView.clearFocus();

		FragmentManager fragmentManager = getSupportFragmentManager();
		eventsFetchFragment = (EventsFetchFragment) fragmentManager.findFragmentByTag(EVENTS_FETCH_FRAGMENT_TAG);

		// If the Fragment is non-null, then it is being retained over a configuration change.
		if (eventsFetchFragment == null) {
			eventsFetchFragment = new EventsFetchFragment();
			fragmentManager.beginTransaction().add(eventsFetchFragment, EVENTS_FETCH_FRAGMENT_TAG).commit();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		eventsFetchFragment.loadEvents();
	}

	@Override
	public void onEventsFetched(boolean status, Exception error) {
		if (status) {
			eventsFetched();
		} else {
			onEventsFetchError();
		}
	}

	private void eventsFetched() {
		EventHelper eventHelper = EventHelper.getInstance(getApplicationContext());

		List<Event> recentEvents = Collections.emptyList();
		try {
			events = eventHelper.readAll();
			recentEvents = eventHelper.getRecentEvents();
		} catch(Exception e) {
			Log.e(LOG_NAME, "Could not get events!");
		}

		if (events.isEmpty()) {
			Log.e(LOG_NAME, "User is part of no events!");

			((MAGE) getApplication()).onLogout(true, null);
			searchView.setVisibility(View.GONE);
			findViewById(R.id.event_status).setVisibility(View.GONE);
			findViewById(R.id.event_serverproblem_info).setVisibility(View.GONE);

			findViewById(R.id.event_back_button).setVisibility(View.VISIBLE);
			findViewById(R.id.event_bummer_info).setVisibility(View.VISIBLE);
		} else if (events.size() == 1) {
			chooseEvent(events.get(0));
		} else {
			final EventListAdapter eventListAdapter = new EventListAdapter(events, recentEvents, new EventListAdapter.OnEventClickListener() {
				@Override
				public void onEventClick(Event event) {
					chooseEvent(event);
				}
			});

			searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
				@Override
				public boolean onQueryTextSubmit(String query) {
					return false;
				}

				@Override
				public boolean onQueryTextChange(String text) {
					eventListAdapter.filter(text);
					return true;
				}
			});

			recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
			recyclerView.setItemAnimator(new DefaultItemAnimator());
			recyclerView.addItemDecoration(new EventItemDecorator(getApplicationContext()));
			recyclerView.setAdapter(eventListAdapter);

			findViewById(R.id.app_bar).setVisibility(View.VISIBLE);
			findViewById(R.id.event_status).setVisibility(View.GONE);
		}
	}

	private void onEventsFetchError() {
		Log.e(LOG_NAME, "User is part of no event!");

		((MAGE) getApplication()).onLogout(true, null);
		findViewById(R.id.event_status).setVisibility(View.GONE);
		findViewById(R.id.event_back_button).setVisibility(View.VISIBLE);
		findViewById(R.id.event_bummer_info).setVisibility(View.GONE);
		findViewById(R.id.event_serverproblem_info).setVisibility(View.VISIBLE);
	}

	public void bummerEvent(View view) {
		startActivity(new Intent(getApplicationContext(), LoginActivity.class));
		finish();
	}

	private void chooseEvent(final Event event) {
		findViewById(R.id.app_bar).setVisibility(View.GONE);
		findViewById(R.id.event_status).setVisibility(View.VISIBLE);

		TextView message = (TextView) findViewById(R.id.event_message);
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

    private void finishEvent(Event event) {
		// Send chosen event to the server
		List<String> userRecentEventInfo = new ArrayList<>();
		userRecentEventInfo.add(event.getRemoteId());
		new RecentEventTask(new AccountDelegate() {
			@Override
			public void finishAccount(AccountStatus accountStatus) {
				// No need to check if this failed
			}
		}, getApplicationContext()).execute(userRecentEventInfo.toArray(new String[userRecentEventInfo.size()]));

		try {
			UserHelper userHelper = UserHelper.getInstance(getApplicationContext());
			User user = userHelper.readCurrentUser();
			userHelper.setCurrentEvent(user, event);
		} catch(Exception e) {
			Log.e(LOG_NAME, "Could not set current event.");
		}

		// disable pushing locations
		if (!UserHelper.getInstance(getApplicationContext()).isCurrentUserPartOfCurrentEvent()) {
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
			editor.putBoolean(getString(R.string.reportLocationKey), false).apply();
		}

		// start up the landing activity!
		Intent launchIntent = new Intent(getApplicationContext(), LandingActivity.class);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			launchIntent.putExtras(extras);
		}

		startActivity(launchIntent);
		finish();
	}

}
