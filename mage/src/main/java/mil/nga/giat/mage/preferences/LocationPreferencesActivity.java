package mil.nga.giat.mage.preferences;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.qualifiers.ApplicationContext;
import mil.nga.giat.mage.MageApplication;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.location.LocationAccess;
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource;

@AndroidEntryPoint
public class LocationPreferencesActivity extends AppCompatActivity {

    private final LocationPreferenceFragment preference = new LocationPreferenceFragment();

    @Inject protected MageApplication application;
    @Inject protected @ApplicationContext Context context;
    @Inject protected LocationAccess locationAccess;

    @AndroidEntryPoint
    public static class LocationPreferenceFragment extends PreferenceFragmentCompat {
        @Inject protected @ApplicationContext Context context;
        @Inject protected UserLocalDataSource userLocalDataSource;
        @Inject protected LocationAccess locationAccess;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.locationpreferences);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.AppTheme);
            LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

            if (!userLocalDataSource.isCurrentUserPartOfCurrentEvent()) {
                Preference reportLocationPreference = findPreference(getString(R.string.reportLocationKey));
                reportLocationPreference.setEnabled(false);
                reportLocationPreference.setSummary(R.string.location_no_event_message);
            }

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
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean serverLocationServiceDisabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("gLocationServiceDisabled", false);
        findViewById(R.id.no_content_frame_disabled).setVisibility(serverLocationServiceDisabled ? View.VISIBLE : View.GONE);
        findViewById(R.id.no_content_frame).setVisibility(locationAccess.isLocationGranted() ? View.GONE : View.VISIBLE);

        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, preference).commit();
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

    public void launchPermissions(View view) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }
}