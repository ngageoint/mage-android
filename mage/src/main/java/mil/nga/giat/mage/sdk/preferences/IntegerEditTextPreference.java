package mil.nga.giat.mage.sdk.preferences;

import android.content.Context;
import androidx.preference.EditTextPreference;
import android.util.AttributeSet;

public class IntegerEditTextPreference extends EditTextPreference {

	public IntegerEditTextPreference(Context context) {
		super(context);
	}

	public IntegerEditTextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public IntegerEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected String getPersistedString(String defaultReturnValue) {
		int defaultValue = -1;
		try {
			defaultValue = Integer.valueOf(defaultReturnValue);
		} catch(NumberFormatException e) {

		}

		return String.valueOf(getPersistedInt(defaultValue));
	}

	@Override
	protected boolean persistString(String value) {
		return persistInt(Integer.valueOf(value));
	}
}