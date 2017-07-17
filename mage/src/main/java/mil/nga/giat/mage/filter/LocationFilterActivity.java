package mil.nga.giat.mage.filter;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;

import mil.nga.giat.mage.R;

/**
 * Created by barela on 7/14/17.
 */

public class LocationFilterActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

	private Integer timeFilter = 0;
	private Integer activeTimeFilter = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_location_filter);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		timeFilter = activeTimeFilter = preferences.getInt(getResources().getString(R.string.activeLocationTimeFilterKey), getResources().getInteger(R.integer.time_filter_none));

		final RadioButton noneRadioButton = ((RadioButton) findViewById(R.id.none_radio));
		findViewById(R.id.none_time_filter).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onCheckedChanged(noneRadioButton, false);
			}
		});

		final RadioButton todayRadioButton = ((RadioButton) findViewById(R.id.since_midnight_radio));
		findViewById(R.id.today_time_filter).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onCheckedChanged(todayRadioButton, false);
			}
		});

		final RadioButton last24HoursRadioButton = ((RadioButton) findViewById(R.id.last_24_hours_radio));
		findViewById(R.id.last_24_hours_time_filter).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onCheckedChanged(last24HoursRadioButton, false);
			}
		});

		final RadioButton lastWeekRadioButton = ((RadioButton) findViewById(R.id.last_week_radio));
		findViewById(R.id.last_week_time_filter).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onCheckedChanged(lastWeekRadioButton, false);
			}
		});

		final RadioButton lastMonthRadioButton = ((RadioButton) findViewById(R.id.last_month_radio));
		findViewById(R.id.last_month_time_filter).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onCheckedChanged(lastMonthRadioButton, false);
			}
		});

		View view = findViewById(android.R.id.content).findViewWithTag(timeFilter.toString());
		if (view == null) {
			view = findViewById(android.R.id.content).findViewWithTag("0");
			timeFilter = activeTimeFilter = getResources().getInteger(R.integer.time_filter_none);
		}

		((RadioButton) view).setChecked(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				this.finish();
				return true;
		}
		return true;
	}

	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		int filter = Integer.parseInt(buttonView.getTag().toString());
		((RadioButton) findViewById(android.R.id.content).findViewWithTag(timeFilter.toString())).setChecked(false);
		timeFilter = filter;
		buttonView.setChecked(true);
		setFilter();
	}

	private void setFilter() {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();

		if (activeTimeFilter != timeFilter) {
			activeTimeFilter = timeFilter;
			editor.putInt(getResources().getString(R.string.activeLocationTimeFilterKey), activeTimeFilter);
		}

		editor.commit();
	}
}
