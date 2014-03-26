package mil.nga.giat.mage.preferences;

import mil.nga.giat.mage.R;
import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.widget.CompoundButton;
import android.widget.Switch;

/**
 * Provides configuration driven settings that are available to the user. Check
 * publicpreferences.xml for the configuration.
 * 
 * @author wiedemannse
 * 
 */
public class LocationPreferencesActivity extends PreferenceActivity {
	
	LocationPreferenceFragment preference = new LocationPreferenceFragment();
	
	public static class LocationPreferenceFragment extends PreferenceFragmentSummary implements CompoundButton.OnCheckedChangeListener {

	    private Switch locationSwitch;
	    
	    public void onCreate(final Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);

	        addPreferencesFromResource(R.xml.locationpreferences);
	        
	        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);

	        Activity activity = getActivity();
	        ActionBar actionbar = activity.getActionBar();
	        locationSwitch = new Switch(activity);

	        actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
	        actionbar.setCustomView(locationSwitch, new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.RIGHT));

	        actionbar.setTitle("Location Services");
	        actionbar.setLogo(R.drawable.ic_compass_white);
	        
	        updateSettings();
	    }
	   
	    public void onResume() {
	        super.onResume();
	        locationSwitch.setOnCheckedChangeListener(this);
	        updateSettings();
	    }

	    public void onPause() {
	        super.onPause();
	        locationSwitch.setOnCheckedChangeListener(null);
	    }

	    protected void updateSettings() {
	        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
	        boolean locationServiceEnabled = preferences.getBoolean("locationServiceEnabled", false);
	        locationSwitch.setChecked(locationServiceEnabled);
	        
	        int count = getPreferenceScreen().getPreferenceCount();
	        for (int i = 0; i < count; ++i) {
	            Preference pref = getPreferenceScreen().getPreference(i);
	            pref.setEnabled(locationServiceEnabled);
	            setSummary(getPreferenceScreen().getPreference(i));         
	        }
	    }

	    @Override
	    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
	        Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
	        editor.putBoolean("locationServiceEnabled", isChecked);
	        editor.commit();
	        	        
	        updateSettings();
	    }
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction().replace(android.R.id.content, preference).commit();
	}
}