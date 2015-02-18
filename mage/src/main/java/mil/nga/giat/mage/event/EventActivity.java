package mil.nga.giat.mage.event;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import mil.nga.giat.mage.LandingActivity;
import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.Team;
import mil.nga.giat.mage.sdk.datastore.user.TeamHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.login.AccountDelegate;
import mil.nga.giat.mage.sdk.login.AccountStatus;
import mil.nga.giat.mage.sdk.login.RecentEventTask;

public class EventActivity extends Activity implements AccountDelegate {

    private static final String LOG_NAME = EventActivity.class.getName();

    private static final int uniqueChildStartingIdIndex = 10000;

    private int uniqueChildIdIndex = uniqueChildStartingIdIndex;

    private List<Event> currentEvents = new ArrayList<Event>();

    private Event choosenEvent = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uniqueChildIdIndex = uniqueChildStartingIdIndex;
        currentEvents = new ArrayList<Event>();

        User currentUser = null;
        try {
            currentUser = UserHelper.getInstance(getApplicationContext()).readCurrentUser();

            List<Team> teams = TeamHelper.getInstance(getApplicationContext()).getTeamsByUser(currentUser);
            for(Team team : teams) {
                for(Event e : EventHelper.getInstance(getApplicationContext()).getEventsByTeam(team)) {
                    if(!currentEvents.contains(e)) {
                        currentEvents.add(e);
                    }
                }
            }
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

            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle("Event");
            alertDialog.setMessage("You are not in any events.  You must be part of an event to use MAGE.  Contact your administrator to be added to an event.");
            alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            alertDialog.show();

        } else {

            Event userRecentEvent = currentUser.getCurrentEvent();

            if(userRecentEvent == null) {
                userRecentEvent = currentEvents.get(0);
                currentUser.setCurrentEvent(userRecentEvent);
            }

            if(currentEvents.size() == 1 && currentEvents.get(0).equals(userRecentEvent)) {
                JSONObject json = new JSONObject();
                try {
                    json.put("currentEvent", String.valueOf(userRecentEvent.getName()));
                } catch(Exception e) {
                    Log.e(LOG_NAME, "Could not set currentEvent preference.");
                }
                finishAccount(new AccountStatus(AccountStatus.Status.SUCCESSFUL_LOGIN, new ArrayList<Integer>(), new ArrayList<String>(), json));
            }

            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.activity_event);

            for (Event e : currentEvents) {
                RadioButton radioButton = new RadioButton(getApplicationContext());
                radioButton.setId(uniqueChildIdIndex++);
                radioButton.setText(e.getName());
                if(userRecentEvent.getRemoteId().equals(e.getRemoteId())) {
                    radioButton.setChecked(true);
                }

                ((RadioGroup)findViewById(R.id.event_radiogroup)).addView(radioButton);
            }
        }
    }

    public void chooseEvent(View view) {
        int eventIndex = (((RadioGroup)findViewById(R.id.event_radiogroup)).getCheckedRadioButtonId() - uniqueChildStartingIdIndex);
        choosenEvent = currentEvents.get(eventIndex);

        List<String> userRecentEventInfo = new ArrayList<String>();
        userRecentEventInfo.add(choosenEvent.getRemoteId());

        new RecentEventTask(this, this.getApplicationContext()).execute(userRecentEventInfo.toArray(new String[userRecentEventInfo.size()]));
    }

    public void finishAccount(AccountStatus accountStatus) {
        if (!accountStatus.getStatus().equals(AccountStatus.Status.SUCCESSFUL_LOGIN)) {
            Log.e(LOG_NAME, "Unable to post your recent event!");
        }
        SharedPreferences.Editor sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        sp.putString(getApplicationContext().getString(R.string.currenteventKey), String.valueOf(choosenEvent.getName())).commit();

        // start up the landing activity!
        startActivity(new Intent(getApplicationContext(), LandingActivity.class));
        ((MAGE) getApplication()).onLogin();
        finish();
    }
}
