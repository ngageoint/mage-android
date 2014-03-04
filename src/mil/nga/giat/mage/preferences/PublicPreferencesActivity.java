package mil.nga.giat.mage.preferences;

import mil.nga.giat.mage.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Provides configuration driven settings that are available to the user
 * 
 * @author wiedemannse
 * 
 */
public class PublicPreferencesActivity extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.publicpreferences);
	}
}
