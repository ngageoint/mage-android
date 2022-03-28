package mil.nga.giat.mage.preferences.color

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

abstract class ColorPreferenceFragment : PreferenceFragmentCompat() {
   override fun onDisplayPreferenceDialog(preference: Preference) {
      val dialog = if (preference is ColorPickerPreference) {
          ColorPreferenceDialog(preference)
      } else null

      if (dialog != null) {
         dialog.setTargetFragment(this, 0)
         dialog.show(parentFragmentManager, DIALOG_FRAGMENT_TAG)
      } else {
         super.onDisplayPreferenceDialog(preference)
      }
   }

   companion object {
      const val DIALOG_FRAGMENT_TAG = "ColorPreferenceFragment.DIALOG"
   }
}