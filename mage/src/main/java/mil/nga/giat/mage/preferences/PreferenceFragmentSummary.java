package mil.nga.giat.mage.preferences;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;

import java.util.Iterator;

import mil.nga.giat.mage.map.preference.OverlayPreference;
import mil.nga.giat.mage.sdk.preferences.IntegerListPreference;
import mil.nga.giat.mage.sdk.preferences.IntegerListValuePreference;
import mil.nga.giat.mage.sdk.preferences.ListValuePreference;

public abstract class PreferenceFragmentSummary extends PreferenceFragment implements OnSharedPreferenceChangeListener {

	private static final String LOG_NAME = PreferenceFragmentSummary.class.getName();

	public final static String xmlResourceClassKey = "xmlResourceClass";

    public void setSummary(Preference preference) {
        if (preference instanceof PreferenceCategory) {
            PreferenceCategory c = (PreferenceCategory) preference;
            for (int i = 0; i < c.getPreferenceCount(); i++) {
                setSummary(c.getPreference(i));
            }
        } else if (preference instanceof ListValuePreference) {
            ListValuePreference p = (ListValuePreference) preference;
            p.setListValue(p.getEntry());
        } else if (preference instanceof ListPreference) {
            ListPreference p = (ListPreference) preference;
            p.setSummary(p.getEntry());
		} else if (preference instanceof IntegerListValuePreference) {
			IntegerListValuePreference p = (IntegerListValuePreference) preference;
			p.setListValue(p.getEntry());
		} else if (preference instanceof IntegerListPreference) {
			IntegerListPreference p = (IntegerListPreference) preference;
			p.setSummary(p.getEntry());
		} else if (preference instanceof EditTextPreference) {
            EditTextPreference p = (EditTextPreference) preference;
            p.setSummary(p.getText());
        } else if (preference instanceof MultiSelectListPreference) {
            MultiSelectListPreference p = (MultiSelectListPreference) preference;
            CharSequence[] entries = p.getEntries();
            StringBuffer summary = new StringBuffer();

            Iterator<String> iterator = p.getValues().iterator();
            while (iterator.hasNext()) {
                String value = iterator.next();
                summary.append(entries[p.findIndexOfValue(value)]);

                if (iterator.hasNext())
                    summary.append("\n");
            }
            p.setSummary(summary);
		} else if (preference instanceof OverlayPreference) {
			OverlayPreference p = (OverlayPreference) preference;
			StringBuffer summary = new StringBuffer();
			Iterator<String> iterator = p.getValues().iterator();
			while (iterator.hasNext()) {
				String value = iterator.next();
				summary.append(value);

				if (iterator.hasNext())
					summary.append("\n");
			}
			p.setSummary(summary);
		} else if(preference != null) {
			Log.d(LOG_NAME, "Not setting preference summary for " + preference + " preference.");
		}
    }

    @Override
    public void onResume() {
        super.onResume();
		PreferenceScreen preferenceScreen = getPreferenceScreen();
		if(preferenceScreen != null) {
			preferenceScreen.removeAll();
		}
		addPreferencesFromResource(getArguments().getInt(xmlResourceClassKey));
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

		for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
			setSummary(getPreferenceScreen().getPreference(i));
		}
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // android HACK as MultiSelectListPreferences don't fire updates as they should
        Preference p = findPreference(key.split(DialogPreference.MultiSelectListPreferenceKey)[0]);
        setSummary(p);
    }
}