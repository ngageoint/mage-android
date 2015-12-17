package mil.nga.giat.mage.preferences;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import mil.nga.giat.mage.R;

public class GeneralPreferencesFragment extends PreferenceFragment {

	private boolean locationServicesEnabled;
	private Preference locationServicesPreference;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActivity().getActionBar().setTitle("Settings");

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
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			ListView listView = (ListView) view.findViewById(android.R.id.list);
			listView.setPadding(listView.getPaddingLeft(), listView.getPaddingTop(), 0, listView.getPaddingBottom());
		}

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

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