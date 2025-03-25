package mil.nga.giat.mage

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mil.nga.geopackage.validate.GeoPackageValidate
import mil.nga.giat.mage.LandingViewModel.NavigationTab
import mil.nga.giat.mage.cache.GeoPackageCacheUtils
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.database.model.feed.Feed
import mil.nga.giat.mage.databinding.ActivityLandingBinding
import mil.nga.giat.mage.event.EventActivity
import mil.nga.giat.mage.event.EventsActivity
import mil.nga.giat.mage.event.EventsActivity.Companion.CLOSABLE_EXTRA
import mil.nga.giat.mage.event.EventsActivity.Companion.EVENT_ID_EXTRA
import mil.nga.giat.mage.feed.FeedActivity
import mil.nga.giat.mage.feed.FeedActivity.Companion.intent
import mil.nga.giat.mage.glide.GlideApp
import mil.nga.giat.mage.glide.model.Avatar.Companion.forUser
import mil.nga.giat.mage.help.HelpActivity
import mil.nga.giat.mage.location.LocationAccess
import mil.nga.giat.mage.location.LocationContractResult
import mil.nga.giat.mage.location.LocationPermission
import mil.nga.giat.mage.login.LoginActivity
import mil.nga.giat.mage.map.MapFragment
import mil.nga.giat.mage.map.cache.CacheProvider
import mil.nga.giat.mage.newsfeed.ObservationFeedFragment
import mil.nga.giat.mage.newsfeed.UserFeedFragment
import mil.nga.giat.mage.preferences.GeneralPreferencesActivity
import mil.nga.giat.mage.profile.ProfileActivity
import org.apache.commons.lang3.StringUtils
import java.io.File
import javax.inject.Inject


/**
 * This is the Activity that holds other fragments. Map, feeds, etc. It
 * starts and stops much of the context. It also contains menus .
 */
