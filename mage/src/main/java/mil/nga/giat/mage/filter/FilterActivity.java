package mil.nga.giat.mage.filter;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Filter");
        actionBar.setDisplayHomeAsUpEnabled(true);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    @Override
	public void onResume() {
		super.onResume();
		updateObservationFilterDescription();
		updateLocationFilterDescription();
	}

    private int getTimeFilterId() {
        return preferences.getInt(getResources().getString(R.string.activeTimeFilterKey), getResources().getInteger(R.integer.time_filter_none));
    }

    private int getLocationTimeFilterId() {
        return preferences.getInt(getResources().getString(R.string.activeLocationTimeFilterKey), getResources().getInteger(R.integer.time_filter_none));
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
