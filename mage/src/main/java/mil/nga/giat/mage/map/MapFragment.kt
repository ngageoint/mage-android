package mil.nga.giat.mage.map

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.hardware.SensorManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.SpannableString
import android.text.util.Linkify
import android.util.Log
import android.util.Pair
import android.view.*
import android.view.ViewGroup.MarginLayoutParams
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.google.android.gms.maps.*
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.ktx.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import mil.nga.geopackage.BoundingBox
import mil.nga.geopackage.GeoPackage
import mil.nga.geopackage.GeoPackageCache
import mil.nga.geopackage.extension.link.FeatureTileTableLinker
import mil.nga.geopackage.extension.scale.TileTableScaling
import mil.nga.geopackage.factory.GeoPackageFactory
import mil.nga.geopackage.map.geom.GoogleMapShapeConverter
import mil.nga.geopackage.map.tiles.overlay.FeatureOverlay
import mil.nga.geopackage.map.tiles.overlay.FeatureOverlayQuery
import mil.nga.geopackage.map.tiles.overlay.GeoPackageOverlayFactory
import mil.nga.geopackage.tiles.TileBoundingBoxUtils
import mil.nga.geopackage.tiles.features.DefaultFeatureTiles
import mil.nga.geopackage.tiles.features.FeatureTiles
import mil.nga.geopackage.tiles.features.custom.NumberFeaturesTile
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.feed.FeedWithItems
import mil.nga.giat.mage.data.layer.LayerRepository
import mil.nga.giat.mage.data.location.LocationRepository.LocationEvent
import mil.nga.giat.mage.data.observation.ObservationRepository.ObservationEvent
import mil.nga.giat.mage.databinding.FragmentMapBinding
import mil.nga.giat.mage.feed.item.FeedItemActivity
import mil.nga.giat.mage.filter.FilterActivity
import mil.nga.giat.mage.location.LocationPolicy
import mil.nga.giat.mage.map.Geocoder.SearchResult
import mil.nga.giat.mage.map.cache.*
import mil.nga.giat.mage.map.cache.CacheProvider.OnCacheOverlayListener
import mil.nga.giat.mage.map.marker.*
import mil.nga.giat.mage.map.navigation.bearing.StraightLineNavigation
import mil.nga.giat.mage.map.preference.MapPreferencesActivity
import mil.nga.giat.mage.newsfeed.ObservationListAdapter.ObservationActionListener
import mil.nga.giat.mage.observation.ObservationLocation
import mil.nga.giat.mage.observation.edit.ObservationEditActivity
import mil.nga.giat.mage.observation.view.ObservationViewActivity
import mil.nga.giat.mage.profile.ProfileActivity
import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper
import mil.nga.giat.mage.sdk.datastore.location.Location
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper
import mil.nga.giat.mage.sdk.datastore.observation.Observation
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import mil.nga.giat.mage.sdk.datastore.user.User
import mil.nga.giat.mage.sdk.datastore.user.UserHelper
import mil.nga.giat.mage.sdk.event.IUserEventListener
import mil.nga.giat.mage.sdk.exceptions.LayerException
import mil.nga.giat.mage.sdk.exceptions.UserException
import mil.nga.mgrs.MGRS
import mil.nga.mgrs.gzd.MGRSTileProvider
import mil.nga.sf.GeometryType
import mil.nga.sf.proj.ProjectionConstants
import mil.nga.sf.proj.ProjectionFactory
import java.util.*
import javax.inject.Inject
import kotlin.collections.set

