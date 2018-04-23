package mil.nga.giat.mage;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mil.nga.geopackage.validate.GeoPackageValidate;
import mil.nga.giat.mage.cache.GeoPackageCacheUtils;
import mil.nga.giat.mage.event.ChangeEventActivity;
import mil.nga.giat.mage.help.HelpActivity;
import mil.nga.giat.mage.login.LoginActivity;
import mil.nga.giat.mage.map.MapFragment;
import mil.nga.giat.mage.map.cache.CacheProvider;
import mil.nga.giat.mage.newsfeed.ObservationFeedFragment;
import mil.nga.giat.mage.newsfeed.PeopleFeedFragment;
import mil.nga.giat.mage.preferences.GeneralPreferencesActivity;
import mil.nga.giat.mage.profile.ProfileActivity;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.RoleHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.datastore.user.UserLocal;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

/**
 * This is the Activity that holds other fragments. Map, feeds, etc. It
 * starts and stops much of the application. It also contains menus .
 *
 */
public class LandingActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    /**
     * Extra key for storing the local file path used to launch MAGE
     */
    public static final String EXTRA_OPEN_FILE_PATH = "extra_open_file_path";

    private static final String BOTTOM_NAVIGATION_ITEM = "BOTTOM_NAVIGATION_ITEM";

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 100;
    private static final int PERMISSIONS_REQUEST_ACCESS_STORAGE= 200;
    private static final int PERMISSIONS_REQUEST_OPEN_FILE = 300;
    private static final int AUTHENTICATE_REQUEST = 400;
    private static final int CHANGE_EVENT_REQUEST = 500;

    private int currentNightMode;

    private static final String LOG_NAME = LandingActivity.class.getName();

    private NavigationView navigationView;

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigationView;
    private List<Fragment> bottomNavigationFragments = new ArrayList<>();

    private boolean locationPermissionGranted = false;
    private Uri openUri;
    private String openPath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        bottomNavigationFragments.add(new MapFragment());
        bottomNavigationFragments.add(new ObservationFeedFragment());
        bottomNavigationFragments.add(new PeopleFeedFragment());

        // TODO investigate moving this call
        // its here because this is the first activity started after login and it ensures
        // the user has selected an event.  However there are other instances that could
        // bring the user back to this activity in which this has already been called,
        // i.e. after TokenExpiredActivity.
        ((MAGE) getApplication()).onLogin();

        CacheProvider.getInstance(getApplicationContext()).refreshTileOverlays();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle();

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

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.navigation);
        navigationView.setNavigationItemSelectedListener(this);

        try {
            int numberOfEvents = EventHelper.getInstance(getApplicationContext()).readAll().size();
            if (UserHelper.getInstance(getApplicationContext()).readCurrentUser().getRole().equals(RoleHelper.getInstance(getApplicationContext()).readAdmin())) {
                // now that ADMINS can be part of any event
                numberOfEvents = EventHelper.getInstance(getApplicationContext()).readAll().size();
            }

            if (numberOfEvents <= 1) {
                navigationView.getMenu().removeItem(R.id.events_navigation);
            }
        } catch(Exception e) {
            Log.e(LOG_NAME, "Problem pulling events for this admin.");
        }

        View headerView = navigationView.getHeaderView(0);
        try {
            final ImageView avatarImageView = (ImageView) headerView.findViewById(R.id.avatar_image_view);
            User user = UserHelper.getInstance(getApplicationContext()).readCurrentUser();
            UserLocal userLocal = user.getUserLocal();
            Glide.with(this)
                    .load(userLocal.getLocalAvatarPath())
                    .asBitmap()
                    .fallback(R.drawable.ic_account_circle_white_48dp)
                    .centerCrop()
                    .into(new BitmapImageViewTarget(avatarImageView) {
                        @Override
                        protected void setResource(Bitmap resource) {
                            RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(LandingActivity.this.getResources(), resource);
                            circularBitmapDrawable.setCircular(true);
                            avatarImageView.setImageDrawable(circularBitmapDrawable);
                        }
                    });

            TextView displayName = (TextView) headerView.findViewById(R.id.display_name);
            displayName.setText(user.getDisplayName());

            TextView email = (TextView) headerView.findViewById(R.id.email);
            email.setText(user.getEmail());
            email.setVisibility(StringUtils.isNoneBlank(user.getEmail()) ? View.VISIBLE : View.GONE);
        } catch (UserException e) {
            Log.e(LOG_NAME, "Error pulling current user from the database", e);
        }

        headerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNavigationItemSelected(navigationView.getMenu().findItem(R.id.profile_navigation));
            }
        });

        // Check if MAGE was launched with a local file
        openPath = getIntent().getStringExtra(EXTRA_OPEN_FILE_PATH);
        if (openPath != null) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
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

        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switchBottomNavigationFragment(item);
                return true;
            }
        });

        MenuItem menuItem = bottomNavigationView.getMenu().findItem(R.id.map_tab);
        if (savedInstanceState != null) {
            int item = savedInstanceState.getInt(BOTTOM_NAVIGATION_ITEM);
            menuItem = bottomNavigationView.getMenu().findItem(item);
        }
        switchBottomNavigationFragment(menuItem);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // This activity is 'singleTop' and as such will not recreate itself based on a uiMode configuration change.
        // Force this by check if the uiMode has changed.
        int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightMode != currentNightMode) {
            recreate();
        }

        if (locationPermissionGranted != (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            locationPermissionGranted = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

            // notify location services that the permissions have changed.
            ((MAGE) getApplication()).getLocationService().onLocationPermissionsChanged();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(BOTTOM_NAVIGATION_ITEM, bottomNavigationView.getSelectedItemId());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

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

    private void setTitle() {
        Event event = EventHelper.getInstance(getApplicationContext()).getCurrentEvent();
        getSupportActionBar().setTitle(event.getName());
    }

    private void showDisabledPermissionsDialog(String title, String message) {
        new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
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

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        drawerLayout.closeDrawer(GravityCompat.START);

        switch (menuItem.getItemId()) {
            case R.id.events_navigation: {
                Intent intent = new Intent(this, ChangeEventActivity.class);
                startActivityForResult(intent, CHANGE_EVENT_REQUEST);
                break;
            }
            case R.id.profile_navigation: {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.settings_navigation: {
                Intent intent = new Intent(this, GeneralPreferencesActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.help_navigation: {
                Intent intent = new Intent(this, HelpActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.logout_navigation: {
                ((MAGE)getApplication()).onLogout(true, new MAGE.OnLogoutListener() {
                    @Override
                    public void onLogout() {
                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
                break;
            }
        }

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHANGE_EVENT_REQUEST) {
            if (resultCode == RESULT_OK) {
                setTitle();
            }
        }
    }

    private void switchBottomNavigationFragment(MenuItem item) {
        Fragment fragment = null;
        switch (item.getItemId()) {
            case R.id.map_tab:
                fragment = bottomNavigationFragments.get(0);
                break;
            case R.id.observations_tab:
                fragment = bottomNavigationFragments.get(1);
                break;
            case R.id.people_tab:
                fragment = bottomNavigationFragments.get(2);
                break;
        }

        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.navigation_content, fragment).commit();
        }
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

        if (dir == null) {
            return true;
        }

        return dir.delete();
    }

}