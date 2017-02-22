package mil.nga.giat.mage.event;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.AppCompatRadioButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.RoleHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.login.AccountDelegate;
import mil.nga.giat.mage.sdk.login.AccountStatus;
import mil.nga.giat.mage.sdk.login.RecentEventTask;

/**
 * Allows the user to switch events within the app
 *
 * @author wiedemanns
 */

public class ChangeEventFragment extends Fragment {

	public interface OnEventChangedListener {
		void onEventChanged();
	}

	private static final String LOG_NAME = ChangeEventFragment.class.getName();

	private static final int uniqueChildStartingIdIndex = 10000;
	private int uniqueChildIdIndex = uniqueChildStartingIdIndex;
	private List<Event> events = new ArrayList<>();
	private RadioGroup radioGroup;
	private int checkedID;
	private OnEventChangedListener onEventChangedListener;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_events, container, false);

		setHasOptionsMenu(true);

		ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
		actionBar.setTitle("Events");
		actionBar.setSubtitle(null);

		uniqueChildIdIndex = uniqueChildStartingIdIndex;

		final Context context = getActivity().getApplicationContext();

		try {
			final User currentUser = UserHelper.getInstance(context).readCurrentUser();
			if(currentUser.getRole().equals(RoleHelper.getInstance(context).readAdmin())) {
				// now that ADMINS can be part of any event, make sure they don't push data to events they are not part of!!
				events = EventHelper.getInstance(context).readAll();
			} else {
				events = EventHelper.getInstance(context).getEventsForCurrentUser();
			}

			Event currentEvent = currentUser.getUserLocal().getCurrentEvent();

			radioGroup = ((RadioGroup) view.findViewById(R.id.event_fragment_radiogroup));
			radioGroup.removeAllViews();
			List<Event> tempEventsForCurrentUser = EventHelper.getInstance(context).getEventsForCurrentUser();
			for (Event e : events) {
				ContextThemeWrapper wrapper = new ContextThemeWrapper(getActivity(), R.style.AppTheme_Radio);
				AppCompatRadioButton radioButton = new AppCompatRadioButton(wrapper);
				radioButton.setId(uniqueChildIdIndex++);
				String text = e.getName();
				if (!tempEventsForCurrentUser.contains(e)) {
					text += " (read-only access)";
				}
				radioButton.setText(text);

				if (currentEvent.getRemoteId().equals(e.getRemoteId())) {
					checkedID = radioButton.getId();
				}

				radioGroup.addView(radioButton);
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, "Could not get current events!");
		}

		view.findViewById(R.id.event_fragment_continue_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				done();
			}
		});

		return view;
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		// This makes sure that the container activity has implemented
		// the callback interface. If not, it throws an exception
		try {
			onEventChangedListener = (OnEventChangedListener) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " must implement OnEventChangedListener");
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		radioGroup.check(checkedID);
	}

	private void done() {
		final Context context = getActivity().getApplicationContext();

		int eventIndex = radioGroup.getCheckedRadioButtonId() - uniqueChildStartingIdIndex;
		Event chosenEvent = events.get(eventIndex);

		List<String> userRecentEventInfo = new ArrayList<>();
		userRecentEventInfo.add(chosenEvent.getRemoteId());

		try {
			UserHelper userHelper = UserHelper.getInstance(context);
			User user = userHelper.readCurrentUser();

			if (!UserHelper.getInstance(context).isCurrentUserPartOfEvent(chosenEvent)) {
				SharedPreferences.Editor sp = PreferenceManager.getDefaultSharedPreferences(context).edit();
				sp.putBoolean(getString(R.string.reportLocationKey), false).commit();
			}

			userHelper.setCurrentEvent(user, chosenEvent);
		} catch (UserException e) {
			Log.e(LOG_NAME, "Could not set current event.");
		}

		new RecentEventTask(new AccountDelegate() {
			@Override
			public void finishAccount(AccountStatus accountStatus) {

			}
		}, context).execute(userRecentEventInfo.toArray(new String[userRecentEventInfo.size()]));

		onEventChangedListener.onEventChanged();
	}
}
