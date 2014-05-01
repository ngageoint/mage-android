package mil.nga.giat.mage.preferences;

import mil.nga.giat.mage.R;
import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.widget.CompoundButton;
import android.widget.Switch;

public class FetchPreferencesActivity extends PreferenceActivity {

    FetchPreferenceFragment preference = new FetchPreferenceFragment();

    public static class FetchPreferenceFragment extends PreferenceFragmentSummary implements CompoundButton.OnCheckedChangeListener {

        private Switch fetchSwitch;

        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.fetchpreferences);

            PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);

            Activity activity = getActivity();
            ActionBar actionbar = activity.getActionBar();
            fetchSwitch = new Switch(activity);

            actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
            actionbar.setCustomView(fetchSwitch, 
                    new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, 
                            ActionBar.LayoutParams.WRAP_CONTENT, 
                            Gravity.CENTER_VERTICAL | Gravity.RIGHT));
            
            updateSettings();
        }
        
        @Override
        public void onResume() {
            super.onResume();
            fetchSwitch.setOnCheckedChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            fetchSwitch.setOnCheckedChangeListener(null);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
            editor.putBoolean(getResources().getString(R.string.dataFetchEnabledKey), isChecked);
            editor.commit();

            updateSettings();
        }
        
        protected void updateSettings() {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            boolean dataFetchEnabled = preferences.getBoolean(getResources().getString(R.string.dataFetchEnabledKey), false);
            fetchSwitch.setChecked(dataFetchEnabled);

            int count = getPreferenceScreen().getPreferenceCount();
            for (int i = 0; i < count; ++i) {
                Preference pref = getPreferenceScreen().getPreference(i);
                pref.setEnabled(dataFetchEnabled);
                setSummary(getPreferenceScreen().getPreference(i));
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, preference).commit();
    }
}