@AndroidEntryPoint
class LandingActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
   @Inject lateinit var application: MageApplication
   @Inject lateinit var locationAccess: LocationAccess
   @Inject lateinit var userLocalDataSource: UserLocalDataSource
   @Inject lateinit var eventLocalDataSource: EventLocalDataSource
   @Inject lateinit var cacheProvider: CacheProvider

   private lateinit var viewModel: LandingViewModel
   private var currentNightMode = 0
   private val bottomNavigationFragments: MutableList<Fragment> = ArrayList()
   private var binding: ActivityLandingBinding? = null

   private var feedIntentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
      if (result.resultCode == RESULT_OK) {
         val data = result.data
         if (data != null) {
            val resultType = data.getSerializableExtra(FeedActivity.FEED_ITEM_RESULT_TYPE) as FeedActivity.ResultType?
            onFeedResult(resultType, data)
         }
      }
   }

   private var reportLocationIntent: ActivityResultLauncher<*> = registerForActivityResult(
      LocationPermission()
   ) { (coarseGranted, preciseGranted): LocationContractResult ->
      if (preciseGranted || coarseGranted) {
         application.startLocationService()
      } else {
         application.stopLocationService()
      }
   }

   public override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      val googleBug = getSharedPreferences("google_bug_154855417", MODE_PRIVATE)
      if (!googleBug.contains("fixed")) {
         val corruptedZoomTables = File(filesDir, "ZoomTables.data")
         corruptedZoomTables.delete()
         googleBug.edit().putBoolean("fixed", true).apply()
      }
      binding = ActivityLandingBinding.inflate(layoutInflater)
      setContentView(binding!!.root)
      binding!!.navigation.setNavigationItemSelectedListener(this)
      currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
      bottomNavigationFragments.add(MapFragment())
      bottomNavigationFragments.add(ObservationFeedFragment())
      bottomNavigationFragments.add(UserFeedFragment())

      // TODO investigate moving this call
      // its here because this is the first activity started after login and it ensures
      // the user has selected an event.  However there are other instances that could
      // bring the user back to this activity in which this has already been called,
      // i.e. after TokenExpiredActivity.
      application.onLogin()
      val event = eventLocalDataSource.currentEvent
      if (event != null) {
         onTitle(event)
         setRecentEvents(event)
      }
      setSupportActionBar(binding!!.toolbar)
      reportLocationIntent.launch(null)
      binding!!.toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp)
      binding!!.toolbar.setNavigationOnClickListener {
         binding!!.drawerLayout.openDrawer(
            GravityCompat.START
         )
      }
      val headerView = binding!!.navigation.getHeaderView(0)
      headerView.setOnClickListener {
         onNavigationItemSelected(
            binding!!.navigation.menu.findItem(R.id.profile_navigation)
         )
      }

      // Check if MAGE was launched with a local file
      val openPath = intent.getStringExtra(EXTRA_OPEN_FILE_PATH)
      openPath?.let { handleOpenFilePath(it) }
      binding!!.bottomNavigation.setOnItemSelectedListener { item: MenuItem ->
         when (item.itemId) {
            R.id.map_tab -> viewModel.setNavigationTab(
               NavigationTab.MAP
            )

            R.id.observations_tab -> viewModel.setNavigationTab(NavigationTab.OBSERVATIONS)
            R.id.people_tab -> viewModel.setNavigationTab(NavigationTab.PEOPLE)
         }
         true
      }
      viewModel = ViewModelProvider(this).get(LandingViewModel::class.java)
      viewModel.filterText.observe(this) { subtitle: String -> setSubtitle(subtitle) }
      viewModel.navigationTab.observe(this) { tab: NavigationTab -> onNavigationTab(tab) }
      viewModel.feeds.observe(this) { feeds: List<Feed> -> setFeeds(feeds) }
      viewModel.setEvent(event!!.remoteId)
   }

   override fun onResume() {
      super.onResume()
      val selectedItem = binding!!.bottomNavigation.menu.findItem(
         binding!!.bottomNavigation.selectedItemId
      )
      when (selectedItem.itemId) {
         R.id.map_tab -> {
            viewModel.setNavigationTab(NavigationTab.MAP)
         }

         R.id.observations_tab -> {
            viewModel.setNavigationTab(NavigationTab.OBSERVATIONS)
         }

         R.id.people_tab -> {
            viewModel.setNavigationTab(NavigationTab.PEOPLE)
         }
      }
      val headerView = binding!!.navigation.getHeaderView(0)
      val avatarImageView = headerView.findViewById<ImageView>(R.id.avatar_image_view)
      val user = userLocalDataSource.readCurrentUser()
      GlideApp.with(this)
         .load(forUser(user!!))
         .circleCrop()
         .fallback(R.drawable.ic_account_circle_white_48dp)
         .error(R.drawable.ic_account_circle_white_48dp)
         .into(avatarImageView)
      val displayName = headerView.findViewById<TextView>(R.id.display_name)
      displayName.text = user.displayName
      val email = headerView.findViewById<TextView>(R.id.email)
      email.text = user.email
      email.visibility = if (StringUtils.isNoneBlank(user.email)) View.VISIBLE else View.GONE

      // This activity is 'singleTop' and as such will not recreate itself based on a uiMode configuration change.
      // Force this by check if the uiMode has changed.
      val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
      if (nightMode != currentNightMode) {
         recreate()
      }
      if (shouldReportLocation()) {
         if (locationAccess.isLocationGranted()) {
            application.startLocationService()
         }
      }
   }

   override fun onRequestPermissionsResult(
      requestCode: Int,
      permissions: Array<String>,
      grantResults: IntArray
   ) {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults)
      when (requestCode) {
         PERMISSIONS_REQUEST_ACCESS_STORAGE -> {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
               CoroutineScope(Dispatchers.IO).launch {
                  cacheProvider.refreshTileOverlays()
               }
            }
         }
      }
   }

   private fun setFeeds(feeds: List<Feed>) {
      val menu = binding!!.navigation.menu
      val feedsMenu: Menu? = menu.findItem(R.id.feeds_item).subMenu
      feedsMenu!!.removeGroup(R.id.feeds_group)
      var i = 1
      for (feed in feeds) {
         val item = feedsMenu
            .add(R.id.feeds_group, Menu.NONE, i++, feed.title)
            .setIcon(R.drawable.ic_rss_feed_24)

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
         item.setOnMenuItemClickListener { menuItem: MenuItem? ->
            binding!!.drawerLayout.closeDrawer(GravityCompat.START)
            val intent = intent(this@LandingActivity, feed)
            feedIntentLauncher.launch(intent)
            true
         }
      }
   }

   private fun setRecentEvents(event: Event?) {
      val menu = binding!!.navigation.menu
      val recentEventsMenu = menu.findItem(R.id.recents_events_item).subMenu
      recentEventsMenu!!.removeGroup(R.id.events_group)
      menu.findItem(R.id.event_navigation)
         .setTitle(event!!.name)
         .setActionView(R.layout.navigation_item_info)


      val recentEvents = eventLocalDataSource.getRecentEvents().filterNot { it.remoteId == event.remoteId }
      var i = 1
      for (recentEvent in recentEvents) {
         val item = recentEventsMenu
            .add(R.id.events_group, Menu.NONE, i++, recentEvent.name)
            .setIcon(R.drawable.ic_restore_black_24dp)
         item.setOnMenuItemClickListener {
            binding!!.drawerLayout.closeDrawer(GravityCompat.START)
            val intent = Intent(this@LandingActivity, EventsActivity::class.java)
            intent.putExtra(EVENT_ID_EXTRA, recentEvent.id)
            startActivityForResult(intent, CHANGE_EVENT_REQUEST)
            true
         }
      }
      val item = recentEventsMenu
         .add(R.id.events_group, Menu.NONE, i, "More Events")
         .setIcon(R.drawable.ic_event_note_white_24dp)
      item.setOnMenuItemClickListener {
         binding!!.drawerLayout.closeDrawer(GravityCompat.START)
         val intent = Intent(this@LandingActivity, EventsActivity::class.java)
         intent.putExtra(CLOSABLE_EXTRA, true)
         startActivityForResult(intent, CHANGE_EVENT_REQUEST)
         true
      }
   }

   override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
      binding!!.drawerLayout.closeDrawer(GravityCompat.START)
      when (menuItem.itemId) {
         R.id.event_navigation -> {
            val event = eventLocalDataSource.currentEvent
            val intent = Intent(this@LandingActivity, EventActivity::class.java)
            intent.putExtra(EventActivity.EVENT_ID_EXTRA, event!!.id)
            startActivityForResult(intent, CHANGE_EVENT_REQUEST)
         }

         R.id.profile_navigation -> {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
         }

         R.id.settings_navigation -> {
            val intent = Intent(this, GeneralPreferencesActivity::class.java)
            startActivity(intent)
         }

         R.id.help_navigation -> {
            val intent = Intent(this, HelpActivity::class.java)
            startActivity(intent)
         }

         R.id.email_navigation -> {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.setData(Uri.parse("mailto:$CONTACT_EMAIL"))
            startActivity(intent)
         }

         R.id.logout_navigation -> {
            application.onLogout(true)
            val intent = Intent(applicationContext, LoginActivity::class.java)
            startActivity(intent)
            finish()
         }
      }
      return false
   }

   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
      super.onActivityResult(requestCode, resultCode, data)

      if (requestCode == CHANGE_EVENT_REQUEST) {
         if (resultCode == RESULT_OK) {
            val event = eventLocalDataSource.currentEvent
            onTitle(event)
            setRecentEvents(event)
            viewModel.setEvent(event!!.remoteId)
         }
      }
   }

   private fun onFeedResult(resultType: FeedActivity.ResultType?, data: Intent) {
      if (resultType === FeedActivity.ResultType.NAVIGATE) {
         val feedId = data.getStringExtra(FeedActivity.FEED_ID_EXTRA)
         val itemId = data.getStringExtra(FeedActivity.FEED_ITEM_ID_EXTRA)
         viewModel.startFeedNavigation(feedId!!, itemId!!)
      }
   }

   private fun onTitle(event: Event?) {
      binding!!.toolbar.title = event!!.name
   }

   private fun setSubtitle(subtitle: String) {
      binding!!.toolbar.subtitle = subtitle
   }

   private fun onNavigationTab(tab: NavigationTab) {
      var fragment: Fragment? = null
      when (tab) {
         NavigationTab.MAP -> {
            binding!!.bottomNavigation.menu.getItem(0).isChecked = true
            fragment = bottomNavigationFragments[0]
         }

         NavigationTab.OBSERVATIONS -> {
            binding!!.bottomNavigation.menu.getItem(1).isChecked = true
            fragment = bottomNavigationFragments[1]
         }

         NavigationTab.PEOPLE -> {
            binding!!.bottomNavigation.menu.getItem(2).isChecked = true
            fragment = bottomNavigationFragments[2]
         }
      }

      val fragmentManager = supportFragmentManager
      fragmentManager.beginTransaction().replace(R.id.navigation_content, fragment).commit()
   }

   private fun shouldReportLocation(): Boolean {
      val reportLocation = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
         getString(R.string.reportLocationKey),
         resources.getBoolean(R.bool.reportLocationDefaultValue)
      )
      val inEvent = userLocalDataSource.isCurrentUserPartOfCurrentEvent()
      return reportLocation && inEvent
   }

   /**
    * Handle opening the file path that MAGE was launched with
    */
   private fun handleOpenFilePath(path: String) {
      val cacheFile = File(path)

      // Handle GeoPackage files by linking them to their current location
      if (GeoPackageValidate.hasGeoPackageExtension(cacheFile)) {
         val event = eventLocalDataSource.currentEvent
         val cacheName = GeoPackageCacheUtils.importGeoPackage(applicationContext, cacheFile)
         if (event != null && cacheName != null) {
            CoroutineScope(Dispatchers.IO).launch {
               cacheProvider.enableAndRefreshTileOverlays(cacheName)
            }
         }
      }
   }

   companion object {
      private const val PERMISSIONS_REQUEST_ACCESS_STORAGE = 100
      private const val CHANGE_EVENT_REQUEST = 200

      const val EXTRA_OPEN_FILE_PATH = "extra_open_file_path"

      private const val CONTACT_EMAIL: String = "magesuitesupport@nga.mil"
      private const val CONTACT_PHONE: String = "tel:5715571121"
   }
}