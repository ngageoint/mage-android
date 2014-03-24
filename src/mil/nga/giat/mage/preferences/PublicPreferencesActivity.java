package mil.nga.giat.mage.preferences;

import mil.nga.giat.mage.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class PublicPreferencesActivity extends PreferenceActivity {

	PublicPreferenceFragment preference = new PublicPreferenceFragment();

	public static class PublicPreferenceFragment extends PreferenceFragmentSummary {
		@Override
		public void onCreate(final Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.publicpreferences);
			addPreferencesFromResource(R.xml.mdkpublicpreferences);

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