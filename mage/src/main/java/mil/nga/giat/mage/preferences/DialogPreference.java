package mil.nga.giat.mage.preferences;

import mil.nga.giat.mage.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.MultiSelectListPreference;
import android.util.AttributeSet;

public class DialogPreference extends MultiSelectListPreference {

	public static String MultiSelectListPreferenceKey = ":MultiSelectListPreferenceChange";
	
	public DialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.map_preference);
	}

	@Override
	protected void onClick() {
		// Don't let users click the actual preference to changed the switch state
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		
		if (!positiveResult) return;
		
		// HACK, for some reason android is not recognizing that the MultiSelectListPreference
		SharedPreferences sharedPreferences = getSharedPreferences();
		String key = getKey();
		Editor e = sharedPreferences.edit();
		e.putLong(key + MultiSelectListPreferenceKey, System.currentTimeMillis());
		e.apply();
	}
}