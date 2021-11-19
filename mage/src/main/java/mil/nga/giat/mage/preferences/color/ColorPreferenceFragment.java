package mil.nga.giat.mage.preferences.color;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public abstract class ColorPreferenceFragment extends PreferenceFragmentCompat {

    public static final String DIALOG_FRAGMENT_TAG = "ColorPreferenceFragment.DIALOG";

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        DialogFragment dialog = null;
        if (preference instanceof ColorPickerPreference) {
            dialog = new ColorPreferenceDialog((ColorPickerPreference)preference);
        }

        if (dialog != null) {
            dialog.setTargetFragment(this, 0);
            dialog.show(getParentFragmentManager(), DIALOG_FRAGMENT_TAG);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }
}