@AndroidEntryPoint
class MapFragment : Fragment(),
   View.OnClickListener,
   LocationSource,
   OnCacheOverlayListener,
   IUserEventListener,
   ObservationActionListener,
   Observer<android.location.Location>
{
   private enum class LocateState {
      OFF, FOLLOW;

      operator fun next(): LocateState {
         return if (ordinal < values().size - 1) values()[ordinal + 1] else OFF
      }
   }

   @Inject
   lateinit var application: Application

   @Inject
   lateinit var preferences: SharedPreferences

   private lateinit var viewModel: MapViewModel
   private lateinit var mapView: MapView
   private var map: GoogleMap? = null

   private lateinit var searchLayout: View
   private lateinit var searchView: SearchView

   private var straightLineNavigation: StraightLineNavigation? = null
   private var locateState = LocateState.OFF
   private var currentUser: User? = null
   private var currentEventId: Long = -1
   private var locationChangedListener: OnLocationChangedListener? = null

   @Inject
   lateinit var locationPolicy: LocationPolicy

   private var locationProvider: LiveData<android.location.Location>? = null
   private var observations: PointCollection<Observation>? = null
   private var locations: PointCollection<Pair<Location?, User?>>? = null
   private val staticGeometryCollection = StaticGeometryCollection()
   private var searchMarker: Marker? = null
   private var feedItems: FeedItemCollection? = null
   private var feeds: Map<String, LiveData<FeedWithItems>> = emptyMap()
   private var cacheOverlays: MutableMap<String, CacheOverlay?> = HashMap()

   private var cacheBoundingBox: BoundingBox? = null
   private lateinit var geoPackageCache: GeoPackageCache
   private lateinit var reportLocationButton: FloatingActionButton
   private lateinit var searchButton: FloatingActionButton
   private lateinit var zoomToLocationButton: FloatingActionButton

   private var showMgrs = false
   private var mgrsTileOverlay: TileOverlay? = null
   private lateinit var mgrsChip: TextView

   private lateinit var featureBottomSheetBehavior: BottomSheetBehavior<View>
   private lateinit var availableLayerDownloadsIcon: View
   private var binding: FragmentMapBinding? = null

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      viewModel = ViewModelProvider(this).get(MapViewModel::class.java)
   }

   override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
   ): View? {
      val view = inflater.inflate(R.layout.fragment_map, container, false)
      binding = DataBindingUtil.bind(view)

      setHasOptionsMenu(true)

      availableLayerDownloadsIcon = view.findViewById(R.id.available_layer_downloads)
      zoomToLocationButton = view.findViewById(R.id.zoom_button)
      reportLocationButton = view.findViewById(R.id.report_location)
      reportLocationButton.setOnClickListener { toggleReportLocation() }
      searchButton = view.findViewById(R.id.map_search_button)
      if (Geocoder.isPresent()) {
         searchButton.setOnClickListener { search() }
      } else {
         searchButton.hide()
      }

      view.findViewById<View>(R.id.new_observation_button).setOnClickListener { onNewObservation() }

      searchLayout = view.findViewById(R.id.search_layout)
      searchView = view.findViewById(R.id.search_view)
      searchView.setIconifiedByDefault(false)
      searchView.isIconified = false
      searchView.clearFocus()

      MapsInitializer.initialize(requireContext())
      val mapSettings = view.findViewById<ImageButton>(R.id.map_settings)
      mapSettings.setOnClickListener(this)
      mapView = view.findViewById(R.id.mapView)
      val mapState = savedInstanceState?.getBundle(MAP_VIEW_STATE)

      mapView.onCreate(mapState)
      mgrsChip = view.findViewById(R.id.mgrs_chip)

      val featureBottomSheet = view.findViewById<View>(R.id.observation_bottom_sheet)
      featureBottomSheetBehavior = BottomSheetBehavior.from(featureBottomSheet)
      featureBottomSheetBehavior.isFitToContents = true
      featureBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
      featureBottomSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
         // this only is called when the user interacts with the sheet, not when we make it EXPANDED programmatically
         override fun onStateChanged(view: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
               unselectObservation()
            }
         }

         override fun onSlide(view: View, v: Float) {}
      })

      locationProvider = locationPolicy.bestLocationProvider
      geoPackageCache = GeoPackageCache(GeoPackageFactory.getManager(application))

      return view
   }

   fun unselectObservation() {
      featureBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
      binding?.chosenObservation = null
   }

   @OptIn(ExperimentalCoroutinesApi::class)
   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      super.onViewCreated(view, savedInstanceState)

      viewModel.items.observe(viewLifecycleOwner, Observer {
         onFeedItems(it)
      })

      viewModel.searchResult.observe(viewLifecycleOwner, Observer {
         onSearchResult(it)
      })

      val isRestore = savedInstanceState != null
      lifecycleScope.launch {
         repeatOnLifecycle(Lifecycle.State.STARTED) {
            val googleMap = mapView.awaitMap()
            map = googleMap
            updateMapView()

            if (!isRestore) {
               googleMap.uiSettings.isMyLocationButtonEnabled = false

               observations = ObservationMarkerCollection(application, googleMap)
               locations = LocationMarkerCollection(application, googleMap)
               feedItems = FeedItemCollection(application, googleMap)

               val sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as? SensorManager
               straightLineNavigation = StraightLineNavigation(
                  sensorManager,
                  googleMap,
                  requireActivity().findViewById(R.id.straight_line_nav_container),
                  requireActivity(),
                  application
               )

               onMapReady(googleMap)
            }

            launch {
               googleMap.mapClickEvents().collect { onMapClick(it) }
            }

            launch {
               googleMap.markerClickEvents().collect { onMarkerClick(it) }
            }

            launch {
               googleMap.mapLongClickEvents().collect { onMapLongClick(it) }
            }

            launch {
               googleMap.infoWindowClickEvents().collect { onInfoWindowClick(it) }
            }

            launch {
               googleMap.infoWindowCloseEvents().collect { onInfoWindowClose(it) }
            }

            launch {
               googleMap.cameraIdleEvents().collect { onCameraIdle() }
            }

            launch {
               googleMap.cameraEvents().collect { // TODO use non deprecated method when reason code is added
                  if (it is CameraMoveStartedEvent) {
                     onCameraMoveStarted(it.reason)
                  }
               }
            }

            launch {
               viewModel.observationEvents.collect { event ->
                  when(event) {
                     is ObservationEvent.ObservationCreateEvent -> {
                        observations?.add(event.observation)
                     }
                     is ObservationEvent.ObservationUpdateEvent -> {
                        observations?.remove(event.observation)
                        observations?.add(event.observation)
                     }
                     is ObservationEvent.ObservationDeleteEvent -> {
                        observations?.remove(event.observation)
                     }
                  }
               }
            }

            launch {
               viewModel.locationEvents.collect { event ->
                  when(event) {
                     is LocationEvent.LocationCreateEvent -> {
                        locations?.add(Pair(event.location, event.user))
                     }
                     is LocationEvent.LocationUpdateEvent -> {
                        locations?.remove(Pair(event.location, event.user))
                        locations?.add(Pair(event.location, event.user))
                     }
                  }
               }
            }

            launch(Dispatchers.IO) {
               while(isActive) {
                  if (locations?.isVisible == true) {
                     locations?.refreshMarkerIcons()
                  }

                  delay(MARKER_REFRESH_INTERVAL.toLong())
               }
            }
         }
      }
   }

   override fun onObservationClick(observation: Observation) {
      val intent = Intent(context, ObservationViewActivity::class.java)
      intent.putExtra(ObservationViewActivity.OBSERVATION_ID, observation.id)
      intent.putExtra(ObservationViewActivity.INITIAL_LOCATION, map?.cameraPosition?.target)
      intent.putExtra(ObservationViewActivity.INITIAL_ZOOM, map?.cameraPosition?.zoom)
      startActivity(intent)
   }

   override fun onObservationDirections(observation: Observation) {
      featureBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

      // present a dialog to pick between android system map and straight line
      AlertDialog.Builder(requireActivity())
         .setTitle(application.resources.getString(R.string.navigation_choice_title))
         .setItems(R.array.navigationOptions) { _: DialogInterface?, which: Int ->
            when (which) {
               0 -> {
                  val intent = Intent(Intent.ACTION_VIEW, observation.googleMapsUri)
                  startActivity(intent)
               }
               1 -> {
                  val location: android.location.Location? = locationProvider?.value
                  if (location == null) {
                     if (ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        AlertDialog.Builder(requireActivity())
                           .setTitle(application.resources.getString(R.string.location_missing_title))
                           .setMessage(application.resources.getString(R.string.location_missing_message))
                           .setPositiveButton(android.R.string.ok, null)
                           .show()
                     } else {
                        AlertDialog.Builder(requireActivity())
                           .setTitle(application.resources.getString(R.string.location_access_observation_title))
                           .setMessage(application.resources.getString(R.string.location_access_observation_message))
                           .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                              requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
                           }
                           .show()
                     }
                  } else {
                     val centroid = observation.geometry.centroid
                     val latLng = LatLng(centroid.y, centroid.x)
                     straightLineNavigation?.startNavigation(location, latLng, ObservationBitmapFactory.bitmap(context, observation))
                     featureBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                  }
               }
            }
         }
         .setNegativeButton(android.R.string.cancel, null)
         .show()
   }

   override fun onObservationLocation(observation: Observation) {}

   override fun onChanged(location: android.location.Location?) {
      if (location == null) return

      locationChangedListener?.onLocationChanged(location)

      if (locateState == LocateState.FOLLOW) {
         val cameraPosition = CameraPosition.Builder()
            .target(LatLng(location.latitude, location.longitude))
            .zoom(17f)
            .bearing(location.bearing)
            .build()
         map?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
      }

      if (preferences.getBoolean(application.resources.getString(R.string.showHeadingKey), false)) {
         startHeading()
      }
   }

   override fun onDestroyView() {
      super.onDestroyView()
      mapView.onDestroy()

      observations?.clear()
      observations = null

      locations?.clear()
      locations = null

      feedItems?.clear()

      searchMarker?.remove()

      mgrsTileOverlay?.remove()
      mgrsTileOverlay = null

      geoPackageCache.closeAll()
      cacheOverlays.clear()

      staticGeometryCollection.clear()

      currentUser = null
      map = null
   }

   override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
      inflater.inflate(R.menu.filter, menu)
   }

   override fun onOptionsItemSelected(item: MenuItem): Boolean {
      return when (item.itemId) {
         R.id.filter_button -> {
            val intent = Intent(activity, FilterActivity::class.java)
            startActivity(intent)
            true
         }
         else -> super.onOptionsItemSelected(item)
      }
   }

   private fun onMapReady(googleMap: GoogleMap) {
      val dayNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
      if (dayNightMode == Configuration.UI_MODE_NIGHT_NO) {
         googleMap.setMapStyle(null)
      } else {
         googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(application, R.raw.map_theme_night))
      }

      val showTraffic = preferences.getBoolean(resources.getString(R.string.showTrafficKey), resources.getBoolean(R.bool.showTrafficDefaultValue))

      googleMap.isTrafficEnabled = showTraffic
      val currentEvent = EventHelper.getInstance(activity).currentEvent
      var currentEventId = currentEventId
      if (currentEvent != null) {
         currentEventId = currentEvent.id
      }

      if (this.currentEventId != currentEventId) {
         this.currentEventId = currentEventId
         observations?.clear()
         locations?.clear()
      }

      UserHelper.getInstance(context).addListener(this)
      CacheProvider.getInstance(context).registerCacheOverlayListener(this)

      zoomToLocationButton.setOnClickListener { zoomToLocation() }

      // Set visibility on map markers as preferences may have changed
      observations?.setVisibility(preferences.getBoolean(resources.getString(R.string.showObservationsKey), true))
      locations?.setVisibility(preferences.getBoolean(resources.getString(R.string.showLocationsKey), true))

      // maybe need to turn off heading
      if (!preferences.getBoolean( resources.getString(R.string.showHeadingKey), false)) {
         straightLineNavigation?.stopHeading()
      }

      // Check if any map preferences changed that I care about
      if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
         googleMap.isMyLocationEnabled = true
         googleMap.setLocationSource(this)
      } else {
         googleMap.isMyLocationEnabled = false
         googleMap.setLocationSource(null)
      }

      requireActivity().findViewById<View>(R.id.mgrs_chip_container).visibility = if (showMgrs) View.VISIBLE else View.GONE
      if (showMgrs) {
         mgrsTileOverlay = googleMap.addTileOverlay(TileOverlayOptions().tileProvider(MGRSTileProvider(application)))
      }
      (activity as AppCompatActivity?)?.supportActionBar?.subtitle = getFilterTitle()

      currentEvent?.remoteId?.let {
         viewModel.setEvent(it)
      }
   }

   private fun onSearchResult(result: SearchResult?) {
      searchMarker?.remove()

      if (result == null) {
         // TODO See what google gives me, would be nice not to check connectivity
         if (ConnectivityUtility.isOnline(context)) {
            Toast.makeText(context, "Could not find address.", Toast.LENGTH_LONG).show()
         } else {
            Toast.makeText(context, "No connectivity, try again later.", Toast.LENGTH_LONG).show()
         }
      } else {
         searchMarker = map?.addMarker(result.markerOptions)
         searchMarker?.showInfoWindow()

         val position = CameraPosition.builder()
            .target(result.markerOptions.position)
            .zoom(result.zoom.toFloat()).build()

         map?.animateCamera(CameraUpdateFactory.newCameraPosition(position))
      }
   }

   private fun updateReportLocationButton() {
      val reportLocation = ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
           preferences.getBoolean(resources.getString(R.string.reportLocationKey), false) &&
           UserHelper.getInstance(application).isCurrentUserPartOfCurrentEvent

      if (reportLocation) {
         reportLocationButton.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(application, R.color.md_green_500))
         reportLocationButton.setImageResource(R.drawable.ic_my_location_white_24dp)
      } else {
         reportLocationButton.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(application, R.color.md_red_500))
         reportLocationButton.setImageResource(R.drawable.ic_outline_location_disabled_24)
      }
   }

   private fun zoomToLocation() {
      if (ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
         AlertDialog.Builder(requireActivity())
            .setTitle(application.resources.getString(R.string.location_access_observation_title))
            .setMessage(application.resources.getString(R.string.location_access_zoom_message))
            .setPositiveButton(R.string.settings) { _: DialogInterface?, _: Int ->
               val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
               intent.data = Uri.fromParts("package", application.packageName, null)
               startActivity(intent)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
         return
      }

      locateState = locateState.next()
      when (locateState) {
         LocateState.OFF -> zoomToLocationButton.isSelected = false
         LocateState.FOLLOW -> {
            zoomToLocationButton.isSelected = true
            val location = locationProvider?.value
            if (location != null) {
               val cameraPosition = CameraPosition.Builder()
                  .target(LatLng(location.latitude, location.longitude))
                  .zoom(17f)
                  .bearing(45f)
                  .build()
               map?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
         }
      }
   }

   private fun toggleReportLocation() {
      if (ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
         AlertDialog.Builder(requireActivity())
            .setTitle(application.resources.getString(R.string.location_access_observation_title))
            .setMessage(application.resources.getString(R.string.location_access_report_message))
            .setPositiveButton(R.string.settings) { _: DialogInterface?, _: Int ->
               val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
               intent.data = Uri.fromParts("package", application.packageName, null)
               startActivity(intent)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()

         return
      }
      if (!UserHelper.getInstance(application).isCurrentUserPartOfCurrentEvent) {
         AlertDialog.Builder(requireActivity())
            .setTitle(application.resources.getString(R.string.no_event_title))
            .setMessage(application.resources.getString(R.string.location_no_event_message))
            .setPositiveButton(android.R.string.ok, null)
            .show()

         return
      }

      val key = resources.getString(R.string.reportLocationKey)
      val reportLocation = !preferences.getBoolean(key, false)
      preferences.edit().putBoolean(key, reportLocation).apply()
      updateReportLocationButton()

      val text = if (reportLocation) resources.getString(R.string.report_location_start) else resources.getString(
            R.string.report_location_stop
         )
      val snackbar = Snackbar.make(requireActivity().findViewById(R.id.coordinator_layout), text, Snackbar.LENGTH_SHORT)
      snackbar.anchorView = requireActivity().findViewById(R.id.new_observation_button)

      val params = snackbar.view.layoutParams as MarginLayoutParams
      params.setMargins(0, 100, 0, 100)
      snackbar.view.layoutParams = params
      snackbar.show()
   }

   private fun startHeading() {
      if (preferences.contains(application.resources.getString(R.string.showHeadingKey))) {
         val location = locationProvider?.value
         if (location != null && straightLineNavigation != null && preferences.getBoolean(application.resources.getString(R.string.showHeadingKey), false)) {
            straightLineNavigation?.startHeading(location)
         }
      } else {
         // show a dialog asking them what they want to do and set the preference
         AlertDialog.Builder(requireActivity())
            .setTitle(application.resources.getString(R.string.always_show_heading_title))
            .setMessage(application.resources.getString(R.string.always_show_heading_message))
            .setPositiveButton(application.resources.getString(R.string.yes)) { _: DialogInterface?, _: Int ->
               val edit = preferences.edit()
               edit.putBoolean(application.resources.getString(R.string.showHeadingKey), true)
               edit.apply()
               startHeading()
            }
            .setNegativeButton(application.resources.getString(R.string.no)) { _: DialogInterface?, _: Int ->
               val edit = preferences.edit()
               edit.putBoolean(application.resources.getString(R.string.showHeadingKey), false)
               edit.apply()
            }
            .show()
      }
   }

   override fun onLowMemory() {
      super.onLowMemory()
      mapView.onLowMemory()
   }

   override fun onResume() {
      super.onResume()

      updateReportLocationButton()

      try {
         currentUser = UserHelper.getInstance(application).readCurrentUser()
      } catch (ue: UserException) {
         Log.e(LOG_NAME, "Could not find current user.", ue)
      }

      mapView.onResume()
      showMgrs = preferences.getBoolean(resources.getString(R.string.showMGRSKey), false)

      try {
         val event = EventHelper.getInstance(application).currentEvent
         val available = LayerHelper.getInstance(application).readByEvent(event, "GeoPackage").any {
            !it.isLoaded
         }

         availableLayerDownloadsIcon.visibility = if (available) View.GONE else View.VISIBLE
      } catch (e: LayerException) {
         Log.e(LOG_NAME, "Error reading layers", e)
      }

      searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
         override fun onQueryTextSubmit(text: String): Boolean {
            if (text.isNotBlank()) {
               viewModel.search(text)
            }
            searchView.clearFocus()
            return true
         }

         override fun onQueryTextChange(newText: String): Boolean {
            if (newText.isEmpty()) {
               searchMarker?.remove()
            }

            return true
         }
      })
   }

   override fun onPause() {
      super.onPause()

      mapView.onPause()

      UserHelper.getInstance(context).removeListener(this)

      observations?.clear()
      locations?.clear()
      feedItems?.clear()

      feeds.values.forEach {
         it.removeObservers(viewLifecycleOwner)
      }

      CacheProvider.getInstance(application).unregisterCacheOverlayListener(this)

      map?.let {
         saveMapView()
         it.setLocationSource(null)
         mgrsTileOverlay?.remove()
      }
   }

   private fun onFeedItems(feeds: Map<String, LiveData<FeedWithItems>>) {
      feeds.values.forEach {
         it.removeObservers(viewLifecycleOwner)
      }

      this.feeds = feeds
      feeds.values.forEach {
         it.observe(viewLifecycleOwner, Observer { items: FeedWithItems? ->
            onFeedItems(items)
         })
      }
   }

   private fun onFeedItems(items: FeedWithItems?) {
      if (items != null) {
         feedItems?.setItems(items)
      }
   }

   private fun search() {
      if (searchLayout.visibility == View.VISIBLE) {
         searchView.clearFocus()
         searchView.onActionViewCollapsed()
         searchButton.isSelected = false
         searchLayout.visibility = View.GONE
      } else {
         searchView.requestFocus()
         searchView.onActionViewExpanded()
         searchButton.isSelected = true
         searchLayout.visibility = View.VISIBLE
      }
   }

   private fun onNewObservation() {
      var location: ObservationLocation? = null

      // if there is not a location from the location service, then try to pull one from the database.
      if (locationProvider?.value == null) {
         val locations = LocationHelper.getInstance(application).getCurrentUserLocations(1, true)
         val userLocation = locations.firstOrNull()
         if (userLocation != null) {
            val propertiesMap = userLocation.propertiesMap
            val provider = propertiesMap["provider"]?.value?.toString() ?: ObservationLocation.MANUAL_PROVIDER
            location = ObservationLocation(provider, userLocation.geometry)
            location.time = userLocation.timestamp.time
            location.accuracy = propertiesMap["accuracy"]?.value?.toString()?.toFloatOrNull()
         }
      } else {
         location = ObservationLocation(locationProvider?.value)
      }

      if (!UserHelper.getInstance(application).isCurrentUserPartOfCurrentEvent) {
         AlertDialog.Builder(requireActivity())
            .setTitle(application.resources.getString(R.string.no_event_title))
            .setMessage(application.resources.getString(R.string.observation_no_event_message))
            .setPositiveButton(android.R.string.ok, null)
            .show()
      } else if (location != null) {
         newObservation(location)
      } else {
         if (ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder(requireActivity())
               .setTitle(application.resources.getString(R.string.location_missing_title))
               .setMessage(application.resources.getString(R.string.location_missing_message))
               .setPositiveButton(android.R.string.ok, null)
               .show()
         } else {
            AlertDialog.Builder(requireActivity())
               .setTitle(application.resources.getString(R.string.location_access_observation_title))
               .setMessage(application.resources.getString(R.string.location_access_observation_message))
               .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                  requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
               }
               .show()
         }
      }
   }

   override fun onRequestPermissionsResult(
      requestCode: Int,
      permissions: Array<String>,
      grantResults: IntArray
   ) {
      when (requestCode) {
         PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
               onNewObservation()
            }
         }
      }
   }

   override fun onUserCreated(user: User) {}
   override fun onUserUpdated(user: User) {}
   override fun onUserIconUpdated(user: User) {
      Handler(Looper.getMainLooper()).post {
         locations?.refresh(Pair(Location(), user))
      }
   }

   override fun onUserAvatarUpdated(user: User) {}

   private fun onInfoWindowClick(marker: Marker) {
      locations?.pointForMarker(marker)?.let {
         val profileView = Intent(context, ProfileActivity::class.java)
         profileView.putExtra(ProfileActivity.USER_ID, it.second?.remoteId)
         startActivity(profileView)
         return
      }

      feedItems?.itemForMarker(marker)?.let {
         val intent = FeedItemActivity.intent(application, it)
         startActivity(intent)
         return
      }
   }

   private fun onInfoWindowClose(marker: Marker) {
      feedItems?.onInfoWindowClose(marker)
   }

   private fun showObservationBottomSheet(observation: Observation?) {
      binding?.chosenObservation = observation
      binding?.observationBottomSheet?.requestLayout()
      binding?.observationBottomSheet?.observationActionListener = this
      featureBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
   }

   private fun onMarkerClick(marker: Marker): Boolean {
      hideKeyboard()
      observations?.offMarkerClick()

      // search marker
      if (searchMarker?.id == marker.id) {
         searchMarker?.showInfoWindow()
         return true
      }

      // You can only have one marker click listener per map.
      // Lets listen here and shell out the click event to all
      // my marker collections. Each one need to handle
      // gracefully if it does not actually contain the marker
      if (observations?.onMarkerClick(marker) == true) {
         observations?.pointForMarker(marker)?.let {
            showObservationBottomSheet(it)
         }
         return true
      }

      if (locations?.onMarkerClick(marker) == true) {
         return true
      }

      if (feedItems?.onMarkerClick(marker) == true) {
         return true
      }

      // static layer
      val snippet = marker.snippet
      if (snippet != null) {
         val markerInfoWindow = LayoutInflater.from(activity).inflate(R.layout.static_feature_infowindow, null, false)
         val webView = markerInfoWindow.findViewById<WebView>(R.id.static_feature_infowindow_content)
         webView.loadData(snippet, "text/html; charset=UTF-8", null)
         AlertDialog.Builder(requireActivity())
            .setView(markerInfoWindow)
            .setPositiveButton(android.R.string.ok, null)
            .show()
      }

      return true
   }

   private fun onMapClick(latLng: LatLng) {
      hideKeyboard()

      locations?.offMarkerClick()
      observations?.offMarkerClick()

      unselectObservation()
      observations?.onMapClick(latLng)
      staticGeometryCollection.onMapClick(map, latLng, activity)

      if (cacheOverlays.isNotEmpty()) {
         val clickMessage = StringBuilder()
         for (cacheOverlay in cacheOverlays.values) {
            val message = cacheOverlay?.onMapClick(latLng, mapView, map)
            if (message != null) {
               if (clickMessage.isNotEmpty()) {
                  clickMessage.append("\n\n")
               }
               clickMessage.append(message)
            }
         }

         if (clickMessage.isNotEmpty()) {
            val text = SpannableString(clickMessage.toString())
            Linkify.addLinks(text, Linkify.WEB_URLS)
            val view = layoutInflater.inflate(R.layout.view_feature_details, null)
            val textView = view.findViewById<TextView>(R.id.text)
            textView.text = text
            AlertDialog.Builder(requireActivity())
               .setView(view)
               .setPositiveButton(android.R.string.ok, null)
               .show()
         }
      }
   }

   private fun onMapLongClick(point: LatLng) {
      hideKeyboard()
      if (!UserHelper.getInstance(application).isCurrentUserPartOfCurrentEvent) {
         AlertDialog.Builder(requireActivity())
            .setTitle(application.resources.getString(R.string.no_event_title))
            .setMessage(application.resources.getString(R.string.observation_no_event_message))
            .setPositiveButton(android.R.string.ok, null)
            .show()
      } else {
         val location = ObservationLocation(ObservationLocation.MANUAL_PROVIDER, point)
         location.accuracy = 0.0f
         location.time = Date().time
         newObservation(location)
      }
   }

   private fun newObservation(location: ObservationLocation) {
      val intent = Intent(activity, ObservationEditActivity::class.java)
      intent.putExtra(ObservationEditActivity.LOCATION, location)
      intent.putExtra(ObservationEditActivity.INITIAL_LOCATION, map?.cameraPosition?.target)
      intent.putExtra(ObservationEditActivity.INITIAL_ZOOM, map?.cameraPosition?.zoom)
      startActivity(intent)
   }

   override fun onClick(view: View) {
      hideKeyboard()

      when (view.id) {
         R.id.map_settings -> {
            val intent = Intent(activity, MapPreferencesActivity::class.java)
            startActivity(intent)
         }
      }
   }

   override fun activate(listener: OnLocationChangedListener) {
      locationChangedListener = listener
      val location = locationProvider?.value
      if (location != null) {
         locationChangedListener?.onLocationChanged(location)
      }
      locationProvider?.observe(this, this)
   }

   override fun deactivate() {
      locationProvider?.removeObserver(this)
      locationChangedListener = null
   }

   private fun onCameraIdle() {
      setMgrsCode()
   }

   private fun onCameraMoveStarted(reason: Int) {
      if (reason == OnCameraMoveStartedListener.REASON_GESTURE) {
         locateState = LocateState.OFF
         zoomToLocationButton.isSelected = false
      }
   }

   private fun setMgrsCode() {
      if (mgrsTileOverlay != null) {
         val zoom = map?.cameraPosition?.zoom ?: 0f
         val center = map?.cameraPosition?.target ?: LatLng(0.0, 0.0)
         val mgrs = MGRS.from(mil.nga.mgrs.wgs84.LatLng(center.latitude, center.longitude))

         val text = when {
            zoom > 9 -> {
               val accuracy = when {
                  zoom < 12 -> 2
                  zoom < 15 -> 3
                  zoom < 17 -> 4
                  else -> 5
               }
               mgrs.format(accuracy)
            }
            zoom > 5 -> {
               "${mgrs.zone}${mgrs.band}${mgrs.e100k}${mgrs.n100k}"
            }
            else -> {
               "${mgrs.zone}${mgrs.band}"
            }
         }

         mgrsChip.text = text
      }
   }

   override fun onCacheOverlay(overlays: List<CacheOverlay>) {
      // Add all overlays that are in the preferences
      val currentEvent = EventHelper.getInstance(activity).currentEvent
      val cacheOverlays = CacheOverlayFilter(application, currentEvent).filter(overlays)

      // Track enabled cache overlays
      val enabledCacheOverlays: MutableMap<String, CacheOverlay?> = HashMap()

      // Track enabled GeoPackages
      val enabledGeoPackages: MutableSet<String> = HashSet()

      // Reset the bounding box for newly added caches
      cacheBoundingBox = null
      for (cacheOverlay in cacheOverlays) {
         if (cacheOverlay is StaticFeatureCacheOverlay) {
            staticGeometryCollection.removeLayer(cacheOverlay.id)
         }

         // If this cache overlay potentially replaced by a new version
         if (cacheOverlay.isAdded) {
            if (cacheOverlay.type == CacheOverlayType.GEOPACKAGE) {
               geoPackageCache.close(cacheOverlay.name)
            }
         }

         // The user has asked for this overlay
         if (cacheOverlay.isEnabled) {
            when (cacheOverlay) {
               is URLCacheOverlay -> addURLCacheOverlay(enabledCacheOverlays, cacheOverlay)
               is GeoPackageCacheOverlay -> addGeoPackageCacheOverlay(enabledCacheOverlays, enabledGeoPackages, cacheOverlay)
               is XYZDirectoryCacheOverlay -> addXYZDirectoryCacheOverlay(enabledCacheOverlays, cacheOverlay)
               is StaticFeatureCacheOverlay -> addStaticFeatureOverlay(cacheOverlay)
            }
         }
         cacheOverlay.isAdded = false
      }

      // Remove any overlays that are on the map but no longer selected in
      // preferences, update the tile overlays to the enabled tile overlays
      this.cacheOverlays.values.forEach { it?.removeFromMap() }
      this.cacheOverlays = enabledCacheOverlays

      // Close GeoPackages no longer enabled
      geoPackageCache.closeRetain(enabledGeoPackages)

      // If a new cache was added, zoom to the bounding box area
      cacheBoundingBox?.let {
         val boundsBuilder = LatLngBounds.Builder()
         boundsBuilder.include(LatLng(it.minLatitude, it.minLongitude))
         boundsBuilder.include(LatLng(it.minLatitude, it.maxLongitude))
         boundsBuilder.include(LatLng(it.maxLatitude, it.minLongitude))
         boundsBuilder.include(LatLng(it.maxLatitude, it.maxLongitude))

         try {
            map?.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 0))
         } catch (e: Exception) {
            Log.e(LOG_NAME, "Unable to move camera to newly added cache location", e)
         }
      }
   }

   private fun addURLCacheOverlay(
      enabledCacheOverlays: MutableMap<String, CacheOverlay?>,
      urlCacheOverlay: URLCacheOverlay
   ) {
      // Retrieve the cache overlay if it already exists (and remove from cache overlays)
      if (!cacheOverlays.containsKey(urlCacheOverlay.cacheName)) {
         // Create a new tile provider and add to the map
         var isTransparent = false
         val tileProvider = when {
            urlCacheOverlay.format.equals("xyz", ignoreCase = true) -> {
               XYZTileProvider(256, 256, urlCacheOverlay)
            }
            urlCacheOverlay.format.equals("tms", ignoreCase = true) -> {
               TMSTileProvider(256, 256, urlCacheOverlay)
            }
            urlCacheOverlay is WMSCacheOverlay -> {
               isTransparent = urlCacheOverlay.wmsTransparent.toBoolean()
               WMSTileProvider(256, 256, urlCacheOverlay)
            }
            else -> null
         }

         if (tileProvider != null) {
            val overlayOptions = createTileOverlayOptions(tileProvider)
            if (urlCacheOverlay.isBase) {
               overlayOptions.zIndex(-4f)
            } else if (!isTransparent) {
               overlayOptions.zIndex(-3f)
            } else {
               overlayOptions.zIndex(-2f)
            }

            // Set the tile overlay in the cache overlay
            val tileOverlay = map?.addTileOverlay(overlayOptions)
            urlCacheOverlay.tileOverlay = tileOverlay

            // Add the cache overlay to the enabled cache overlays
            enabledCacheOverlays[urlCacheOverlay.cacheName] = urlCacheOverlay
         }
      }
   }

   private fun addStaticFeatureOverlay(overlay: StaticFeatureCacheOverlay) {
      lifecycleScope.launch {
         viewModel.getStaticFeatureEvents(overlay.id)
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .flowOn(Dispatchers.IO)
            .collect { event ->
               when(event) {
                  is LayerRepository.StaticFeatureEvent.Point -> {
                     map?.addMarker(event.options)?.let {
                        staticGeometryCollection.addMarker(event.layerId, it)
                     }
                  }
                  is LayerRepository.StaticFeatureEvent.Polyline -> {
                     map?.addPolyline(event.options)?.let {
                        staticGeometryCollection.addPolyline(event.layerId, it, event.content)
                     }
                  }
                  is LayerRepository.StaticFeatureEvent.Polygon -> {
                     map?.addPolygon(event.options)?.let {
                        staticGeometryCollection.addPolygon(event.layerId, it, event.content)
                     }
                  }
               }
            }
      }
   }

   /**
    * Add XYZ Directory tile cache overlay
    * @param enabledCacheOverlays
    * @param xyzDirectoryCacheOverlay
    */
   private fun addXYZDirectoryCacheOverlay(
      enabledCacheOverlays: MutableMap<String, CacheOverlay?>,
      xyzDirectoryCacheOverlay: XYZDirectoryCacheOverlay
   ) {
      // Retrieve the cache overlay if it already exists (and remove from cache overlays)
      var cacheOverlay = cacheOverlays.remove(xyzDirectoryCacheOverlay.cacheName)
      if (cacheOverlay == null) {
         // Create a new tile provider and add to the map
         val tileProvider: TileProvider = FileSystemTileProvider(256, 256, xyzDirectoryCacheOverlay.directory.absolutePath)
         val overlayOptions = createTileOverlayOptions(tileProvider)
         // Set the tile overlay in the cache overlay
         val tileOverlay = map?.addTileOverlay(overlayOptions)
         xyzDirectoryCacheOverlay.tileOverlay = tileOverlay
         cacheOverlay = xyzDirectoryCacheOverlay
      }
      // Add the cache overlay to the enabled cache overlays
      enabledCacheOverlays[cacheOverlay.cacheName] = cacheOverlay
   }

   /**
    * Add a GeoPackage cache overlay, which contains tile and feature tables
    * @param enabledCacheOverlays
    * @param enabledGeoPackages
    * @param geoPackageCacheOverlay
    */
   private fun addGeoPackageCacheOverlay(
      enabledCacheOverlays: MutableMap<String, CacheOverlay?>,
      enabledGeoPackages: MutableSet<String>,
      geoPackageCacheOverlay: GeoPackageCacheOverlay
   ) {

      // Check each GeoPackage table
      for (tableCacheOverlay in geoPackageCacheOverlay.children) {
         // Check if the table is enabled
         if (tableCacheOverlay.isEnabled) {

            // Get and open if needed the GeoPackage
            val geoPackage = geoPackageCache.getOrOpen(geoPackageCacheOverlay.name)
            enabledGeoPackages.add(geoPackage.name)

            // Handle tile and feature tables
            try {
               when (tableCacheOverlay.type) {
                  CacheOverlayType.GEOPACKAGE_TILE_TABLE -> addGeoPackageTileCacheOverlay(
                     enabledCacheOverlays,
                     tableCacheOverlay as GeoPackageTileTableCacheOverlay,
                     geoPackage
                  )
                  CacheOverlayType.GEOPACKAGE_FEATURE_TABLE -> addGeoPackageFeatureCacheOverlay(
                     enabledCacheOverlays,
                     tableCacheOverlay as GeoPackageFeatureTableCacheOverlay,
                     geoPackage
                  )
                  else -> throw UnsupportedOperationException("Unsupported GeoPackage type: " + tableCacheOverlay.type)
               }
            } catch (e: Exception) {
               Log.e(
                  LOG_NAME,
                  "Failed to add GeoPackage overlay. GeoPackage: " + geoPackage.name + ", Name: " + tableCacheOverlay.name,
                  e
               )
            }

            // If a newly added cache, update the bounding box for zooming
            if (geoPackageCacheOverlay.isAdded) {
               try {
                  val boundingBox = geoPackage.getBoundingBox(
                     ProjectionFactory.getProjection(ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM.toLong()),
                     tableCacheOverlay.name
                  )
                  if (boundingBox != null) {
                     cacheBoundingBox = if (cacheBoundingBox == null) {
                        boundingBox
                     } else {
                        TileBoundingBoxUtils.union(cacheBoundingBox, boundingBox)
                     }
                  }
               } catch (e: Exception) {
                  Log.e(
                     LOG_NAME, "Failed to retrieve GeoPackage Table bounding box. GeoPackage: "
                             + geoPackage.name + ", Table: " + tableCacheOverlay.name, e
                  )
               }
            }
         }
      }
   }

   /**
    * Add the GeoPackage Tile Table Cache Overlay
    * @param enabledCacheOverlays
    * @param tileTableCacheOverlay
    * @param geoPackage
    */
   private fun addGeoPackageTileCacheOverlay(
      enabledCacheOverlays: MutableMap<String, CacheOverlay?>,
      tileTableCacheOverlay: GeoPackageTileTableCacheOverlay,
      geoPackage: GeoPackage
   ) {
      // Retrieve the cache overlay if it already exists (and remove from cache overlays)
      var cacheOverlay = cacheOverlays.remove(tileTableCacheOverlay.cacheName)
      if (cacheOverlay != null) {
         // If the existing cache overlay is being replaced, create a new cache overlay
         if (tileTableCacheOverlay.parent.isAdded) {
            cacheOverlay = null
         }
      }
      if (cacheOverlay == null) {
         // Create a new GeoPackage tile provider and add to the map
         val tileDao = geoPackage.getTileDao(tileTableCacheOverlay.name)
         val tileTableScaling = TileTableScaling(geoPackage, tileDao)
         val tileScaling = tileTableScaling.get()
         val overlay = GeoPackageOverlayFactory.getBoundedOverlay(tileDao, resources.displayMetrics.density, tileScaling)
         val overlayOptions = createTileOverlayOptions(overlay)
         val tileOverlay = map?.addTileOverlay(overlayOptions)
         tileTableCacheOverlay.tileOverlay = tileOverlay

         // Check for linked feature tables
         tileTableCacheOverlay.clearFeatureOverlayQueries()
         val linker = FeatureTileTableLinker(geoPackage)
         val featureDaos = linker.getFeatureDaosForTileTable(tileDao.tableName)
         for (featureDao in featureDaos) {
            val featureTiles: FeatureTiles = DefaultFeatureTiles(activity, geoPackage, featureDao, resources.displayMetrics.density)

            // Add the feature overlay query
            val featureOverlayQuery = FeatureOverlayQuery(activity, overlay, featureTiles)
            tileTableCacheOverlay.addFeatureOverlayQuery(featureOverlayQuery)
         }
         cacheOverlay = tileTableCacheOverlay
      }
      // Add the cache overlay to the enabled cache overlays
      enabledCacheOverlays[cacheOverlay.cacheName] = cacheOverlay
   }

   /**
    * Add the GeoPackage Feature Table Cache Overlay
    * @param enabledCacheOverlays
    * @param featureTableCacheOverlay
    * @param geoPackage
    */
   private fun addGeoPackageFeatureCacheOverlay(
      enabledCacheOverlays: MutableMap<String, CacheOverlay?>,
      featureTableCacheOverlay: GeoPackageFeatureTableCacheOverlay,
      geoPackage: GeoPackage
   ) {
      // Retrieve the cache overlay if it already exists (and remove from cache overlays)
      var cacheOverlay = cacheOverlays.remove(featureTableCacheOverlay.cacheName)
      if (cacheOverlay != null) {
         // If the existing cache overlay is being replaced, create a new cache overlay
         if (featureTableCacheOverlay.parent.isAdded) {
            cacheOverlay = null
         }
         for (linkedTileTable in featureTableCacheOverlay.linkedTileTables) {
            cacheOverlays.remove(linkedTileTable.cacheName)
         }
      }
      if (cacheOverlay == null) {
         // Add the features to the map
         val featureDao = geoPackage.getFeatureDao(featureTableCacheOverlay.name)

         // If indexed, add as a tile overlay
         if (featureTableCacheOverlay.isIndexed) {
            val featureTiles: FeatureTiles = DefaultFeatureTiles(
               activity, geoPackage, featureDao,
               resources.displayMetrics.density
            )
            val maxFeaturesPerTile = if (featureDao.geometryType == GeometryType.POINT) {
               resources.getInteger(R.integer.geopackage_feature_tiles_max_points_per_tile)
            } else {
               resources.getInteger(R.integer.geopackage_feature_tiles_max_features_per_tile)
            }

            featureTiles.maxFeaturesPerTile = maxFeaturesPerTile
            val numberFeaturesTile = NumberFeaturesTile(activity)
            // Adjust the max features number tile draw paint attributes here as needed to
            // change how tiles are drawn when more than the max features exist in a tile
            featureTiles.maxFeaturesTileDraw = numberFeaturesTile
            // Adjust the feature tiles draw paint attributes here as needed to change how
            // features are drawn on tiles
            val featureOverlay = FeatureOverlay(featureTiles)
            featureOverlay.minZoom = featureTableCacheOverlay.minZoom

            // Get the tile linked overlay
            val overlay = GeoPackageOverlayFactory.getLinkedFeatureOverlay(featureOverlay, geoPackage)
            val featureOverlayQuery = FeatureOverlayQuery(activity, overlay, featureTiles)
            featureTableCacheOverlay.featureOverlayQuery = featureOverlayQuery
            val overlayOptions = createFeatureTileOverlayOptions(overlay)
            val tileOverlay = map?.addTileOverlay(overlayOptions)
            featureTableCacheOverlay.tileOverlay = tileOverlay
         } else {
            val maxFeaturesPerTable = if (featureDao.geometryType == GeometryType.POINT) {
               resources.getInteger(R.integer.geopackage_features_max_points_per_table)
            } else {
               resources.getInteger(R.integer.geopackage_features_max_features_per_table)
            }

            val projection = featureDao.projection
            val shapeConverter = GoogleMapShapeConverter(projection)
            featureDao.queryForAll().use { cursor ->
               val totalCount = cursor.count
               var count = 0
               while (cursor.moveToNext()) {
                  try {
                     val featureRow = cursor.row
                     val geometryData = featureRow.geometry
                     if (geometryData != null && !geometryData.isEmpty) {
                        val geometry = geometryData.geometry
                        if (geometry != null) {
                           val shape = shapeConverter.toShape(geometry)
                           // Set the Shape Marker, PolylineOptions, and PolygonOptions here if needed to change color and style
                           featureTableCacheOverlay.addShapeToMap(featureRow.id, shape, map)
                           if (++count >= maxFeaturesPerTable) {
                              if (count < totalCount) {
                                 Toast.makeText(application, featureTableCacheOverlay.cacheName + "- added " + count + " of " + totalCount, Toast.LENGTH_LONG).show()
                              }
                              break
                           }
                        }
                     }
                  } catch (e: Exception) {
                     Log.e(LOG_NAME, "Failed to display feature. GeoPackage: " + geoPackage.name + ", Table: " + featureDao.tableName + ", Row: " + cursor.position, e)
                  }
               }
            }
         }
         cacheOverlay = featureTableCacheOverlay
      }

      // Add the cache overlay to the enabled cache overlays
      enabledCacheOverlays[cacheOverlay.cacheName] = cacheOverlay
   }

   /**
    * Create Feature Tile Overlay Options with the default z index for tile layers drawn from features
    * @param tileProvider
    * @return
    */
   private fun createFeatureTileOverlayOptions(tileProvider: TileProvider): TileOverlayOptions {
      return createTileOverlayOptions(tileProvider, -1)
   }

   /**
    * Create Tile Overlay Options with the default z index for tile layers
    * @param tileProvider
    * @param zIndex
    * @return
    */
   private fun createTileOverlayOptions(
      tileProvider: TileProvider,
      zIndex: Int = -2
   ): TileOverlayOptions {
      val overlayOptions = TileOverlayOptions()
      overlayOptions.tileProvider(tileProvider)
      overlayOptions.zIndex(zIndex.toFloat())
      return overlayOptions
   }

   private fun updateMapView() {
      map?.mapType = preferences.getInt(getString(R.string.baseLayerKey), resources.getInteger(R.integer.baseLayerDefaultValue))

      val xyz = preferences.getString(getString(R.string.recentMapXYZKey), getString(R.string.recentMapXYZDefaultValue))!!
      val values = xyz.split(",")

      val longitude = values.getOrNull(0)?.toDoubleOrNull() ?: 0.0
      val latitude = values.getOrNull(1)?.toDoubleOrNull() ?: 0.0
      val zoom = values.getOrNull(2)?.toFloatOrNull() ?: 1f

      map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), zoom))
   }

   private fun saveMapView() {
      val position = map?.cameraPosition
      val xyz = "${position?.target?.longitude?.toString()},${position?.target?.latitude?.toString()},${position?.zoom?.toString()}"
      preferences.edit().putString(resources.getString(R.string.recentMapXYZKey), xyz).apply()
   }

   override fun onError(error: Throwable) {}

   private fun getFilterTitle(): String {
      val timeFilterId = preferences.getInt(resources.getString(R.string.activeTimeFilterKey), resources.getInteger(R.integer.time_filter_last_month))
      val locationTimeFilterId = preferences.getInt(resources.getString(R.string.activeLocationTimeFilterKey), resources.getInteger(R.integer.time_filter_last_month))

      return if (timeFilterId != resources.getInteger(R.integer.time_filter_none) ||
         locationTimeFilterId != resources.getInteger(R.integer.time_filter_none) ||
         preferences.getBoolean(resources.getString(R.string.activeImportantFilterKey), false) ||
         preferences.getBoolean(resources.getString(R.string.activeFavoritesFilterKey), false)
      ) {
         "Showing filtered results."
      } else {
         ""
      }
   }

   private fun hideKeyboard() {
      val inputMethodManager = application.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
      if (requireActivity().currentFocus != null) {
         inputMethodManager.hideSoftInputFromWindow(requireActivity().currentFocus?.windowToken, 0)
      }
   }

   companion object {
      private val LOG_NAME = MapFragment::class.java.name
      private const val MAP_VIEW_STATE = "MAP_VIEW_STATE"
      private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
      private const val MARKER_REFRESH_INTERVAL: Int = 300 * 1000
   }
}