package mil.nga.giat.mage.preferences;

/**
 * Created by thanhm on 6/8/15.
 */

import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;

import mil.nga.giat.mage.R;

public class NotificationPreferencesActivity extends PreferenceActivity {

    private final NotificationPreferenceFragment preference = new NotificationPreferenceFragment();

    public static class NotificationPreferenceFragment extends PreferenceFragmentSummary implements CompoundButton.OnCheckedChangeListener {

        private Switch notificationSwitch;

        public NotificationPreferenceFragment() {
            Bundle bundle = new Bundle();
            bundle.putInt(PreferenceFragmentSummary.xmlResourceClassKey, R.xml.notificationpreferences);
            setArguments(bundle);
        }

        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);

            Activity activity = getActivity();
            ActionBar actionbar = activity.getActionBar();
            notificationSwitch = new Switch(activity);

            actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
            actionbar.setCustomView(notificationSwitch,
                    new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                            ActionBar.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER_VERTICAL | Gravity.RIGHT));
        }

        @Override
        public void onResume() {
            super.onResume();
            updateEnabled();
            notificationSwitch.setOnCheckedChangeListener(this);
        }

        @Override
        public void onPause() {
            notificationSwitch.setOnCheckedChangeListener(null);
            super.onPause();
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean(getResources().getString(R.string.notificationsEnabledKey), isChecked).commit();

            updateEnabled();
        }

        protected void updateEnabled() {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            boolean notificationsEnabled = preferences.getBoolean(getString(R.string.notificationsEnabledKey), getResources().getBoolean(R.bool.notificationsEnabledDefaultValue));
            notificationSwitch.setChecked(notificationsEnabled);

            int count = getPreferenceScreen().getPreferenceCount();
            for (int i = 0; i < count; ++i) {
                Preference pref = getPreferenceScreen().getPreference(i);
                pref.setEnabled(notificationsEnabled);
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
