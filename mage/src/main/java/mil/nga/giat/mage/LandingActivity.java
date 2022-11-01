package mil.nga.giat.mage;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationView;
import com.google.common.collect.Iterables;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import mil.nga.geopackage.validate.GeoPackageValidate;
import mil.nga.giat.mage.cache.GeoPackageCacheUtils;
import mil.nga.giat.mage.data.feed.Feed;
import mil.nga.giat.mage.databinding.ActivityLandingBinding;
import mil.nga.giat.mage.event.EventActivity;
import mil.nga.giat.mage.event.EventsActivity;
import mil.nga.giat.mage.feed.FeedActivity;
import mil.nga.giat.mage.glide.GlideApp;
import mil.nga.giat.mage.glide.model.Avatar;
import mil.nga.giat.mage.help.HelpActivity;
import mil.nga.giat.mage.location.LocationAccess;
import mil.nga.giat.mage.login.LoginActivity;
import mil.nga.giat.mage.location.LocationPermission;
import mil.nga.giat.mage.map.MapFragment;
import mil.nga.giat.mage.map.cache.CacheProvider;
import mil.nga.giat.mage.newsfeed.ObservationFeedFragment;
import mil.nga.giat.mage.newsfeed.UserFeedFragment;
import mil.nga.giat.mage.preferences.GeneralPreferencesActivity;
import mil.nga.giat.mage.profile.ProfileActivity;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.EventException;
import mil.nga.giat.mage.sdk.exceptions.UserException;

/**
 * This is the Activity that holds other fragments. Map, feeds, etc. It
 * starts and stops much of the context. It also contains menus .
 */
