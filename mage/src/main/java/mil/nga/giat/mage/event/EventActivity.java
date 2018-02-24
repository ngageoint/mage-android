package mil.nga.giat.mage.event;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Window;
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
import mil.nga.giat.mage.sdk.fetch.EventsServerFetch;
import mil.nga.giat.mage.sdk.login.AccountDelegate;
import mil.nga.giat.mage.sdk.login.AccountStatus;
import mil.nga.giat.mage.sdk.login.RecentEventTask;

public class EventActivity extends AppCompatActivity {

	private static final String STATE_EVENT = "stateEvent";

	private static final String LOG_NAME = EventActivity.class.getName();

    private List<Event> events = Collections.EMPTY_LIST;
	private RecyclerView recyclerView;
	private EventListAdapter eventListAdapter;

    private Event chosenEvent = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);



		setContentView(R.layout.activity_event);

		findViewById(R.id.event_content).setVisibility(View.GONE);
		findViewById(R.id.event_status).setVisibility(View.VISIBLE);

		if (savedInstanceState == null) {
			events = new ArrayList<>();
		} else {
			long[] eventIds = savedInstanceState.getLongArray(STATE_EVENT);
			try {
				for (long eventId : eventIds) {
					events.add(EventHelper.getInstance(getApplicationContext()).read(eventId));
				}
			} catch(Exception e) {
				Log.e(LOG_NAME, "Could not hydrate events!");
			}
		}

		recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

		EventsServerFetch eventsServerFetch = new EventsServerFetch(getApplicationContext());
		eventsServerFetch.setEventFetchListener(new EventsServerFetch.EventsFetchListener() {
			@Override
			public void onEventsFetched(boolean status, Exception error) {
				if (status) {
					eventsFetched();
				} else {
					onEventsFetchError();
				}
			}
		});
		eventsServerFetch.execute();
	}

	private void eventsFetched() {
		EventHelper eventHelper = EventHelper.getInstance(getApplicationContext());

		List<Event> recentEvents = Collections.EMPTY_LIST;
		try {
			events = eventHelper.readAll();
			recentEvents = eventHelper.getRecentEvents();
		} catch(Exception e) {
			Log.e(LOG_NAME, "Could not get events!");
		}

		if (events.isEmpty()) {
			Log.e(LOG_NAME, "User is part of no events!");

			((MAGE) getApplication()).onLogout(true, null);
			findViewById(R.id.event_status).setVisibility(View.GONE);
			findViewById(R.id.event_select_content).setVisibility(View.GONE);
			findViewById(R.id.event_serverproblem_info).setVisibility(View.GONE);

			findViewById(R.id.event_content).setVisibility(View.VISIBLE);
			findViewById(R.id.event_back_button).setVisibility(View.VISIBLE);
			findViewById(R.id.event_bummer_info).setVisibility(View.VISIBLE);
		} else if (events.size() == 1) {
			chooseEvent(events.get(0));
		} else {
			eventListAdapter = new EventListAdapter(events, recentEvents, new EventListAdapter.OnEventClickListener() {
				@Override
				public void onEventClick(Event event) {
					chooseEvent(event);
				}
			});

			recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
			recyclerView.setItemAnimator(new DefaultItemAnimator());
			recyclerView.addItemDecoration(new EventItemDecorator(getApplicationContext(), recentEvents.size()));
			recyclerView.setAdapter(eventListAdapter);


			findViewById(R.id.event_status).setVisibility(View.GONE);
			findViewById(R.id.event_content).setVisibility(View.VISIBLE);
		}
	}

	private void onEventsFetchError() {
		Log.e(LOG_NAME, "User is part of no event!");

		((MAGE) getApplication()).onLogout(true, null);
		findViewById(R.id.event_status).setVisibility(View.GONE);
		findViewById(R.id.event_content).setVisibility(View.VISIBLE);
		findViewById(R.id.event_select_content).setVisibility(View.GONE);
		findViewById(R.id.event_back_button).setVisibility(View.VISIBLE);
		findViewById(R.id.event_bummer_info).setVisibility(View.GONE);
		findViewById(R.id.event_serverproblem_info).setVisibility(View.VISIBLE);
	}

	public void onSaveInstanceState(Bundle savedInstanceState) {

		long[] eventIds = new long[events.size()];

		for (int i = 0; i < events.size(); i++) {
			Event e = events.get(i);
			eventIds[i] = e.getId();
		}

		savedInstanceState.putLongArray(STATE_EVENT, eventIds);
		super.onSaveInstanceState(savedInstanceState);
	}

	public void bummerEvent(View view) {
		startActivity(new Intent(getApplicationContext(), LoginActivity.class));
		finish();
	}

	private void chooseEvent(Event event) {
		chosenEvent = event;

		findViewById(R.id.event_content).setVisibility(View.GONE);
		findViewById(R.id.event_status).setVisibility(View.VISIBLE);

        List<String> userRecentEventInfo = new ArrayList<>();
        userRecentEventInfo.add(chosenEvent.getRemoteId());

		TextView message = (TextView) findViewById(R.id.event_message);
		message.setText("Loading " + chosenEvent.getName());

		// Send chosen event to the server
		new RecentEventTask(new AccountDelegate() {
			@Override
			public void finishAccount(AccountStatus accountStatus) {
				// No need to check if this failed
			}
		}, getApplicationContext()).execute(userRecentEventInfo.toArray(new String[userRecentEventInfo.size()]));

		EventServerFetch eventFetch = new EventServerFetch(getApplicationContext(), chosenEvent.getRemoteId());
		eventFetch.setEventFetchListener(new EventServerFetch.EventFetchListener() {
			@Override
			public void onEventFetched(boolean status, Exception e) {
				finishEvent();
			}
		});
		eventFetch.execute();
    }

    private void finishEvent() {
		try {
			UserHelper userHelper = UserHelper.getInstance(getApplicationContext());
			User user = userHelper.readCurrentUser();
			userHelper.setCurrentEvent(user, chosenEvent);
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
