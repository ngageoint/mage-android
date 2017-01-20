package mil.nga.giat.mage.filter;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;

import mil.nga.giat.mage.R;

/**
 * Created by wnewman on 1/25/17.
 */

public class FilterActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    private Integer timeFilter = 0;
    private Integer activeTimeFilter = 0;

    private CheckBox favoriteCheckBox;
    private boolean activeFavoriteFilter = false;

    private CheckBox importantCheckBox;
    private boolean activeImportantFilter = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Filter");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        timeFilter = activeTimeFilter = preferences.getInt(getResources().getString(R.string.activeTimeFilterKey), getResources().getInteger(R.integer.time_filter_none));
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

        importantCheckBox = (CheckBox) findViewById(R.id.status_important);
        favoriteCheckBox = (CheckBox) findViewById(R.id.status_favorite);

        View view = findViewById(android.R.id.content).findViewWithTag(timeFilter.toString());
        if (view == null) {
            view = findViewById(android.R.id.content).findViewWithTag("0");
            timeFilter = activeTimeFilter = getResources().getInteger(R.integer.time_filter_none);
        }

        ((RadioButton) view).setChecked(true);

        boolean favorite = preferences.getBoolean(getResources().getString(R.string.activeFavoritesFilterKey), false);
        favoriteCheckBox.setChecked(favorite);

        boolean important = preferences.getBoolean(getResources().getString(R.string.activeImportantFilterKey), false);
        importantCheckBox.setChecked(important);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        getMenuInflater().inflate(R.menu.filter_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.filter_button:
                setFilter();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onFavoriteFilter(View view) {
        favoriteCheckBox.setChecked(!favoriteCheckBox.isChecked());
    }

    public void onImportantFilter(View view) {
        importantCheckBox.setChecked(!importantCheckBox.isChecked());
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int filter = Integer.parseInt(buttonView.getTag().toString());
        ((RadioButton) findViewById(android.R.id.content).findViewWithTag(timeFilter.toString())).setChecked(false);
        timeFilter = filter;
        buttonView.setChecked(true);
    }

    private void setFilter() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();

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

        editor.commit();

        finish();
    }
}
