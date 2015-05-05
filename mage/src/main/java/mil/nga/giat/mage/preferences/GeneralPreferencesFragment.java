package mil.nga.giat.mage.preferences;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import mil.nga.giat.mage.R;

public class GeneralPreferencesFragment extends PreferenceFragmentSummary {

	public GeneralPreferencesFragment() {
		Bundle bundle = new Bundle();
		bundle.putInt(PreferenceFragmentSummary.xmlResourceClassKey, R.xml.generalpreferences);
		setArguments(bundle);
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActivity().getActionBar().setTitle("Settings");
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

//	@Override
//	public void onResume() {
//		super.onResume();
//
//		SwitchPreference locationServiceSwitch = (SwitchPreference) getPreferenceManager().findPreference(getString(R.string.locationServiceEnabledKey));
//		SwitchPreference dataFetchSwitch = (SwitchPreference) getPreferenceManager().findPreference(getString(R.string.dataFetchEnabledKey));
//
//		boolean locationServiceEnabled = getPreferenceManager().getSharedPreferences().getBoolean(getString(R.string.locationServiceEnabledKey), getResources().getBoolean(R.bool.locationServiceEnabledDefaultValue));
//		boolean dataFetchEnabled = getPreferenceManager().getSharedPreferences().getBoolean(getString(R.string.dataFetchEnabledKey), getResources().getBoolean(R.bool.dataFetchEnabledDefaultValue));
//
//		locationServiceSwitch.setChecked(locationServiceEnabled);
//		dataFetchSwitch.setChecked(dataFetchEnabled);
//	}
}