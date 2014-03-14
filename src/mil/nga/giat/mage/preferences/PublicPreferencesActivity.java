package mil.nga.giat.mage.preferences;

import java.util.Iterator;

import mil.nga.giat.mage.R;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;

/**
 * Provides configuration driven settings that are available to the user. Check
 * publicpreferences.xml for the configuration.
 * 
 * @author wiedemannse
 * 
 */
public class PublicPreferencesActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	PublicPreferenceFragment preference = new PublicPreferenceFragment();

	public class PublicPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(final Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.publicpreferences);
			addPreferencesFromResource(R.xml.mdkpublicpreferences);

			for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
				initSummary(getPreferenceScreen().getPreference(i));
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction().replace(android.R.id.content, preference).commit();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Set up a listener whenever a key changes
		preference.getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		preference.getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// TODO android HACK as MultiSelectListPreferences don't fire updates as
		// they should
		Preference p = preference.findPreference(key.split(DialogPreference.MultiSelectListPreferenceKey)[0]);
		updatePrefSummary(p);
	}

	private void initSummary(Preference p) {
		if (p instanceof PreferenceCategory) {
			PreferenceCategory pCat = (PreferenceCategory) p;
			for (int i = 0; i < pCat.getPreferenceCount(); i++) {
				initSummary(pCat.getPreference(i));
			}
		} else {
			updatePrefSummary(p);
		}
	}

	private void updatePrefSummary(Preference p) {
		if (p instanceof ListPreference) {
			ListPreference pref = (ListPreference) p;
			p.setSummary(pref.getEntry());
		} else if (p instanceof EditTextPreference) {
			EditTextPreference pref = (EditTextPreference) p;
			p.setSummary(pref.getText());
		} else if (p instanceof MultiSelectListPreference) {
			MultiSelectListPreference pref = (MultiSelectListPreference) p;
			CharSequence[] entries = pref.getEntries();
			StringBuffer summary = new StringBuffer();

			Iterator<String> iterator = pref.getValues().iterator();
			while (iterator.hasNext()) {
				String value = iterator.next();
				summary.append(entries[pref.findIndexOfValue(value)]);

				if (iterator.hasNext()) summary.append("\n");
			}

			p.setSummary(summary);
		} else {
			p.setSummary(preference.getPreferenceScreen().getSharedPreferences().getString(p.getKey(), ""));
		}
	}
}