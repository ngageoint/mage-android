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
import android.widget.CheckBox;
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

public class ObservationFilterActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
	private Integer timeFilter = 0;
	private Integer activeTimeFilter = 0;

	private Integer customTimeNumber = 0;
	private String customTimeUnit = "";

	private CheckBox favoriteCheckBox;
	private boolean activeFavoriteFilter = false;

	private CheckBox importantCheckBox;
	private boolean activeImportantFilter = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_observation_filter);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		timeFilter = activeTimeFilter = preferences.getInt(getResources().getString(R.string.activeTimeFilterKey), getResources().getInteger(R.integer.time_filter_last_month));
		customTimeNumber = preferences.getInt(getResources().getString(R.string.customObservationTimeNumberFilterKey), 0);
		customTimeUnit = preferences.getString(getResources().getString(R.string.customObservationTimeUnitFilterKey), getResources().getStringArray(R.array.timeUnitEntries)[0]);

		activeFavoriteFilter = preferences.getBoolean(getResources().getString(R.string.activeFavoritesFilterKey), false);
		activeImportantFilter = preferences.getBoolean(getResources().getString(R.string.activeImportantFilterKey), false);

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

		importantCheckBox = (CheckBox) findViewById(R.id.status_important);
		favoriteCheckBox = (CheckBox) findViewById(R.id.status_favorite);

		View view = findViewById(android.R.id.content).findViewWithTag(timeFilter.toString());
		if (view == null) {
			view = findViewById(android.R.id.content).findViewWithTag("0");
			timeFilter = activeTimeFilter = getResources().getInteger(R.integer.time_filter_last_month);
		}

		((RadioButton) view).setChecked(true);
		showOrHideCustomWindow();

		boolean favorite = preferences.getBoolean(getResources().getString(R.string.activeFavoritesFilterKey), false);
		favoriteCheckBox.setChecked(favorite);

		boolean important = preferences.getBoolean(getResources().getString(R.string.activeImportantFilterKey), false);
		importantCheckBox.setChecked(important);
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

	public void onFavoriteFilter(View view) {
		favoriteCheckBox.setChecked(!favoriteCheckBox.isChecked());
		setFilter();
	}

	public void onImportantFilter(View view) {
		importantCheckBox.setChecked(!importantCheckBox.isChecked());
		setFilter();
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

		editor.putInt(getResources().getString(R.string.customObservationTimeNumberFilterKey), customTimeNumber);
		editor.putString(getResources().getString(R.string.customObservationTimeUnitFilterKey), customTimeUnit);

		if (activeTimeFilter != timeFilter) {
			activeTimeFilter = timeFilter;
			editor.putInt(getResources().getString(R.string.activeTimeFilterKey), activeTimeFilter);
		}

		if (activeFavoriteFilter != favoriteCheckBox.isChecked()) {
			activeFavoriteFilter = favoriteCheckBox.isChecked();
			editor.putBoolean(getResources().getString(R.string.activeFavoritesFilterKey), activeFavoriteFilter);
		}

		if (activeImportantFilter != importantCheckBox.isChecked()) {
			activeImportantFilter = importantCheckBox.isChecked();
			editor.putBoolean(getResources().getString(R.string.activeImportantFilterKey), activeImportantFilter);
		}

		editor.apply();
	}
}
