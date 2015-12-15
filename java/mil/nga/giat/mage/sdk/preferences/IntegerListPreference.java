package mil.nga.giat.mage.sdk.preferences;

import android.content.Context;
import android.preference.ListPreference;
import android.preference.Preference;
import android.util.AttributeSet;

/**
 * A {@link Preference} that displays a list of entries as
 * a dialog.
 * <p/>
 * This preference will store an int into the SharedPreferences.
 */

public class IntegerListPreference extends ListPreference {

	public IntegerListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public IntegerListPreference(Context context) {
		this(context, null);
	}

	@Override
	protected boolean persistString(String value) {
		if (value == null) {
			return false;
		} else {
			return persistInt(Integer.valueOf(value));
		}
	}

	@Override
	protected String getPersistedString(String defaultReturnValue) {
		if (getSharedPreferences().contains(getKey())) {
			int intValue = getPersistedInt(0);
			return String.valueOf(intValue);
		} else {
			return defaultReturnValue;
		}
	}
}