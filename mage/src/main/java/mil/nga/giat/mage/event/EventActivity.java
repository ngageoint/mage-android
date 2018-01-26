package mil.nga.giat.mage.event;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatRadioButton;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.Window;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.LandingActivity;
import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.login.LoginActivity;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.RoleHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.EventException;
import mil.nga.giat.mage.sdk.fetch.EventIconFetchIntentService;
import mil.nga.giat.mage.sdk.fetch.InitialFetchIntentService;
import mil.nga.giat.mage.sdk.login.AccountDelegate;
import mil.nga.giat.mage.sdk.login.AccountStatus;
import mil.nga.giat.mage.sdk.login.RecentEventTask;

public class EventActivity extends AppCompatActivity {

	private static final String STATE_EVENT = "stateEvent";

	private static final String LOG_NAME = EventActivity.class.getName();

    private static final int uniqueChildStartingIdIndex = 10000;

    private int uniqueChildIdIndex = uniqueChildStartingIdIndex;

    private List<Event> events = new ArrayList<>();

    private Event chosenEvent = null;
	EventIconsBroadcastReceiver iconsBroadcastReceiver = new EventIconsBroadcastReceiver();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_event);

		findViewById(R.id.event_content).setVisibility(View.GONE);
		findViewById(R.id.event_status).setVisibility(View.VISIBLE);

		uniqueChildIdIndex = uniqueChildStartingIdIndex;
		if(savedInstanceState == null) {
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

		BroadcastReceiver receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(this);

				EventHelper eventHelper = EventHelper.getInstance(getApplicationContext());

				// status?
				if (intent.getBooleanExtra("status", false)) {
					User currentUser = null;
					try {
						currentUser = UserHelper.getInstance(getApplicationContext()).readCurrentUser();
						if (currentUser.getRole().equals(RoleHelper.getInstance(getApplicationContext()).readAdmin())) {
							// now that ADMINS can be part of any event, make sure they don't push data to events they are not part of!!
							events = eventHelper.readAll();
						} else {
							events = eventHelper.getEventsForCurrentUser();
						}
					} catch(Exception e) {
						Log.e(LOG_NAME, "Could not get current events!");
					}

					if (events.isEmpty() || currentUser == null) {
						Log.e(LOG_NAME, "User is part of no events!");

						((MAGE) getApplication()).onLogout(true, null);
						findViewById(R.id.event_status).setVisibility(View.GONE);
						findViewById(R.id.event_content).setVisibility(View.VISIBLE);
						findViewById(R.id.event_continue_button).setVisibility(View.GONE);
						findViewById(R.id.event_select_content).setVisibility(View.GONE);
						findViewById(R.id.event_back_button).setVisibility(View.VISIBLE);
						findViewById(R.id.event_bummer_info).setVisibility(View.VISIBLE);
						findViewById(R.id.event_serverproblem_info).setVisibility(View.GONE);
					} else {
						Event recentEvent = null;
						try {
							recentEvent = eventHelper.getRecentEvent();
						} catch (EventException e) {
							Log.e(LOG_NAME, "Error getting recent event", e);
						}

						if (recentEvent == null) {
							recentEvent = events.get(0);
						}

						if (events.size() == 1 && events.get(0).equals(recentEvent)) {
							chooseEvent(recentEvent);
						} else {
							List<Event> tempEventsForCurrentUser = EventHelper.getInstance(getApplicationContext()).getEventsForCurrentUser();
							((RadioGroup)findViewById(R.id.event_radiogroup)).removeAllViews();
							for (Event e : events) {
								ContextThemeWrapper wrapper = new ContextThemeWrapper(EventActivity.this, R.style.AppTheme_Radio);
								AppCompatRadioButton radioButton = new AppCompatRadioButton(wrapper);
								TextViewCompat.setTextAppearance(radioButton, R.style.AppTheme_Radio);
								radioButton.setId(uniqueChildIdIndex++);
								String text = e.getName();
								if(!tempEventsForCurrentUser.contains(e)) {
									text += " (read-only access)";
								}
								radioButton.setText(text);

								if (recentEvent.getRemoteId().equals(e.getRemoteId())) {
									radioButton.setChecked(true);
								}

								((RadioGroup)findViewById(R.id.event_radiogroup)).addView(radioButton);
							}
							findViewById(R.id.event_status).setVisibility(View.GONE);
							findViewById(R.id.event_content).setVisibility(View.VISIBLE);
						}
					}
				} else {
					Log.e(LOG_NAME, "User is part of no event!");

					((MAGE) getApplication()).onLogout(true, null);
					findViewById(R.id.event_status).setVisibility(View.GONE);
					findViewById(R.id.event_content).setVisibility(View.VISIBLE);
					findViewById(R.id.event_continue_button).setVisibility(View.GONE);
					findViewById(R.id.event_select_content).setVisibility(View.GONE);
					findViewById(R.id.event_back_button).setVisibility(View.VISIBLE);
					findViewById(R.id.event_bummer_info).setVisibility(View.GONE);
					findViewById(R.id.event_serverproblem_info).setVisibility(View.VISIBLE);
				}
			}
		};

		// receive response from event fetch
		IntentFilter statusIntentFilter = new IntentFilter(InitialFetchIntentService.InitialFetchIntentServiceAction);
		statusIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
		LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, statusIntentFilter);
		getApplicationContext().startService(new Intent(getApplicationContext(), InitialFetchIntentService.class));
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

	public void onChooseEvent(View view) {
		int eventIndex = (((RadioGroup)findViewById(R.id.event_radiogroup)).getCheckedRadioButtonId() - uniqueChildStartingIdIndex);
		chooseEvent(events.get(eventIndex));
	}

	public void chooseEvent(Event event) {
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

		// Listen for event observation icons completion
		IntentFilter iconsBroadcastFilter = new IntentFilter(EventIconFetchIntentService.BROADCAST_EVENT_ICONS_ACTION);
		iconsBroadcastFilter.addCategory(Intent.CATEGORY_DEFAULT);
		LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(iconsBroadcastReceiver, iconsBroadcastFilter);

		// Get event observation icons
		Intent iconServiceIntent = new Intent(getApplicationContext(), EventIconFetchIntentService.class);
		iconServiceIntent.putExtra(EventIconFetchIntentService.EXTRA_EVENT_ID, chosenEvent.getId());
		startService(iconServiceIntent);
    }

    public void bummerEvent(View view) {
        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        finish();
    }

    private class EventIconsBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(this);

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

			// fetch all other events icons
			List<Long> eventIds = new ArrayList<>();
			for (Event event : events) {
				// Already loaded chosen event icons
				if (!event.getId().equals(chosenEvent.getId())) {
					eventIds.add(event.getId());
				}
			}
			Intent iconServiceIntent = new Intent(getApplicationContext(), EventIconFetchIntentService.class);
			iconServiceIntent.putExtra(EventIconFetchIntentService.EXTRA_EVENT_IDS, ArrayUtils.toPrimitive(eventIds.toArray(new Long[eventIds.size()])));
			startService(iconServiceIntent);

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
}
