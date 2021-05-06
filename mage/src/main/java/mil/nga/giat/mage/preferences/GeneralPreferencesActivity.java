package mil.nga.giat.mage.preferences;

import android.content.Context;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.preferences.color.ColorPickerPreference;
import mil.nga.giat.mage.preferences.color.ColorPreferenceFragment;


public class GeneralPreferencesActivity extends AppCompatActivity implements
		PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_general_preferences);

		getSupportActionBar().setTitle("Settings");

		getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new GeneralPreferencesFragment()).commit();
	}

	@Override
	public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
		// Instantiate the new Fragment
		final Bundle args = pref.getExtras();
		final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
				getClassLoader(),
				pref.getFragment());
		fragment.setArguments(args);
		fragment.setTargetFragment(caller, 0);
		// Replace the existing Fragment with the new Fragment
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.content_frame, fragment)
				.addToBackStack(null)
				.commit();
		return true;
	}

	public static class NavigationPreferencesFragment extends ColorPreferenceFragment implements Preference.OnPreferenceChangeListener {

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.navigationpreferences, rootKey);
//			addPreferencesFromResource(R.xml.navigationpreferences);

//			dayNightThemePreference = findPreference(getResources().getString(R.string.dayNightThemeKey));
		}

//		@Override
//		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//			final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.AppTheme);
//			LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);
//			return super.onCreateView(localInflater, container, savedInstanceState);
//		}
//
//		@Override
//		public void onDisplayPreferenceDialog(Preference preference) {
//			DialogFragment dialog = null;
//			if (preference instanceof ColorPickerPreference) {
//				dialog = new ColorPreferenceDialog((ColorPickerPreference)preference);
//			}
//
//			if (dialog != null) {
//				dialog.setTargetFragment(this, 0);
//				dialog.show(getChildFragmentManager(), FRAGMENT_TAG);
//			} else {
//				super.onDisplayPreferenceDialog(preference);
//			}
//		}

		//		@Override
//		public void onResume() {
//			super.onResume();
//
//			dayNightThemePreference.setOnPreferenceChangeListener(this);
//		}
//
//		@Override
//		public void onPause() {
//			super.onPause();
//
//			dayNightThemePreference.setOnPreferenceChangeListener(null);
//		}

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
//			AppCompatDelegate.setDefaultNightMode(Integer.parseInt(newValue.toString()));
			getActivity().recreate();
			return true;
		}
	}

	public static class GeneralPreferencesFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {
		private Preference dayNightThemePreference;

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			addPreferencesFromResource(R.xml.generalpreferences);

			dayNightThemePreference = findPreference(getResources().getString(R.string.dayNightThemeKey));
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.AppTheme);
			LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);
			return super.onCreateView(localInflater, container, savedInstanceState);
		}

		@Override
		public void onResume() {
			super.onResume();

			dayNightThemePreference.setOnPreferenceChangeListener(this);
		}

		@Override
		public void onPause() {
			super.onPause();

			dayNightThemePreference.setOnPreferenceChangeListener(null);
		}

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			AppCompatDelegate.setDefaultNightMode(Integer.parseInt(newValue.toString()));
			getActivity().recreate();
			return true;
		}
	}
}

