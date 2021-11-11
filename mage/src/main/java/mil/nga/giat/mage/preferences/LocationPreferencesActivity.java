package mil.nga.giat.mage.preferences;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.qualifiers.ApplicationContext;
import mil.nga.giat.mage.MageApplication;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;

@AndroidEntryPoint
public class LocationPreferencesActivity extends AppCompatActivity {

    private final LocationPreferenceFragment preference = new LocationPreferenceFragment();

    @Inject
    protected MageApplication application;

    @Inject
    protected @ApplicationContext
    Context context;

    private Toolbar toolbar;

    @AndroidEntryPoint
    public static class LocationPreferenceFragment extends PreferenceFragmentCompat {
        @Inject
        protected @ApplicationContext Context context;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.locationpreferences);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.AppTheme);
            LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

            if (!UserHelper.getInstance(context).isCurrentUserPartOfCurrentEvent()) {
                Preference reportLocationPreference = findPreference(getString(R.string.reportLocationKey));
                reportLocationPreference.setEnabled(false);
                reportLocationPreference.setSummary("You are an administrator and not a member of the current event.  You can not report your location in this event.");
            }

            return super.onCreateView(localInflater, container, savedInstanceState);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_location_preferences);

        toolbar = findViewById(R.id.toolbar);
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean locationServicesEnabled = false;
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            toolbar.inflateMenu(R.menu.fetch_preferences_menu);
            toolbar.setVisibility(View.VISIBLE);

            locationServicesEnabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getResources().getString(R.string.locationServiceEnabledKey), getResources().getBoolean(R.bool.locationServiceEnabledDefaultValue));

            SwitchCompat locationServicesEnabledSwitch = toolbar.findViewById(R.id.toolbar_switch);
            locationServicesEnabledSwitch.setChecked(locationServicesEnabled);
            locationServicesEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean locationServicesEnabled) {
                    PreferenceManager.getDefaultSharedPreferences(LocationPreferencesActivity.this).edit().putBoolean(getResources().getString(R.string.locationServiceEnabledKey), locationServicesEnabled).commit();
                    updateView(locationServicesEnabled);

                    if (locationServicesEnabled) {
                        application.startLocationService();
                    } else {
                        application.stopLocationService();
                    }
                }
            });

            updateView(locationServicesEnabled);

        } else {
            toolbar.setVisibility(View.GONE);
            locationServicesEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            findViewById(R.id.no_content_frame_l).setVisibility(locationServicesEnabled ? View.GONE : View.VISIBLE);
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

    public void launchPermissions(View view) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }

    private void updateView(boolean locationServicesEnabled) {
        toolbar.setTitle(locationServicesEnabled ? "On" : "Off");
        findViewById(R.id.no_content_frame_pre_m).setVisibility(locationServicesEnabled ? View.GONE : View.VISIBLE);
    }
}