package mil.nga.giat.mage.preferences;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import mil.nga.giat.mage.R;

public class FetchPreferencesActivity extends AppCompatActivity {

    private final FetchPreferenceFragment preference = new FetchPreferenceFragment();

    private Toolbar toolbar;
    private View noContentView;

    public static class FetchPreferenceFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.fetchpreferences);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.AppTheme);
            LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);
            return super.onCreateView(localInflater, container, savedInstanceState);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fetch_preferences);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.fetch_preferences_menu);

        noContentView = findViewById(R.id.no_content_frame);

        boolean fetchEnabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getResources().getString(R.string.dataFetchEnabledKey), getResources().getBoolean(R.bool.dataFetchEnabledDefaultValue));

        SwitchCompat dataEnabledSwitch = (SwitchCompat) toolbar.findViewById(R.id.toolbar_switch);
        dataEnabledSwitch.setChecked(fetchEnabled);
        dataEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceManager.getDefaultSharedPreferences(FetchPreferencesActivity.this).edit().putBoolean(getResources().getString(R.string.dataFetchEnabledKey), isChecked).commit();
                updateView(isChecked);
            }
        });

        updateView(fetchEnabled);

        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, preference).commit();
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

    private void updateView(boolean fetchEnabled) {
        toolbar.setTitle(fetchEnabled ? "On" : "Off");
        noContentView.setVisibility(fetchEnabled ? View.GONE : View.VISIBLE);
    }
}