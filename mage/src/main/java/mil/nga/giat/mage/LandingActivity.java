package mil.nga.giat.mage;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mil.nga.geopackage.validate.GeoPackageValidate;
import mil.nga.giat.mage.cache.GeoPackageCacheUtils;
import mil.nga.giat.mage.event.EventFragment;
import mil.nga.giat.mage.help.HelpFragment;
import mil.nga.giat.mage.login.AlertBannerFragment;
import mil.nga.giat.mage.login.LoginActivity;
import mil.nga.giat.mage.map.MapFragment;
import mil.nga.giat.mage.map.cache.CacheProvider;
import mil.nga.giat.mage.navigation.DrawerItem;
import mil.nga.giat.mage.newsfeed.ObservationFeedFragment;
import mil.nga.giat.mage.newsfeed.PeopleFeedFragment;
import mil.nga.giat.mage.preferences.GeneralPreferencesFragment;
import mil.nga.giat.mage.profile.ProfileFragment;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.RoleHelper;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

/**
 * This is the Activity that holds other fragments. Map, feeds, etc. It 
 * starts and stops much of the application. It also contains menus .
 * 
 */
public class LandingActivity extends Activity implements ListView.OnItemClickListener, CompoundButton.OnCheckedChangeListener {

    /**
     * Extra key for storing the local file path used to launch MAGE
     */
    public static final String EXTRA_OPEN_FILE_PATH = "extra_open_file_path";

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 100;
    private static final int PERMISSIONS_REQUEST_ACCESS_STORAGE= 200;
    private static final int PERMISSIONS_REQUEST_OPEN_FILE = 300;

    private static final String LOG_NAME = LandingActivity.class.getName();

    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;
    private DrawerItem currentActivity;

    private Integer timeFilter = 0;
    private Integer activeTimeFilter = 0;

    private CheckBox favoriteCheckBox;
    private boolean activeFavoriteFilter = false;

    private CheckBox importantCheckBox;
    private boolean activeImportantFilter = false;

    private String currentTitle = "";
    private DrawerItem mapItem;
	private int logoutId;
    private boolean switchFragment;
    private DrawerItem itemToSwitchTo;
    private boolean locationPermissionGranted = false;
    private Uri openUri;
    private String openPath;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        ((MAGE) getApplication()).onLogin();
        CacheProvider.getInstance(getApplicationContext()).refreshTileOverlays();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int[] timeFilterValues = getResources().getIntArray(R.array.timeFilterValues);
        timeFilter = activeTimeFilter = preferences.getInt(getResources().getString(R.string.activeTimeFilterKey), timeFilterValues[0]);
        if (findViewById(android.R.id.content).findViewWithTag(timeFilter.toString()) == null) {
            timeFilter = activeTimeFilter = timeFilterValues[0];
        }

        activeFavoriteFilter = preferences.getBoolean(getResources().getString(R.string.activeFavoritesFilterKey), false);
        activeImportantFilter = preferences.getBoolean(getResources().getString(R.string.activeImportantFilterKey), false);

