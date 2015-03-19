package mil.nga.giat.mage.preferences;

import mil.nga.giat.mage.R;
import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.widget.CompoundButton;
import android.widget.Switch;

public class LocationPreferencesActivity extends PreferenceActivity {

	private final LocationPreferenceFragment preference = new LocationPreferenceFragment();

    public static class LocationPreferenceFragment extends PreferenceFragmentSummary implements CompoundButton.OnCheckedChangeListener {

		private Switch locationSwitch;

		public LocationPreferenceFragment() {
			Bundle bundle = new Bundle();
			bundle.putInt(PreferenceFragmentSummary.xmlResourceClassKey, R.xml.locationpreferences);
			setArguments(bundle);
		}

        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);

            Activity activity = getActivity();
            ActionBar actionbar = activity.getActionBar();
            locationSwitch = new Switch(activity);

            actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
            actionbar.setCustomView(locationSwitch, 
                    new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, 
                            ActionBar.LayoutParams.WRAP_CONTENT, 
                            Gravity.CENTER_VERTICAL | Gravity.RIGHT));
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
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            boolean locationServiceEnabled = preferences.getBoolean(getString(R.string.locationServiceEnabledKey), getResources().getBoolean(R.bool.locationServiceEnabledDefaultValue));
            locationSwitch.setChecked(locationServiceEnabled);

            int count = getPreferenceScreen().getPreferenceCount();
            for (int i = 0; i < count; ++i) {
                Preference pref = getPreferenceScreen().getPreference(i);
                pref.setEnabled(locationServiceEnabled);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, preference).commit();
    }
}