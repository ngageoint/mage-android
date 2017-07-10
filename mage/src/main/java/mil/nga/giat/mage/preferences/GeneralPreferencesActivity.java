package mil.nga.giat.mage.preferences;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import mil.nga.giat.mage.R;


public class GeneralPreferencesActivity extends AppCompatActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_general_preferences);

		getSupportActionBar().setTitle("Settings");

		getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new GeneralPreferencesFragment()).commit();
	}

	public static class GeneralPreferencesFragment extends PreferenceFragmentCompat {

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			addPreferencesFromResource(R.xml.generalpreferences);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.AppTheme_PrimaryAccent);
			LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);
			return super.onCreateView(localInflater, container, savedInstanceState);
		}
	}
}

