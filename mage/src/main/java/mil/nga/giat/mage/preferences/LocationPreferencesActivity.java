package mil.nga.giat.mage.preferences;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;

public class LocationPreferencesActivity extends AppCompatActivity {

	private final LocationPreferenceFragment preference = new LocationPreferenceFragment();

    private Toolbar toolbar;
    private View noContentView;

    public static class LocationPreferenceFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.locationpreferences);

            if (!UserHelper.getInstance(getActivity().getApplicationContext()).isCurrentUserPartOfCurrentEvent()) {
                Preference reportLocationPreference = findPreference(getString(R.string.reportLocationKey));
                reportLocationPreference.setEnabled(false);
                reportLocationPreference.setSummary("You are an administrator and not a member of the current event.  You can not report your location in this event.");
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.AppTheme_PrimaryAccent);
            LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);
            return super.onCreateView(localInflater, container, savedInstanceState);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_location_preferences);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            toolbar.inflateMenu(R.menu.fetch_preferences_menu);

            noContentView = findViewById(R.id.no_content_frame);

            boolean locationServicesEnabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getResources().getString(R.string.locationServiceEnabledKey), false);

            SwitchCompat locationServicesEnabledSwitch = (SwitchCompat) toolbar.findViewById(R.id.toolbar_switch);
            locationServicesEnabledSwitch.setChecked(locationServicesEnabled);
            locationServicesEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    PreferenceManager.getDefaultSharedPreferences(LocationPreferencesActivity.this).edit().putBoolean(getResources().getString(R.string.locationServiceEnabledKey), isChecked).commit();
                    updateView(isChecked);
                }
            });

            updateView(locationServicesEnabled);
        } else {
            toolbar.setVisibility(View.GONE);
            noContentView.setVisibility(View.GONE);
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, preference).commit();
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

    private void updateView(boolean locationServicesEnabled) {
        toolbar.setTitle(locationServicesEnabled ? "On" : "Off");
        noContentView.setVisibility(locationServicesEnabled ? View.GONE : View.VISIBLE);
    }
}