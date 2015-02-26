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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import mil.nga.giat.mage.LandingActivity;
import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.login.LoginActivity;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.Team;
import mil.nga.giat.mage.sdk.datastore.user.TeamHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.fetch.InitialFetchIntentService;
import mil.nga.giat.mage.sdk.login.AccountDelegate;
import mil.nga.giat.mage.sdk.login.AccountStatus;
import mil.nga.giat.mage.sdk.login.RecentEventTask;

public class EventActivity extends Activity implements AccountDelegate {

    private static final String LOG_NAME = EventActivity.class.getName();

    private static final int uniqueChildStartingIdIndex = 10000;

    private int uniqueChildIdIndex = uniqueChildStartingIdIndex;

    private List<Event> currentEvents = new ArrayList<Event>();

    private Event chosenEvent = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uniqueChildIdIndex = uniqueChildStartingIdIndex;
        currentEvents = new ArrayList<Event>();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_event);

        BroadcastReceiver initialFetchReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(this);

                // status?
                if (intent.getBooleanExtra("status", false)) {
                    User currentUser = null;
                    try {
                        currentUser = UserHelper.getInstance(getApplicationContext()).readCurrentUser();
						currentEvents = EventHelper.getInstance(getApplicationContext()).getEventsByUser(currentUser);
                    } catch(Exception e) {
                        Log.e(LOG_NAME, "Could not get current events!");
                    }

                    Collections.sort(currentEvents, new Comparator<Event>() {
                        @Override
                        public int compare(Event lhs, Event rhs) {
                            return lhs.getName().compareTo(rhs.getName());
                        }
                    });

                    if(currentEvents.isEmpty() || currentUser == null) {
                        Log.e(LOG_NAME, "User is part of no event!");
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
                            userRecentEvent = currentEvents.get(0);
                            currentUser.setCurrentEvent(userRecentEvent);
                            UserHelper.getInstance(getApplicationContext()).createOrUpdate(currentUser);
                        }

                        if(currentEvents.size() == 1 && currentEvents.get(0).equals(userRecentEvent)) {
                            currentUser.setCurrentEvent(userRecentEvent);
                            UserHelper.getInstance(getApplicationContext()).createOrUpdate(currentUser);
                            chosenEvent = userRecentEvent;
                            finishAccount(new AccountStatus(AccountStatus.Status.SUCCESSFUL_LOGIN));
                        } else {
                            for (Event e : currentEvents) {
                                RadioButton radioButton = new RadioButton(getApplicationContext());
                                radioButton.setId(uniqueChildIdIndex++);
                                radioButton.setText(e.getName());
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

    public void chooseEvent(View view) {
        int eventIndex = (((RadioGroup)findViewById(R.id.event_radiogroup)).getCheckedRadioButtonId() - uniqueChildStartingIdIndex);
        chosenEvent = currentEvents.get(eventIndex);

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
        SharedPreferences.Editor sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        sp.putString(getApplicationContext().getString(R.string.currenteventKey), String.valueOf(chosenEvent.getName())).commit();

        // start up the landing activity!
        startActivity(new Intent(getApplicationContext(), LandingActivity.class));
        ((MAGE) getApplication()).onLogin();
        finish();
    }
}
