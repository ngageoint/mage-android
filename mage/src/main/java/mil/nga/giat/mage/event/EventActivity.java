package mil.nga.giat.mage.event;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.RadioButton;
import android.widget.RadioGroup;

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
import mil.nga.giat.mage.sdk.fetch.InitialFetchIntentService;
import mil.nga.giat.mage.sdk.login.AccountDelegate;
import mil.nga.giat.mage.sdk.login.AccountStatus;
import mil.nga.giat.mage.sdk.login.RecentEventTask;

public class EventActivity extends Activity implements AccountDelegate {

	public static final String EXTRA_CHOOSE_CURRENT_EVENT = "CHOOSE_CURRENT_EVENT";
	private static final String STATE_EVENT = "stateEvent";

	private static final String LOG_NAME = EventActivity.class.getName();

    private static final int uniqueChildStartingIdIndex = 10000;

    private int uniqueChildIdIndex = uniqueChildStartingIdIndex;

    private List<Event> events = new ArrayList<Event>();

    private Event chosenEvent = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_event);

		findViewById(R.id.event_content).setVisibility(View.GONE);
		findViewById(R.id.event_status).setVisibility(View.VISIBLE);

		uniqueChildIdIndex = uniqueChildStartingIdIndex;
		if(savedInstanceState == null) {
			events = new ArrayList<Event>();
		} else {
			long[] te = savedInstanceState.getLongArray(STATE_EVENT);
			try {
				for(int i = 0; i < te.length; i++) {
					events.add(EventHelper.getInstance(getApplicationContext()).read(te[i]));
				}
			} catch(Exception e) {
				Log.e(LOG_NAME, "Could not hydrate events!");
			}
		}

		final boolean pickDefaultEvent = getIntent().getBooleanExtra(EXTRA_CHOOSE_CURRENT_EVENT, false);

		BroadcastReceiver initialFetchReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(this);

				// status?
				if (intent.getBooleanExtra("status", false)) {
					User currentUser = null;
					try {
						currentUser = UserHelper.getInstance(getApplicationContext()).readCurrentUser();
						if(currentUser.getRole().equals(RoleHelper.getInstance(getApplicationContext()).readAdmin())) {
							// now that ADMINS can be part of any event, make sure they don't push data to events they are not part of!!
							events = EventHelper.getInstance(getApplicationContext()).readAll();
						} else {
							events = EventHelper.getInstance(getApplicationContext()).getEventsForCurrentUser();
						}
					} catch(Exception e) {
						Log.e(LOG_NAME, "Could not get current events!");
					}

					if(events.isEmpty() || currentUser == null) {
						Log.e(LOG_NAME, "User is part of no events!");
						((MAGE) getApplication()).onLogout(true);
						findViewById(R.id.event_status).setVisibility(View.GONE);
						findViewById(R.id.event_content).setVisibility(View.VISIBLE);
						findViewById(R.id.event_continue_button).setVisibility(View.GONE);
						findViewById(R.id.event_select_content).setVisibility(View.GONE);
						findViewById(R.id.event_back_button).setVisibility(View.VISIBLE);
						findViewById(R.id.event_bummer_info).setVisibility(View.VISIBLE);
						findViewById(R.id.event_serverproblem_info).setVisibility(View.GONE);
					} else {
						Event userRecentEvent = currentUser.getCurrentEvent();

						if(userRecentEvent == null) {
							userRecentEvent = events.get(0);
							currentUser.setCurrentEvent(userRecentEvent);
							UserHelper.getInstance(getApplicationContext()).createOrUpdate(currentUser);
						}

						if(events.size() == 1 && events.get(0).equals(userRecentEvent)) {
							currentUser.setCurrentEvent(userRecentEvent);
							UserHelper.getInstance(getApplicationContext()).createOrUpdate(currentUser);
							chosenEvent = userRecentEvent;
							finishAccount(new AccountStatus(AccountStatus.Status.SUCCESSFUL_LOGIN));
						} else if (pickDefaultEvent && events.contains(userRecentEvent)) {
							chosenEvent = userRecentEvent;
							finishAccount(new AccountStatus(AccountStatus.Status.SUCCESSFUL_LOGIN));
						} else {
							List<Event> tempEventsForCurrentUser = EventHelper.getInstance(getApplicationContext()).getEventsForCurrentUser();
							((RadioGroup)findViewById(R.id.event_radiogroup)).removeAllViews();
							for (Event e : events) {
								RadioButton radioButton = new RadioButton(getApplicationContext());
								radioButton.setId(uniqueChildIdIndex++);
								String text = e.getName();
								if(!tempEventsForCurrentUser.contains(e)) {
									text += " (read-only access)";
								}
								radioButton.setText(text);

								if(userRecentEvent.getRemoteId().equals(e.getRemoteId())) {
									radioButton.setChecked(true);
								}

								((RadioGroup)findViewById(R.id.event_radiogroup)).addView(radioButton);
							}
							findViewById(R.id.event_status).setVisibility(View.GONE);
							findViewById(R.id.event_content).setVisibility(View.VISIBLE);
						}
					}
				} else {
					findViewById(R.id.event_status).setVisibility(View.GONE);
					findViewById(R.id.event_content).setVisibility(View.VISIBLE);
					Log.e(LOG_NAME, "User is part of no event!");
					((MAGE) getApplication()).onLogout(true);
					findViewById(R.id.event_continue_button).setVisibility(View.GONE);
					findViewById(R.id.event_select_content).setVisibility(View.GONE);
					findViewById(R.id.event_back_button).setVisibility(View.VISIBLE);
					findViewById(R.id.event_bummer_info).setVisibility(View.GONE);
					findViewById(R.id.event_serverproblem_info).setVisibility(View.VISIBLE);
				}
			}
		};

		// receive response from initial pull
		IntentFilter statusIntentFilter = new IntentFilter(InitialFetchIntentService.InitialFetchIntentServiceAction);
		statusIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);

		LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(initialFetchReceiver, statusIntentFilter);

		getApplicationContext().startService(new Intent(getApplicationContext(), InitialFetchIntentService.class));
	}

	public void onSaveInstanceState(Bundle savedInstanceState) {

		long[] te = new long[events.size()];

		for(int i = 0; i < events.size(); i++) {
			Event e = events.get(i);
			te[i] = e.getId();
		}

		savedInstanceState.putLongArray(STATE_EVENT, te);
		super.onSaveInstanceState(savedInstanceState);
	}

	public void chooseEvent(View view) {

		findViewById(R.id.event_content).setVisibility(View.GONE);
		findViewById(R.id.event_status).setVisibility(View.VISIBLE);

        int eventIndex = (((RadioGroup)findViewById(R.id.event_radiogroup)).getCheckedRadioButtonId() - uniqueChildStartingIdIndex);
        chosenEvent = events.get(eventIndex);

        List<String> userRecentEventInfo = new ArrayList<String>();
        userRecentEventInfo.add(chosenEvent.getRemoteId());

        new RecentEventTask(this, this.getApplicationContext()).execute(userRecentEventInfo.toArray(new String[userRecentEventInfo.size()]));
    }

    public void bummerEvent(View view) {
        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        finish();
    }

    public void finishAccount(AccountStatus accountStatus) {
        if (!accountStatus.getStatus().equals(AccountStatus.Status.SUCCESSFUL_LOGIN)) {
            Log.e(LOG_NAME, "Unable to post your recent event!");
        }
		// regardless of the return status, set the user's currentevent
		try {
			User currentUser = UserHelper.getInstance(getApplicationContext()).readCurrentUser();
			currentUser.setCurrentEvent(chosenEvent);
			UserHelper.getInstance(getApplicationContext()).createOrUpdate(currentUser);
		} catch(Exception e) {
			Log.e(LOG_NAME, "Could not set current event.");
		}

        SharedPreferences.Editor sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        sp.putString(getString(R.string.currentEventKey), String.valueOf(chosenEvent.getName())).commit();

		// disable pushing locations
		if(!UserHelper.getInstance(getApplicationContext()).isCurrentUserPartOfCurrentEvent()) {
			sp.putBoolean(getString(R.string.reportLocationKey), false).commit();
		}

        // start up the landing activity!
		Intent intent = new Intent(getApplicationContext(), LandingActivity.class);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			intent.putExtras(extras);
		}
		startActivity(intent);
        finish();
    }
}
