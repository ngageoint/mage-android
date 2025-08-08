package mil.nga.giat.mage.filter;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.R;

/**
 * Created by wnewman on 1/25/17.
 */

public class FilterActivity extends AppCompatActivity {

    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_filter);

		Toolbar toolbar = findViewById(R.id.filter_toolbar);
		ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, windowInsets) -> {
			Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
			v.setPadding(insets.left,0,insets.right, 0);
			return windowInsets;
		});

		setSupportActionBar(toolbar);

		getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		View scrollView = findViewById(R.id.filter_scrollview);
		ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, windowInsets) -> {
			Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
			v.setPadding(insets.left,0,insets.right, insets.bottom);
			return windowInsets;
		});

		preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    @Override
	public void onResume() {
		super.onResume();
		updateObservationFilterDescription();
		updateLocationFilterDescription();
	}

    private int getTimeFilterId() {
        return preferences.getInt(getResources().getString(R.string.activeTimeFilterKey), getResources().getInteger(R.integer.time_filter_last_month));
    }

    private int getLocationTimeFilterId() {
        return preferences.getInt(getResources().getString(R.string.activeLocationTimeFilterKey), getResources().getInteger(R.integer.time_filter_last_month));
    }

    private void updateObservationFilterDescription() {
        List<String> filters = new ArrayList<>();

		int filterId = getTimeFilterId();

		if (filterId == getResources().getInteger(R.integer.time_filter_last_month)) {
			filters.add("Last Month");
		} else if (filterId == getResources().getInteger(R.integer.time_filter_last_week)) {
			filters.add("Last Week");
		} else if (filterId == getResources().getInteger(R.integer.time_filter_last_24_hours)) {
			filters.add("Last 24 Hours");
		} else if (filterId == getResources().getInteger(R.integer.time_filter_today)) {
			filters.add("Since Midnight");
		} else if (filterId == getResources().getInteger(R.integer.time_filter_custom)) {
			int timeNumber = preferences.getInt(getResources().getString(R.string.customObservationTimeNumberFilterKey), 0);
			String timeUnit = preferences.getString(getResources().getString(R.string.customObservationTimeUnitFilterKey), getResources().getStringArray(R.array.timeUnitEntries)[0]);
			filters.add("Last " + timeNumber + " " + timeUnit);
		}

		List<String> actionFilters = new ArrayList<>();
		if (preferences.getBoolean(getResources().getString(R.string.activeFavoritesFilterKey), false)) {
			actionFilters.add("Favorites");
		}

		if (preferences.getBoolean(getResources().getString(R.string.activeImportantFilterKey), false)) {
			actionFilters.add("Important");
		}

		if (!actionFilters.isEmpty()) {
			filters.add(StringUtils.join(actionFilters, " & "));
		}

		String filter = StringUtils.join(filters, ", ");
        if (filter.equalsIgnoreCase("")) {
            filter = "All";
        }
        ((TextView) findViewById(R.id.observation_filter_description)).setText(filter);
    }

    private void updateLocationFilterDescription() {
		String filter = "All";
		int filterId = getLocationTimeFilterId();

		if (filterId == getResources().getInteger(R.integer.time_filter_last_month)) {
			filter = "Last Month";
		} else if (filterId == getResources().getInteger(R.integer.time_filter_last_week)) {
			filter = "Last Week";
		} else if (filterId == getResources().getInteger(R.integer.time_filter_last_24_hours)) {
			filter = "Last 24 Hours";
		} else if (filterId == getResources().getInteger(R.integer.time_filter_today)) {
			filter = "Since Midnight";
		} else if (filterId == getResources().getInteger(R.integer.time_filter_custom)) {
			int timeNumber = preferences.getInt(getResources().getString(R.string.customLocationTimeNumberFilterKey), 0);
			String timeUnit = preferences.getString(getResources().getString(R.string.customLocationTimeUnitFilterKey), getResources().getStringArray(R.array.timeUnitEntries)[0]);
			filter = "Last " + timeNumber + " " + timeUnit;
		}
		((TextView) findViewById(R.id.location_filter_description)).setText(filter);
	}

    public void onObservationFilterClick(View view) {
        Intent intent = new Intent(this, ObservationFilterActivity.class);
        startActivity(intent);
    }

    public void onLocationFilterClick(View view) {
        Intent intent = new Intent(this, LocationFilterActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

}