@AndroidEntryPoint
public class LandingActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

   /**
    * Extra key for storing the local file path used to launch MAGE
    */
   public static final String EXTRA_OPEN_FILE_PATH = "extra_open_file_path";

   private static final int PERMISSIONS_REQUEST_ACCESS_STORAGE= 100;
   private static final int PERMISSIONS_REQUEST_OPEN_FILE = 200;
   private static final int CHANGE_EVENT_REQUEST = 300;

   @Inject protected MageApplication application;
   @Inject protected LocationAccess locationAccess;

   private LandingViewModel viewModel;

   private int currentNightMode;

   private static final String LOG_NAME = LandingActivity.class.getName();

   private final List<Fragment> bottomNavigationFragments = new ArrayList<>();

   private String openPath;

   private ActivityLandingBinding binding;

   ActivityResultLauncher<Intent> feedIntentLauncher = registerForActivityResult(
     new ActivityResultContracts.StartActivityForResult(),
        result -> {
           if (result.getResultCode() == Activity.RESULT_OK) {
              Intent data = result.getData();
              if (data != null) {
                 FeedActivity.ResultType resultType = (FeedActivity.ResultType) data.getSerializableExtra(FeedActivity.FEED_ITEM_RESULT_TYPE);
                 onFeedResult(resultType, data);
              }
           }
        }
     );

   ActivityResultLauncher<?> reportLocationIntent = registerForActivityResult(
        new LocationPermission(), result -> {
           if (result.getPreciseGranted() || result.getCoarseGranted()) {
              application.startLocationService();
           } else {
              application.stopLocationService();
           }
        }
     );

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      SharedPreferences googleBug = getSharedPreferences("google_bug_154855417", Context.MODE_PRIVATE);
      if (!googleBug.contains("fixed")) {
         File corruptedZoomTables = new File(getFilesDir(), "ZoomTables.data");
         corruptedZoomTables.delete();
         googleBug.edit().putBoolean("fixed", true).apply();
      }

      binding = ActivityLandingBinding.inflate(getLayoutInflater());
      setContentView(binding.getRoot());

      binding.navigation.setNavigationItemSelectedListener(this);

      currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

      bottomNavigationFragments.add(new MapFragment());
      bottomNavigationFragments.add(new ObservationFeedFragment());
      bottomNavigationFragments.add(new UserFeedFragment());

      // TODO investigate moving this call
      // its here because this is the first activity started after login and it ensures
      // the user has selected an event.  However there are other instances that could
      // bring the user back to this activity in which this has already been called,
      // i.e. after TokenExpiredActivity.
      application.onLogin();

      CacheProvider.getInstance(getApplicationContext()).refreshTileOverlays();

      Event event = EventHelper.getInstance(getApplicationContext()).getCurrentEvent();
      onTitle(event);
      setRecentEvents(event);

      setSupportActionBar(binding.toolbar);

      reportLocationIntent.launch(null);

      binding.toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
      binding.toolbar.setNavigationOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));

      View headerView = binding.navigation.getHeaderView(0);
      headerView.setOnClickListener(v -> onNavigationItemSelected(binding.navigation.getMenu().findItem(R.id.profile_navigation)));

      // Check if MAGE was launched with a local file
      openPath = getIntent().getStringExtra(EXTRA_OPEN_FILE_PATH);
      if (openPath != null) {
         if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
               new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
                       .setTitle(R.string.cache_access_rationale_title)
                       .setMessage(R.string.cache_access_rationale_message)
                       .setPositiveButton(android.R.string.ok, (dialog, which) -> ActivityCompat.requestPermissions(LandingActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_OPEN_FILE))
                       .show();
            } else {
               ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_OPEN_FILE);
            }
         } else {
            // Else, store the path to pass to further intents
            handleOpenFilePath();
         }
      }

      binding.bottomNavigation.setOnItemSelectedListener(item -> {
         switch (item.getItemId()) {
            case R.id.map_tab:
               viewModel.setNavigationTab(LandingViewModel.NavigationTab.MAP);
               break;
            case R.id.observations_tab:
               viewModel.setNavigationTab(LandingViewModel.NavigationTab.OBSERVATIONS);
               break;
            case R.id.people_tab:
               viewModel.setNavigationTab(LandingViewModel.NavigationTab.PEOPLE);
               break;
         }

         return true;
      });

      viewModel = new ViewModelProvider(this).get(LandingViewModel.class);
      viewModel.getFilterText().observe(this, this::setSubtitle);
      viewModel.getNavigationTab().observe(this, this::onNavigationTab);
      viewModel.getFeeds().observe(this, this::setFeeds);
      viewModel.setEvent(event.getRemoteId());
   }

   @Override
   protected void onResume() {
      super.onResume();

      MenuItem selectedItem = binding.bottomNavigation.getMenu().findItem(binding.bottomNavigation.getSelectedItemId());
      switch (selectedItem.getItemId()) {
         case R.id.map_tab: {
            viewModel.setNavigationTab(LandingViewModel.NavigationTab.MAP);
            break;
         }
         case R.id.observations_tab: {
            viewModel.setNavigationTab(LandingViewModel.NavigationTab.OBSERVATIONS);
            break;
         }
         case R.id.people_tab: {
            viewModel.setNavigationTab(LandingViewModel.NavigationTab.PEOPLE);
            break;
         }
      }

      View headerView = binding.navigation.getHeaderView(0);
      try {
         final ImageView avatarImageView = headerView.findViewById(R.id.avatar_image_view);
         User user = UserHelper.getInstance(getApplicationContext()).readCurrentUser();
         GlideApp.with(this)
                 .load(Avatar.Companion.forUser(user))
                 .circleCrop()
                 .fallback(R.drawable.ic_account_circle_white_48dp)
                 .error(R.drawable.ic_account_circle_white_48dp)
                 .into(avatarImageView);

         TextView displayName = headerView.findViewById(R.id.display_name);
         displayName.setText(user.getDisplayName());

         TextView email = headerView.findViewById(R.id.email);
         email.setText(user.getEmail());
         email.setVisibility(StringUtils.isNoneBlank(user.getEmail()) ? View.VISIBLE : View.GONE);
      } catch (UserException e) {
         Log.e(LOG_NAME, "Error pulling current user from the database", e);
      }

      // This activity is 'singleTop' and as such will not recreate itself based on a uiMode configuration change.
      // Force this by check if the uiMode has changed.
      int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
      if (nightMode != currentNightMode) {
         recreate();
      }

      if (shouldReportLocation()) {
         if (locationAccess.isLocationGranted()) {
            application.startLocationService();
         }
      }
   }

   @Override
   public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);

      switch (requestCode) {
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

   private void setFeeds(List<Feed> feeds) {
      Menu menu = binding.navigation.getMenu();
      Menu feedsMenu = menu.findItem(R.id.feeds_item).getSubMenu();
      feedsMenu.removeGroup(R.id.feeds_group);

      int i = 1;
      for (final Feed feed : feeds) {

         MenuItem item = feedsMenu
                 .add(R.id.feeds_group, Menu.NONE, i++, feed.getTitle())
                 .setIcon(R.drawable.ic_rss_feed_24);

         // TODO get feed icon when available
//            if (feed.getMapStyle().getIconUrl() != null) {
//                int px = (int) Math.floor(TypedValue.applyDimension(
//                    TypedValue.COMPLEX_UNIT_DIP,
//                    24f,
//                    getResources().getDisplayMetrics()));
//
//                Glide.with(this)
//                    .asBitmap()
//                    .load(feed.getMapStyle().getIconUrl())
//                    .transform(new MultiTransformation<>(new FitCenter(), new PadToFrame()))
//                    .into(new CustomTarget<Bitmap>(px, px) {
//                        @Override
//                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
//                            item.setIcon(new BitmapDrawable(getResources(), resource));
//                        }
//
//                        @Override
//                        public void onLoadCleared(@Nullable Drawable placeholder) {}
//                    });
//            }

         item.setOnMenuItemClickListener(menuItem -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            Intent intent = FeedActivity.Companion.intent(LandingActivity.this, feed);
            feedIntentLauncher.launch(intent);
            return true;
         });
      }
   }

   private void setRecentEvents(Event event) {
      Menu menu = binding.navigation.getMenu();

      Menu recentEventsMenu = menu.findItem(R.id.recents_events_item).getSubMenu();
      recentEventsMenu.removeGroup(R.id.events_group);

      EventHelper eventHelper = EventHelper.getInstance(getApplicationContext());
      try {
         menu.findItem(R.id.event_navigation).setTitle(event.getName()).setActionView(R.layout.navigation_item_info);

         Iterable<Event> recentEvents = Iterables.filter(eventHelper.getRecentEvents(), recentEvent -> !recentEvent.getRemoteId().equals(event.getRemoteId()));

         int i = 1;
         for (final Event recentEvent : recentEvents) {
            MenuItem item = recentEventsMenu
                    .add(R.id.events_group, Menu.NONE, i++, recentEvent.getName())
                    .setIcon(R.drawable.ic_restore_black_24dp);

            item.setOnMenuItemClickListener(menuItem -> {
               binding.drawerLayout.closeDrawer(GravityCompat.START);
               Intent intent = new Intent(LandingActivity.this, EventsActivity.class);
               intent.putExtra(EventsActivity.Companion.getEVENT_ID_EXTRA(), recentEvent.getId());
               startActivityForResult(intent, CHANGE_EVENT_REQUEST);
               return true;
            });
         }

         MenuItem item = recentEventsMenu
                 .add(R.id.events_group, Menu.NONE, i, "More Events")
                 .setIcon(R.drawable.ic_event_note_white_24dp);

         item.setOnMenuItemClickListener(menuItem -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            Intent intent = new Intent(LandingActivity.this, EventsActivity.class);
            intent.putExtra(EventsActivity.Companion.getCLOSABLE_EXTRA(), true);
            startActivityForResult(intent, CHANGE_EVENT_REQUEST);
            return true;
         });
      } catch (EventException e) {
         e.printStackTrace();
      }
   }

   private void showDisabledPermissionsDialog(String title, String message) {
      new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
              .setTitle(title)
              .setMessage(message)
              .setPositiveButton(R.string.settings, (dialog, which) -> {
                 Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                 intent.setData(Uri.fromParts("package", getApplicationContext().getPackageName(), null));
                 startActivity(intent);
              })
              .setNegativeButton(android.R.string.cancel, null)
              .show();
   }

   @Override
   public boolean onNavigationItemSelected(MenuItem menuItem) {
      binding.drawerLayout.closeDrawer(GravityCompat.START);

      switch (menuItem.getItemId()) {
         case R.id.event_navigation: {
            Event event = EventHelper.getInstance(getApplicationContext()).getCurrentEvent();
            Intent intent = new Intent(LandingActivity.this, EventActivity.class);
            intent.putExtra(EventActivity.EVENT_ID_EXTRA, event.getId());
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
            application.onLogout(true, () -> {
               Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
               startActivity(intent);
               finish();
            });
            break;
         }
      }

      return false;
   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      super.onActivityResult(requestCode, resultCode, data);

      if (requestCode == CHANGE_EVENT_REQUEST) {
         if (resultCode == RESULT_OK) {
            Event event = EventHelper.getInstance(getApplicationContext()).getCurrentEvent();
            onTitle(event);
            setRecentEvents(event);
            viewModel.setEvent(event.getRemoteId());
         }
      }
   }

   private void onFeedResult(FeedActivity.ResultType resultType, Intent data) {
      if (resultType == FeedActivity.ResultType.NAVIGATE) {
         String feedId = data.getStringExtra(FeedActivity.FEED_ID_EXTRA);
         String itemId = data.getStringExtra(FeedActivity.FEED_ITEM_ID_EXTRA);
         viewModel.startFeedNavigation(feedId, itemId);
      }
   }

   private void onTitle(Event event) {
      binding.toolbar.setTitle(event.getName());
   }

   private void setSubtitle(String subtitle) {
      binding.toolbar.setSubtitle(subtitle);
   }

   private void onNavigationTab(LandingViewModel.NavigationTab tab) {
      Fragment fragment = null;
      switch (tab) {
         case MAP:
            binding.bottomNavigation.getMenu().getItem(0).setChecked(true);
            fragment = bottomNavigationFragments.get(0);
            break;
         case OBSERVATIONS:
            binding.bottomNavigation.getMenu().getItem(1).setChecked(true);
            fragment = bottomNavigationFragments.get(1);
            break;
         case PEOPLE:
            binding.bottomNavigation.getMenu().getItem(2).setChecked(true);
            fragment = bottomNavigationFragments.get(2);
            break;
      }

      if (fragment != null) {
         FragmentManager fragmentManager = getSupportFragmentManager();
         fragmentManager.beginTransaction().replace(R.id.navigation_content, fragment).commit();
      }
   }

   private boolean shouldReportLocation() {
      boolean reportLocation = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.reportLocationKey), getResources().getBoolean(R.bool.reportLocationDefaultValue));
      boolean inEvent = UserHelper.getInstance(getApplicationContext()).isCurrentUserPartOfCurrentEvent();

      return reportLocation && inEvent;
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
}