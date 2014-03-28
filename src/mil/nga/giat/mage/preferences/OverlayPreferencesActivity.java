package mil.nga.giat.mage.preferences;

import mil.nga.giat.mage.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class OverlayPreferencesActivity extends PreferenceActivity {

    OverlayPreferenceFragment preference = new OverlayPreferenceFragment();

    public static class OverlayPreferenceFragment extends PreferenceFragmentSummary {

        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.overlaypreferences);

            PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);

//            ActionBar actionbar = getActivity().getActionBar();
//            actionbar.setTitle("Overlay Maps");
//            actionbar.setLogo(R.drawable.ic_map_white);
            
            updateSettings();
        }
        
        @Override
        public void onResume() {
            super.onResume();
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        
        protected void updateSettings() {
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, preference).commit();
    }
}