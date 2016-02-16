package mil.nga.giat.mage.preferences;

import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;

public class LocationPreferencesActivity extends PreferenceActivity {

	private final LocationPreferenceFragment preference = new LocationPreferenceFragment();

    public static class LocationPreferenceFragment extends PreferenceFragment implements CompoundButton.OnCheckedChangeListener {

		private Switch locationSwitch;

        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Activity activity = getActivity();
            locationSwitch = new Switch(activity);

            if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                ActionBar actionbar = getActivity().getActionBar();
                actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
                actionbar.setCustomView(locationSwitch,
                        new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                                ActionBar.LayoutParams.WRAP_CONTENT,
                                Gravity.CENTER_VERTICAL | Gravity.RIGHT));
            }

            addPreferencesFromResource(R.xml.locationpreferences);
        }

        @Override
        public void onResume() {
            super.onResume();

            updateEnabled();
            locationSwitch.setOnCheckedChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();

            locationSwitch.setOnCheckedChangeListener(null);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean(getResources().getString(R.string.locationServiceEnabledKey), isChecked).commit();
			updateEnabled();
        }
        
        protected void updateEnabled() {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Location services permissions are enabled/disabled outside the app in the phones
                // settings.  We won't switch manually
                return;
            }

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            boolean locationServiceEnabled = preferences.getBoolean(getString(R.string.locationServiceEnabledKey), getResources().getBoolean(R.bool.locationServiceEnabledDefaultValue));
            locationSwitch.setChecked(locationServiceEnabled);

            int count = getPreferenceScreen().getPreferenceCount();
            for (int i = 0; i < count; ++i) {
                Preference pref = getPreferenceScreen().getPreference(i);
                pref.setEnabled(locationServiceEnabled);
            }

			// ADMIN user?
			if (!UserHelper.getInstance(getActivity().getApplicationContext()).isCurrentUserPartOfCurrentEvent()) {
				Preference reportLocationPreference = findPreference(getString(R.string.reportLocationKey));
				reportLocationPreference.setEnabled(false);
				reportLocationPreference.setSummary("You are an administrator and not a member of the current event.  You can not report your location in this event.");
			}
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, preference).commit();
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}