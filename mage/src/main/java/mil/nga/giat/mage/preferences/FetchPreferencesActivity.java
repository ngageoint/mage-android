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

public class FetchPreferencesActivity extends PreferenceActivity {

    private final FetchPreferenceFragment preference = new FetchPreferenceFragment();

    public static class FetchPreferenceFragment extends PreferenceFragmentSummary implements CompoundButton.OnCheckedChangeListener {

        private Switch fetchSwitch;

		public FetchPreferenceFragment() {
			Bundle bundle = new Bundle();
			bundle.putInt(PreferenceFragmentSummary.xmlResourceClassKey, R.xml.fetchpreferences);
			setArguments(bundle);
		}

        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);

            Activity activity = getActivity();
            ActionBar actionbar = activity.getActionBar();
            fetchSwitch = new Switch(activity);

            actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
            actionbar.setCustomView(fetchSwitch, 
                    new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, 
                            ActionBar.LayoutParams.WRAP_CONTENT, 
                            Gravity.CENTER_VERTICAL | Gravity.RIGHT));
        }
        
        @Override
        public void onResume() {
            super.onResume();
			updateEnabled();
            fetchSwitch.setOnCheckedChangeListener(this);
        }

        @Override
        public void onPause() {
			fetchSwitch.setOnCheckedChangeListener(null);
            super.onPause();
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean(getResources().getString(R.string.dataFetchEnabledKey), isChecked).commit();

			updateEnabled();
        }
        
        protected void updateEnabled() {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            boolean dataFetchEnabled = preferences.getBoolean(getString(R.string.dataFetchEnabledKey), getResources().getBoolean(R.bool.dataFetchEnabledDefaultValue));
            fetchSwitch.setChecked(dataFetchEnabled);

            int count = getPreferenceScreen().getPreferenceCount();
            for (int i = 0; i < count; ++i) {
                Preference pref = getPreferenceScreen().getPreference(i);
                pref.setEnabled(dataFetchEnabled);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, preference).commit();
    }
}