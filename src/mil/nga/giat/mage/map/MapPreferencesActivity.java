package mil.nga.giat.mage.map;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.preferences.PreferenceFragmentSummary;
import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Provides configuration driven settings that are available to the user. Check
 * publicpreferences.xml for the configuration.
 * 
 * @author wiedemannse
 * 
 */
public class MapPreferencesActivity extends PreferenceActivity {

	MapPreferenceFragment preference = new MapPreferenceFragment();

	public static class MapPreferenceFragment extends PreferenceFragmentSummary {

		@Override
		public void onCreate(final Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.map_preferences);
			
			for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
				setSummary(getPreferenceScreen().getPreference(i));			
			}
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction().replace(android.R.id.content, preference).commit();
	}
}