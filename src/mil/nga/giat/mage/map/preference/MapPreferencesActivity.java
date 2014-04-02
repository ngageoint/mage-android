package mil.nga.giat.mage.map.preference;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.preferences.PreferenceFragmentSummary;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

/**
 * Provides map configuration driven settings that are available to the user.
 * Check mappreferences.xml for the configuration.
 * 
 * @author newmanw
 * 
 */
public class MapPreferencesActivity extends PreferenceActivity {

    MapPreferenceFragment preference = new MapPreferenceFragment();

    public static class MapPreferenceFragment extends PreferenceFragmentSummary {
        MultiSelectListPreference overlayPreference;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.mappreferences);
        }

        @Override
        public void onResume() {
            super.onResume();
            
            for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
                Preference preference = getPreferenceScreen().getPreference(i);
                setSummary(preference);
            }
            
            OverlayPreference p = (OverlayPreference) findPreference("mapTileOverlays");
            StringBuffer summary = new StringBuffer();
            Iterator<String> iterator = p.getValues().iterator();
            while (iterator.hasNext()) {
                String value = iterator.next();
                summary.append(value);
                
                if (iterator.hasNext())
                    summary.append("\n");
            }
            
            p.setSummary(summary);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, preference).commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case OverlayPreference.OVERLAY_ACTIVITY: {
            if (resultCode == Activity.RESULT_OK) {
                Set<String> overlays = new HashSet<String>(data.getStringArrayListExtra(OverlayPreference.OVERLAY_EXTENDED_DATA_KEY));
                OverlayPreference p = (OverlayPreference) preference.findPreference("mapTileOverlays");
                p.setValues(overlays);
            }
            break;
        }
        default:
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}