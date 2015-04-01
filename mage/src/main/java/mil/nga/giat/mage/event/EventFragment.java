package mil.nga.giat.mage.event;

import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		uniqueChildIdIndex = uniqueChildStartingIdIndex;
		View rootView = inflater.inflate(R.layout.fragment_events, container, false);
		getActivity().getActionBar().setTitle("Events");

		final FragmentManager fragmentManager = getFragmentManager();
		final EventBannerFragment eventBannerFragment = new EventBannerFragment();
		fragmentManager.beginTransaction().add(R.id.event_fragment_event_holder, eventBannerFragment).commit();

		try {
			final User currentUser = UserHelper.getInstance(getActivity().getApplicationContext()).readCurrentUser();
			if(currentUser.getRole().equals(RoleHelper.getInstance(getActivity().getApplicationContext()).readAdmin())) {
				// now that ADMINS can be part of any event, make sure they don't push data to events they are not part of!!
				events = EventHelper.getInstance(getActivity().getApplicationContext()).readAll();
			} else {
				events = EventHelper.getInstance(getActivity().getApplicationContext()).getEventsForCurrentUser();
			}

			Event currentEvent = currentUser.getCurrentEvent();

			RadioGroup radioGroup = ((RadioGroup) rootView.findViewById(R.id.event_fragment_radiogroup));

			List<Event> tempEventsForCurrentUser = EventHelper.getInstance(getActivity().getApplicationContext()).getEventsForCurrentUser();
			for (Event e : events) {
				RadioButton radioButton = new RadioButton(getActivity().getApplicationContext());
				radioButton.setId(uniqueChildIdIndex++);
				String text = e.getName();
				if(!tempEventsForCurrentUser.contains(e)) {
					text += " (admin access)";
				}
				radioButton.setText(text);
				radioButton.setTextColor(Color.BLACK);
				if (currentEvent.getRemoteId().equals(e.getRemoteId())) {
					radioButton.setChecked(true);
				}

				radioGroup.addView(radioButton);
			}

			radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
				public void onCheckedChanged(RadioGroup rGroup, int checkedId) {
					int eventIndex = rGroup.getCheckedRadioButtonId() - uniqueChildStartingIdIndex;
					Event chosenEvent = events.get(eventIndex);

					List<String> userRecentEventInfo = new ArrayList<String>();
					userRecentEventInfo.add(chosenEvent.getRemoteId());

					new RecentEventTask(new AccountDelegate() {
						@Override
						public void finishAccount(AccountStatus accountStatus) {

						}
					}, getActivity().getApplicationContext()).execute(userRecentEventInfo.toArray(new String[userRecentEventInfo.size()]));

					// regardless of the return status, set the user's currentevent which will fire off local onEventChanged
					try {
						if(!UserHelper.getInstance(getActivity().getApplicationContext()).isCurrentUserPartOfEvent(chosenEvent)) {
							PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit().putBoolean(getString(R.string.reportLocationKey), false).commit();
						}
						currentUser.setCurrentEvent(chosenEvent);
						UserHelper.getInstance(getActivity().getApplicationContext()).createOrUpdate(currentUser);
						eventBannerFragment.refresh();
					} catch(Exception e) {
						Log.e(LOG_NAME, "Could not set current event.");
					}
				}
			});

		} catch (Exception e) {
			Log.e(LOG_NAME, "Could not get current events!");
		}

		return rootView;
	}
}
