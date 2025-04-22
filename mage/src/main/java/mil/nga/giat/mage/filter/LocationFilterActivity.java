package mil.nga.giat.mage.filter;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import mil.nga.giat.mage.R;

/**
 * Created by barela on 7/14/17.
 */

public class LocationFilterActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

	private Integer timeFilter = 0;
	private Integer activeTimeFilter = 0;

	private Integer customTimeNumber = 0;
	private String customTimeUnit = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_location_filter);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		timeFilter = activeTimeFilter = preferences.getInt(getResources().getString(R.string.activeLocationTimeFilterKey), getResources().getInteger(R.integer.time_filter_last_month));
		customTimeNumber = preferences.getInt(getResources().getString(R.string.customLocationTimeNumberFilterKey), 0);
		customTimeUnit = preferences.getString(getResources().getString(R.string.customLocationTimeUnitFilterKey), getResources().getStringArray(R.array.timeUnitEntries)[0]);

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

		final RadioButton customRadioButton = ((RadioButton) findViewById(R.id.custom_radio));
		findViewById(R.id.custom_time_filter).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onCheckedChanged(customRadioButton, false);
			}
		});

		final EditText customNumberField = ((EditText) findViewById(R.id.custom_input_time_number));
		customNumberField.setText(Integer.toString(customTimeNumber));
		customNumberField.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				try {
					customTimeNumber = Integer.parseInt(s.toString());
					onCheckedChanged(customRadioButton, false);
				} catch (NumberFormatException nfe) {

				}

			}
		});

		final Spinner customTimeUnitField = ((Spinner) findViewById(R.id.custom_input_time_unit));
		customTimeUnitField.setSelection(((ArrayAdapter)customTimeUnitField.getAdapter()).getPosition(customTimeUnit));
		customTimeUnitField.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				customTimeUnit = getResources().getStringArray(R.array.timeUnitEntries)[position];
				setFilter();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

		View view = findViewById(android.R.id.content).findViewWithTag(timeFilter.toString());
		if (view == null) {
			view = findViewById(android.R.id.content).findViewWithTag("0");
			timeFilter = activeTimeFilter = getResources().getInteger(R.integer.time_filter_last_month);
		}

		showOrHideCustomWindow();

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

	public void showOrHideCustomWindow() {
		if (timeFilter == getResources().getInteger(R.integer.time_filter_custom)) {
			findViewById(R.id.custom_window_view).setVisibility(View.VISIBLE);
			final ScrollView sv = (ScrollView)findViewById(R.id.scrollView);
			sv.post(new Runnable() {
				public void run() {
					sv.scrollTo(0, sv.getBottom());
				}
			});
		} else {
			findViewById(R.id.custom_window_view).setVisibility(View.GONE);
		}
	}

	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		int filter = Integer.parseInt(buttonView.getTag().toString());
		((RadioButton) findViewById(android.R.id.content).findViewWithTag(timeFilter.toString())).setChecked(false);
		timeFilter = filter;
		buttonView.setChecked(true);
		showOrHideCustomWindow();
		setFilter();
	}

	private void setFilter() {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
		editor.putInt(getResources().getString(R.string.customLocationTimeNumberFilterKey), customTimeNumber);
		editor.putString(getResources().getString(R.string.customLocationTimeUnitFilterKey), customTimeUnit);

		if (activeTimeFilter != timeFilter) {
			activeTimeFilter = timeFilter;
			editor.putInt(getResources().getString(R.string.activeLocationTimeFilterKey), activeTimeFilter);
		}

		editor.apply();
	}
}
