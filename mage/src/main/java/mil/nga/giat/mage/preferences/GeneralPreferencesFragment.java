package mil.nga.giat.mage.preferences;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import mil.nga.giat.mage.R;

public class GeneralPreferencesFragment extends PreferenceFragmentCompat {

	private boolean locationServicesEnabled;
	private Preference locationServicesPreference;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(false);
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.generalpreferences);

		locationServicesPreference = findPreference(getActivity().getResources().getString(R.string.locationServiceEnabledKey));

		locationServicesEnabled = ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
		setLocationServicesSummary();

		locationServicesPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !locationServicesEnabled) {
					Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
					intent.setData(Uri.fromParts("package", getActivity().getPackageName(), null));
					startActivity(intent);
					return true;
				}

				return false;
			}
		});
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
		actionBar.setTitle("Settings");
		actionBar.setSubtitle(null);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.AppTheme_PrimaryAccent);
		LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);
		return super.onCreateView(localInflater, container, savedInstanceState);
	}

	@Override
	public void onResume() {
		super.onResume();

		getActivity().invalidateOptionsMenu();

		if (locationServicesEnabled != (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
			locationServicesEnabled = ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
			setLocationServicesSummary();
		}
	}



	private void setLocationServicesSummary() {
		if (locationServicesEnabled) {
			locationServicesPreference.setSummary(getActivity().getApplicationContext().getResources().getString(R.string.location_services_enabled_summary));
		} else {
			locationServicesPreference.setSummary(getActivity().getApplicationContext().getResources().getString(R.string.location_services_disabled_summary));
		}
	}
}