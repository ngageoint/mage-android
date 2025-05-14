package mil.nga.giat.mage.preferences;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.preferences.color.ColorPreferenceFragment;
import mil.nga.giat.mage.utils.DialogUtils;
import mil.nga.giat.mage.utils.NotificationUtils;
import mil.nga.giat.mage.utils.ThemeUtils;


public class GeneralPreferencesActivity extends AppCompatActivity implements
		PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_general_preferences);

		getSupportActionBar().setTitle("Settings");
		getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new GeneralPreferencesFragment()).commit();
	}

	@Override
	public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
		// Instantiate the new Fragment
		final Bundle args = pref.getExtras();
		final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(getClassLoader(), pref.getFragment());
		fragment.setArguments(args);
		fragment.setTargetFragment(caller, 0);
		// Replace the existing Fragment with the new Fragment
		getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).addToBackStack(null).commit();

		return true;
	}

	public static class NavigationPreferencesFragment extends ColorPreferenceFragment {
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.navigationpreferences, rootKey);
		}
	}


	public static class GeneralPreferencesFragment extends PreferenceFragmentCompat {
		private Preference dayNightThemePreference;
		private Preference notificationPreference;

		ActivityResultLauncher<String> requestNotificationPermissionLauncher  = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
			if (isGranted) {
				//permission granted, update the notifications switch state to enabled
				Preference notificationPreference = findPreference(getResources().getString(R.string.notificationsEnabledKey));
				if (notificationPreference != null) {
					//set switch to enabled and save to preferences
					SwitchPreferenceCompat notificationSwitchPref = (SwitchPreferenceCompat) notificationPreference;
					notificationSwitchPref.setChecked(true);
					notificationPreference.getSharedPreferences().edit().putBoolean(getResources().getString(R.string.notificationsEnabledKey), true).apply();
				}

			} else {
				//permission denied, show dialog for instructions to enable the permission under application settings
				DialogUtils.INSTANCE.showDialogForDisabledPermission(requireActivity(), getResources().getString(R.string.notifications_denied_title), getResources().getString(R.string.notifications_denied_message));
			}
		});

		Preference.OnPreferenceChangeListener themeChangeListener = (preference, newValue) -> {
			int themeCode = Integer.parseInt(newValue.toString());
			ThemeUtils.INSTANCE.updateUiWithDayNightTheme(themeCode);

			getActivity().recreate();
			return true;
		};

		Preference.OnPreferenceChangeListener notificationChangeListener = (preference, newValue) -> {
			Boolean isSwitchEnablementRequested = (Boolean) newValue;
			Boolean allowSwitchStateToBeSaved = true;

			//verify that the necessary permissions are in place before allowing the notifications switch to be enabled
			if (isSwitchEnablementRequested) {
				allowSwitchStateToBeSaved = NotificationUtils.INSTANCE.isNotificationsPermissionGranted(requireContext());
				if (!allowSwitchStateToBeSaved) {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
						//for Android 13 and above, the notifications permission must be requested
						requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
					} else {
						//for Android versions below 13, show dialog for instructions to enable notifications under application settings
						DialogUtils.INSTANCE.showDialogForDisabledPermission(requireActivity(), getResources().getString(R.string.notifications_denied_title), getResources().getString(R.string.notifications_denied_message));
					}
				}
			}

			return allowSwitchStateToBeSaved;
		};

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			addPreferencesFromResource(R.xml.generalpreferences);

			dayNightThemePreference = findPreference(getResources().getString(R.string.dayNightThemeKey));
			notificationPreference = findPreference(getResources().getString(R.string.notificationsEnabledKey));
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.AppTheme);
			LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);
			return super.onCreateView(localInflater, container, savedInstanceState);
		}

		@Override
		public void onResume() {
			super.onResume();

			dayNightThemePreference.setOnPreferenceChangeListener(themeChangeListener);
			notificationPreference.setOnPreferenceChangeListener(notificationChangeListener);

			//if the notifications preference is enabled but the notifications permission is not granted, disable it
			//this handles the case where a user revoked the permission in system settings after previously granting it
			SwitchPreferenceCompat notificationSwitchPref = (SwitchPreferenceCompat) notificationPreference;
			if (notificationSwitchPref.isChecked()) {
				boolean isNotificationsPermissionGranted = NotificationUtils.INSTANCE.isNotificationsPermissionGranted(requireContext());

				if (!isNotificationsPermissionGranted) {
					notificationSwitchPref.setChecked(false);
					notificationPreference.getSharedPreferences().edit().putBoolean(getResources().getString(R.string.notificationsEnabledKey), false).apply();
				}
			}
		}

		@Override
		public void onPause() {
			super.onPause();

			dayNightThemePreference.setOnPreferenceChangeListener(null);
			notificationPreference.setOnPreferenceChangeListener(null);
		}

	}
}