        final RadioButton noneRadioButton = ((RadioButton) findViewById(R.id.none_radio));
        noneRadioButton.setOnCheckedChangeListener(this);
        findViewById(R.id.none_time_filter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCheckedChanged(noneRadioButton, false);
            }
        });

        final RadioButton todayRadioButton = ((RadioButton) findViewById(R.id.since_midnight_radio));
        todayRadioButton.setOnCheckedChangeListener(this);
        findViewById(R.id.today_time_filter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCheckedChanged(todayRadioButton, false);
            }
        });

        final RadioButton last24HoursRadioButton = ((RadioButton) findViewById(R.id.last_24_hours_radio));
        last24HoursRadioButton.setOnCheckedChangeListener(this);
        findViewById(R.id.last_24_hours_time_filter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCheckedChanged(last24HoursRadioButton, false);
            }
        });

        final RadioButton lastWeekRadioButton = ((RadioButton) findViewById(R.id.last_week_radio));
        lastWeekRadioButton.setOnCheckedChangeListener(this);
        findViewById(R.id.last_week_time_filter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCheckedChanged(lastWeekRadioButton, false);
            }
        });

        final RadioButton lastMonthRadioButton = ((RadioButton) findViewById(R.id.last_month_radio));
        lastMonthRadioButton.setOnCheckedChangeListener(this);
        findViewById(R.id.last_month_time_filter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCheckedChanged(lastMonthRadioButton, false);
            }
        });

        importantCheckBox = (CheckBox) findViewById(R.id.status_important);
        favoriteCheckBox = (CheckBox) findViewById(R.id.status_favorite);

        // Ask for permissions
        locationPermissionGranted = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!locationPermissionGranted) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(LandingActivity.this, R.style.AppCompatAlertDialogStyle)
                        .setTitle(R.string.location_access_rational_title)
                        .setMessage(R.string.location_access_rational_message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(LandingActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                            }
                        })
                        .create()
                        .show();

            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        }

        int id = 0;
        mapItem = new DrawerItem.Builder().id(id++).text("Map").drawableId(R.drawable.ic_map_white_24dp).fragment(new MapFragment()).build();
		DrawerItem logoutItem = new DrawerItem.Builder().id(id++).text("Logout").drawableId(R.drawable.ic_power_settings_new_white_24dp).build();
		logoutId = logoutItem.getId();

		List<DrawerItem> drawerItems = new ArrayList<>();
		drawerItems.add(mapItem);
		drawerItems.add(new DrawerItem.Builder().id(id++).text("Observations").drawableId(R.drawable.ic_place_white_24dp).fragment(new ObservationFeedFragment()).build());
		drawerItems.add(new DrawerItem.Builder().id(id++).text("People").drawableId(R.drawable.ic_people_white_24dp).fragment(new PeopleFeedFragment()).build());

		int numberOfEvents = EventHelper.getInstance(getApplicationContext()).getEventsForCurrentUser().size();
		try {
			if (UserHelper.getInstance(getApplicationContext()).readCurrentUser().getRole().equals(RoleHelper.getInstance(getApplicationContext()).readAdmin())) {
				// now that ADMINS can be part of any event
				numberOfEvents = EventHelper.getInstance(getApplicationContext()).readAll().size();
			}
		} catch(Exception e) {
			Log.e(LOG_NAME, "Problem pulling events for this admin.");
		}

		if(numberOfEvents > 1) {
			drawerItems.add(new DrawerItem.Builder().id(id++).text("Events").drawableId(R.drawable.ic_event_white_24dp).fragment(new EventFragment()).build());
		}
		drawerItems.add(new DrawerItem.Builder().id(id++).text("My Profile").drawableId(R.drawable.ic_person_white_24dp).fragment(new ProfileFragment()).build());
        drawerItems.add(new DrawerItem.Builder().id(id++).seperator(true).build());
        drawerItems.add(new DrawerItem.Builder().id(id++).text("Settings").drawableId(R.drawable.ic_settings_white_24dp).fragment(new GeneralPreferencesFragment()).build());
		drawerItems.add(new DrawerItem.Builder().id(id++).text("Help").drawableId(R.drawable.ic_help_outline_white_24dp).fragment(new HelpFragment()).build());
		drawerItems.add(logoutItem);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        drawerList.setAdapter(new ArrayAdapter<DrawerItem>(this, R.layout.drawer_list_item, drawerItems) {
            @Override
            public View getView(int position, View view, ViewGroup parent) {
                DrawerItem item = getItem(position);

                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    if (item.isSeperator()) {
                        view = inflater.inflate(R.layout.drawer_list_seperator_item, null);
                        view.setClickable(false);
                    } else {
                        view = inflater.inflate(R.layout.drawer_list_item, null);
                        if (item.getDrawableId() != null) {
                            ImageView iv = (ImageView) view.findViewById(R.id.drawer_item_icon);
                            iv.setImageResource(item.getDrawableId());
                        }

                        TextView text = (TextView) view.findViewById(R.id.text);
                        text.setText(item.getText());
                    }
                }

                return view;
            }
        });

        // Set the list's click listener
        drawerList.setOnItemClickListener(this);

        actionbarToggleHandler();

		if (savedInstanceState == null) {
			Fragment alertBannerFragment = new AlertBannerFragment();
            getFragmentManager().beginTransaction().add(android.R.id.content, alertBannerFragment).commit();
		}

        // Check if MAGE was launched with a local file
        openPath = getIntent().getStringExtra(EXTRA_OPEN_FILE_PATH);
        if (openPath != null) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    new android.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
                            .setTitle(R.string.cache_access_rational_title)
                            .setMessage(R.string.cache_access_rational_message)
                            .setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(LandingActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_OPEN_FILE);
                                }
                            })
                            .show();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_OPEN_FILE);
                }
            } else {
                // Else, store the path to pass to further intents
                handleOpenFilePath();
            }
        }

        goToMap();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (locationPermissionGranted != (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            locationPermissionGranted = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

            // notify location services that the permissions have changed.
            ((MAGE) getApplication()).getLocationService().onLocationPermissionsChanged();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                locationPermissionGranted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (locationPermissionGranted) {
                    ((MAGE) getApplication()).getLocationService().onLocationPermissionsChanged();
                }

                break;
            }
            case PERMISSIONS_REQUEST_ACCESS_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    CacheProvider.getInstance(getApplicationContext()).refreshTileOverlays();
                }

                break;
            }
            case PERMISSIONS_REQUEST_OPEN_FILE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    handleOpenFilePath();
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        // User denied storage with never ask again.  Since they will get here
                        // by opening a cache into MAGE, give them a dialog that will
                        // by opening a cache into MAGE, give them a dialog that will
                        // guide them to settings if they want to enable the permission
                        showDisabledPermissionsDialog(
                                getResources().getString(R.string.cache_access_title),
                                getResources().getString(R.string.cache_access_message));
                    }
                }

                break;
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int filter = Integer.parseInt(buttonView.getTag().toString());
        if (timeFilter != filter) {
            RadioButton oldFilterRadioButton = (RadioButton) findViewById(android.R.id.content).findViewWithTag(timeFilter.toString());
            oldFilterRadioButton.setChecked(false);

            timeFilter = filter;
            buttonView.setChecked(true);
        }
    }

    public void onFavoriteFilter(View view) {
        favoriteCheckBox.setChecked(!favoriteCheckBox.isChecked());
    }

    public void onImportantFilter(View view) {
        importantCheckBox.setChecked(!importantCheckBox.isChecked());
    }

    public void onTimeFilter(View view) {
        onCheckedChanged((CompoundButton) view, false);
    }

    private void showDisabledPermissionsDialog(String title, String message) {
        new android.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.settings, new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.fromParts("package", getApplicationContext().getPackageName(), null));
                        startActivity(intent);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void goToMap() {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, mapItem.getFragment()).commit();
        getActionBar().setTitle("MAGE");
        currentActivity = mapItem;
    }

    private void actionbarToggleHandler() {
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                getActionBar().setTitle(currentTitle);

                if (drawerView.getId() == R.id.filter_drawer) {
                    setFilter();
                } else if (drawerView.getId() == R.id.left_drawer && switchFragment) {
                    switchFragment = false;

                    // Insert the fragment by replacing any existing fragment
                    FragmentManager fragmentManager = getFragmentManager();
                    fragmentManager.beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).add(R.id.content_frame, itemToSwitchTo.getFragment()).commit();
                    currentActivity = itemToSwitchTo;
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                // hide keyboard
				InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
				if (getCurrentFocus() != null) {
					inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
				}
                currentTitle = (String) getActionBar().getTitle();
                if (drawerView.getId() == R.id.filter_drawer) {
                    getActionBar().setTitle("Filter");

                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                    int[] timeFilterValues = getResources().getIntArray(R.array.timeFilterValues);
                    Integer checkedFilter = preferences.getInt(getResources().getString(R.string.activeTimeFilterKey), timeFilterValues[0]);
                    View view = findViewById(android.R.id.content).findViewWithTag(checkedFilter.toString());
                    if (view == null) {
                        view = findViewById(android.R.id.content).findViewWithTag("0");
                    }

                    ((RadioButton) view).setChecked(true);

                    boolean favorite = preferences.getBoolean(getResources().getString(R.string.activeFavoritesFilterKey), false);
                    CheckBox favoriteCheckBox = (CheckBox) findViewById(R.id.status_favorite);
                    favoriteCheckBox.setChecked(favorite);

                    boolean important = preferences.getBoolean(getResources().getString(R.string.activeImportantFilterKey), false);
                    CheckBox importantCheckBox = (CheckBox) findViewById(R.id.status_important);
                    importantCheckBox.setChecked(important);
                }
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                invalidateOptionsMenu();
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);
    }

    public void filterOkClick(View v) {
        drawerLayout.closeDrawer(findViewById(R.id.filter_drawer));
    }

    public void setFilter() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();

        if (activeTimeFilter != timeFilter) {
            activeTimeFilter = timeFilter;
            editor.putInt(getResources().getString(R.string.activeTimeFilterKey), activeTimeFilter);
        }

        CheckBox favorite = (CheckBox) findViewById(R.id.status_favorite);
        if (activeFavoriteFilter != favorite.isChecked()) {
            activeFavoriteFilter = favorite.isChecked();
            editor.putBoolean(getResources().getString(R.string.activeFavoritesFilterKey), activeFavoriteFilter);
        }

        CheckBox important = (CheckBox) findViewById(R.id.status_important);
        if (activeImportantFilter != important.isChecked()) {
            activeImportantFilter = important.isChecked();
            editor.putBoolean(getResources().getString(R.string.activeImportantFilterKey), activeImportantFilter);
        }

        editor.commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        drawerToggle.syncState();
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (currentActivity != mapItem) {
                goToMap();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            // drawer handled the event
            return true;
        }

        switch (item.getItemId()) {
        case R.id.filter_button:
            DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            View filterDrawer = findViewById(R.id.filter_drawer);
            if (!drawerLayout.isDrawerOpen(filterDrawer)) {
                drawerLayout.openDrawer(filterDrawer);
            } else {
                drawerLayout.closeDrawer(filterDrawer);
            }
            break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = drawerLayout.isDrawerOpen(drawerList);
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setVisible(!drawerOpen);
        }
        
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Takes you to the home screen
     */
    @Override
    public void onBackPressed() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        ArrayAdapter<DrawerItem> adapter = (ArrayAdapter<DrawerItem>) adapterView.getAdapter();
        itemToSwitchTo = adapter.getItem(position);
        if (itemToSwitchTo.getFragment() == null) {
            if(itemToSwitchTo.getId() == logoutId) {
                ((MAGE)getApplication()).onLogout(true, new MAGE.OnLogoutListener() {
                    @Override
                    public void onLogout() {
                        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                        finish();
                    }
                });

                return;
            } else {
				Log.e(LOG_NAME, "Your fragment was null. Fix the code.");
            }
        }
        if (currentActivity != itemToSwitchTo && itemToSwitchTo.getFragment() != null) {
            switchFragment = true;

            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().remove(currentActivity.getFragment()).commit();
        }

        // Highlight the selected item, update the title, and close the drawer
        drawerList.setItemChecked(position, true);
        drawerLayout.closeDrawer(drawerList);
    }

    /**
     * Handle opening the file path that MAGE was launched with
     */
    private void handleOpenFilePath() {

        File cacheFile = new File(openPath);

        // Handle GeoPackage files by linking them to their current location
        if (GeoPackageValidate.hasGeoPackageExtension(cacheFile)) {

            String cacheName = GeoPackageCacheUtils.importGeoPackage(this, cacheFile);
            if (cacheName != null) {
                CacheProvider.getInstance(getApplicationContext()).enableAndRefreshTileOverlays(cacheName);
            }
        }
    }

	public static void deleteAllData(Context context) {
		DaoStore.getInstance(context).resetDatabase();
		PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
		deleteDir(MediaUtility.getMediaStageDirectory(context));
		clearApplicationData(context);
	}

	public static void clearApplicationData(Context context) {
		File cache = context.getCacheDir();
		File appDir = new File(cache.getParent());
		if (appDir.exists()) {
			String[] children = appDir.list();
			for (String s : children) {
				if (!s.equals("lib") && !s.equals("databases")) {
					File f = new File(appDir, s);
					Log.d(LOG_NAME, "Deleting " + f.getAbsolutePath());
					deleteDir(f);
				}
			}
		}

       deleteDir(MediaUtility.getMediaStageDirectory(context));
    }

	public static boolean deleteDir(File dir) {
		if (dir != null && dir.isDirectory()) {
			String[] children = dir.list();
			for (String kid : children) {
				boolean success = deleteDir(new File(dir, kid));
				if (!success) {
					return false;
				}
			}
		}
		if(dir == null) {
			return true;
		}
		return dir.delete();
	}

}