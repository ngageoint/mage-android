package mil.nga.giat.mage.preferences;

import android.content.Context;
import android.os.Bundle;
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
}