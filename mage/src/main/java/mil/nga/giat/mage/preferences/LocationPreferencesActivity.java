package mil.nga.giat.mage.preferences;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.location.LocationManagerCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.qualifiers.ApplicationContext;
import mil.nga.giat.mage.MageApplication;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.location.LocationAccessPermissionsState;
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource;

@AndroidEntryPoint
public class LocationPreferencesActivity extends AppCompatActivity {

    private final LocationPreferenceFragment preference = new LocationPreferenceFragment();

    @Inject protected MageApplication application;
    @Inject protected @ApplicationContext Context context;
    @Inject protected LocationAccessPermissionsState locationAccess;

    @AndroidEntryPoint
    public static class LocationPreferenceFragment extends PreferenceFragmentCompat {
        @Inject protected @ApplicationContext Context context;
        @Inject protected UserLocalDataSource userLocalDataSource;
        @Inject protected LocationAccessPermissionsState locationAccess;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.locationpreferences);
            CheckBoxPreference reportLocation = findPreference(getString(R.string.reportLocationKey));

            if (reportLocation != null) {
                boolean serverLocationDisabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(getString(R.string.locationServiceDisabledKey), getResources().getBoolean(R.bool.locationServiceDisabledDefaultValue));
                if (serverLocationDisabled) {
                    //server location tracking is disabled
                    reportLocation.setEnabled(false);
                    reportLocation.setChecked(false);
                    reportLocation.setSummary(getString(R.string.report_location_disabled));
                } else {
                    reportLocation.setOnPreferenceChangeListener((preference, newValue) -> {
                        boolean isEnabling = (boolean) newValue;

                        if (isEnabling) {
                            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                            if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
                                //device location tracking is disabled
                                new AlertDialog.Builder(getActivity())
                                        .setTitle(getString(R.string.location_access_denied_title))
                                        .setMessage(getString(R.string.location_device_access_disabled_message))
                                        .setPositiveButton(getString(R.string.settings), (dialog, which) -> {
                                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                            startActivity(intent);
                                        })
                                        .setNegativeButton(getString(android.R.string.cancel), null)
                                        .show();
                                return false;
                            }

                            if (!locationAccess.isLocationGranted()) {
                                //location permission is not granted
                                new AlertDialog.Builder(getActivity())
                                        .setTitle(getString(R.string.location_access_denied_title))
                                        .setMessage(getString(R.string.location_access_report_message))
                                        .setPositiveButton(getString(R.string.settings), (dialog, which) -> {
                                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            intent.setData(Uri.fromParts("package", getActivity().getPackageName(), null));
                                            startActivity(intent);
                                        })
                                        .setNegativeButton(getString(android.R.string.cancel), null)
                                        .show();
                                return false;
                            }
                        }

                        return true;
                    });
                }
            }
        }

        @Override
        public void onResume() {
            super.onResume();

            CheckBoxPreference reportLocation = findPreference(getString(R.string.reportLocationKey));

            if (reportLocation != null) {
                boolean serverLocationDisabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(getString(R.string.locationServiceDisabledKey), getResources().getBoolean(R.bool.locationServiceDisabledDefaultValue));
                if (serverLocationDisabled) {
                    //server location tracking is disabled
                    reportLocation.setEnabled(false);
                    reportLocation.setChecked(false);
                    reportLocation.setSummary(getString(R.string.report_location_disabled));
                } else if (reportLocation.isChecked()) {
                    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                    if (!LocationManagerCompat.isLocationEnabled(locationManager) || !locationAccess.isLocationGranted()) {
                        //device location tracking disabled or location permission not granted
                        reportLocation.setChecked(false);
                    }
                }
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.AppTheme);
            LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

            if (!locationAccess.isPreciseLocationGranted()) {
                Preference locationPushFrequency = findPreference(getString(R.string.locationPushFrequencyKey));
                locationPushFrequency.setEnabled(false);
                locationPushFrequency.setSummary("Precise location access is denied.  Approximate locations will be pushed to server when received from the GPS.");

                Preference gpsSensitivity = findPreference(getString(R.string.gpsSensitivityKey));
                gpsSensitivity.setEnabled(false);
                gpsSensitivity.setSummary("Precise location access is denied.  All approximate locations will used regardless of accuracy.");
            }

            return super.onCreateView(localInflater, container, savedInstanceState);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_location_preferences);

        Toolbar mainToolbar = findViewById(R.id.location_prefs_toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(mainToolbar, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left,0,insets.right, 0);
            return windowInsets;
        });

        setSupportActionBar(mainToolbar);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        View contentFrame = findViewById(R.id.location_prefs_container);
        ViewCompat.setOnApplyWindowInsetsListener(contentFrame, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(insets.left,0,insets.right, insets.bottom);
            return windowInsets;
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!preference.isAdded()) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, preference)
                    .commit();
        }
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}