package mil.nga.giat.mage.preferences;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceFragmentCompat;

import mil.nga.giat.mage.R;

public class FetchPreferencesActivity extends AppCompatActivity {

    private final FetchPreferenceFragment preference = new FetchPreferenceFragment();

    private Toolbar subToolbarWithSwitch;
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

        Toolbar mainToolbar = findViewById(R.id.data_fetching_toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(mainToolbar, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left,0,insets.right, 0);
            return windowInsets;
        });

        setSupportActionBar(mainToolbar);

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        subToolbarWithSwitch = findViewById(R.id.sub_toolbar);
        subToolbarWithSwitch.inflateMenu(R.menu.fetch_preferences_menu);

        ViewCompat.setOnApplyWindowInsetsListener(subToolbarWithSwitch, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left,0,insets.right, 0);
            return windowInsets;
        });

        noContentView = findViewById(R.id.no_content_frame);

        boolean fetchEnabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getResources().getString(R.string.dataFetchEnabledKey), getResources().getBoolean(R.bool.dataFetchEnabledDefaultValue));

        SwitchCompat dataEnabledSwitch = subToolbarWithSwitch.findViewById(R.id.toolbar_switch);
        dataEnabledSwitch.setChecked(fetchEnabled);
        dataEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceManager.getDefaultSharedPreferences(FetchPreferencesActivity.this).edit().putBoolean(getResources().getString(R.string.dataFetchEnabledKey), isChecked).apply();
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
        subToolbarWithSwitch.setTitle(fetchEnabled ? "On" : "Off");
        noContentView.setVisibility(fetchEnabled ? View.GONE : View.VISIBLE);
    }
}