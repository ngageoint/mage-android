package mil.nga.giat.mage.preferences;

import mil.nga.giat.mage.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class PublicPreferencesFragment extends PreferenceFragmentSummary {

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActivity().getActionBar().setTitle("Settings");
		addPreferencesFromResource(R.xml.generalpreferences);

		for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
			setSummary(getPreferenceScreen().getPreference(i));
		}
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
}