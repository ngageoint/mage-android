package mil.nga.giat.mage.event;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.RoleHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.login.AccountDelegate;
import mil.nga.giat.mage.sdk.login.AccountStatus;
import mil.nga.giat.mage.sdk.login.RecentEventTask;

/**
 * Allows the user to switch events within the app
 *
 * @author wiedemanns
 */
public class EventFragment extends Fragment {

	private static final String LOG_NAME = EventFragment.class.getName();

	private static final int uniqueChildStartingIdIndex = 10000;
	private int uniqueChildIdIndex = uniqueChildStartingIdIndex;
	private List<Event> events = new ArrayList<Event>();
	private RadioGroup radioGroup;
	private int checkedID;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		uniqueChildIdIndex = uniqueChildStartingIdIndex;
		View rootView = inflater.inflate(R.layout.fragment_events, container, false);
		getActivity().getActionBar().setTitle("Events");

		try {
			final User currentUser = UserHelper.getInstance(getActivity().getApplicationContext()).readCurrentUser();
			if(currentUser.getRole().equals(RoleHelper.getInstance(getActivity().getApplicationContext()).readAdmin())) {
				// now that ADMINS can be part of any event, make sure they don't push data to events they are not part of!!
				events = EventHelper.getInstance(getActivity().getApplicationContext()).readAll();
			} else {
				events = EventHelper.getInstance(getActivity().getApplicationContext()).getEventsForCurrentUser();
			}

			Event currentEvent = currentUser.getCurrentEvent();

			radioGroup = ((RadioGroup)rootView.findViewById(R.id.event_fragment_radiogroup));
			radioGroup.removeAllViews();
			List<Event> tempEventsForCurrentUser = EventHelper.getInstance(getActivity().getApplicationContext()).getEventsForCurrentUser();
			for (Event e : events) {
				RadioButton radioButton = new RadioButton(getActivity());
				radioButton.setId(uniqueChildIdIndex++);
				String text = e.getName();
				if(!tempEventsForCurrentUser.contains(e)) {
					text += " (read-only access)";
				}
				radioButton.setText(text);
				radioButton.setTextColor(Color.BLACK);
				if(currentEvent.getRemoteId().equals(e.getRemoteId())) {
					checkedID = radioButton.getId();
				}

				radioGroup.addView(radioButton);
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, "Could not get current events!");
		}


		((Button)rootView.findViewById(R.id.event_fragment_continue_button)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					final User currentUser = UserHelper.getInstance(getActivity().getApplicationContext()).readCurrentUser();
					int eventIndex = radioGroup.getCheckedRadioButtonId() - uniqueChildStartingIdIndex;
					Event chosenEvent = events.get(eventIndex);

					List<String> userRecentEventInfo = new ArrayList<String>();
					userRecentEventInfo.add(chosenEvent.getRemoteId());

					new RecentEventTask(new AccountDelegate() {
						@Override
						public void finishAccount(AccountStatus accountStatus) {

						}
					}, getActivity().getApplicationContext()).execute(userRecentEventInfo.toArray(new String[userRecentEventInfo.size()]));

					// regardless of the return status, set the user's currentevent which will fire off local onEventChanged
					SharedPreferences.Editor sp = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit();
					if (!UserHelper.getInstance(getActivity().getApplicationContext()).isCurrentUserPartOfEvent(chosenEvent)) {
						sp.putBoolean(getString(R.string.reportLocationKey), false).commit();
					}
					currentUser.setCurrentEvent(chosenEvent);
					UserHelper.getInstance(getActivity().getApplicationContext()).createOrUpdate(currentUser);
					sp.putString(getString(R.string.currentEventKey), String.valueOf(chosenEvent.getName())).commit();
				} catch (Exception e) {
					Log.e(LOG_NAME, "Could not set current event.");
				}
				getActivity().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
			}
		});

		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
		radioGroup.check(checkedID);
	}
}
