package mil.nga.giat.mage.preferences;

import java.util.Iterator;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;

/**
 * Provides configuration driven settings that are available to the user. Check
 * publicpreferences.xml for the configuration.
 * 
 * @author wiedemannse
 * 
 */
public class PreferenceFragmentSummary extends PreferenceFragment implements OnSharedPreferenceChangeListener {

	public void setSummary(Preference p) {		
		if (p instanceof PreferenceCategory) {
			PreferenceCategory pCat = (PreferenceCategory) p;
			for (int i = 0; i < pCat.getPreferenceCount(); i++) {
				setSummary(pCat.getPreference(i));
			}
		} else if (p instanceof ListPreference) {
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
		} 
		
//		else {
//			p.setSummary(getPreferenceScreen().getSharedPreferences().getString(p.getKey(), ""));
//		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// TODO android HACK as MultiSelectListPreferences don't fire updates as
		// they should
		Preference p = findPreference(key.split(DialogPreference.MultiSelectListPreferenceKey)[0]);
		setSummary(p);
	}
}