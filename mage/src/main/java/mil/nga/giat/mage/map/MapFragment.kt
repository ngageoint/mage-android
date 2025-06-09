package mil.nga.giat.mage.map

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.GONE
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewGroup.VISIBLE
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.TileProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.ktx.awaitMap
import com.google.maps.android.ktx.cameraIdleEvents
import com.google.maps.android.ktx.cameraMoveStartedEvents
import com.google.maps.android.ktx.mapClickEvents
import com.google.maps.android.ktx.mapLongClickEvents
import com.google.maps.android.ktx.markerClickEvents
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mil.nga.gars.GARS
import mil.nga.gars.tile.GARSTileProvider
import mil.nga.geopackage.BoundingBox
import mil.nga.geopackage.GeoPackage
import mil.nga.geopackage.GeoPackageCache
import mil.nga.geopackage.GeoPackageFactory
import mil.nga.geopackage.extension.nga.link.FeatureTileTableLinker
import mil.nga.geopackage.extension.nga.scale.TileTableScaling
import mil.nga.geopackage.map.geom.GoogleMapShapeConverter
import mil.nga.geopackage.map.tiles.overlay.FeatureOverlay
import mil.nga.geopackage.map.tiles.overlay.FeatureOverlayQuery
import mil.nga.geopackage.map.tiles.overlay.GeoPackageOverlayFactory
import mil.nga.geopackage.tiles.TileBoundingBoxUtils
import mil.nga.geopackage.tiles.features.DefaultFeatureTiles
import mil.nga.geopackage.tiles.features.FeatureTiles
import mil.nga.geopackage.tiles.features.custom.NumberFeaturesTile
import mil.nga.giat.mage.LandingViewModel
import mil.nga.giat.mage.LandingViewModel.NavigableType
import mil.nga.giat.mage.R
import mil.nga.giat.mage.coordinate.CoordinateFormatter
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.data.datasource.layer.LayerLocalDataSource
import mil.nga.giat.mage.data.datasource.location.LocationLocalDataSource
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.databinding.FragmentMapBinding
import mil.nga.giat.mage.feed.item.FeedItemActivity
import mil.nga.giat.mage.filter.FilterActivity
import mil.nga.giat.mage.geopackage.media.GeoPackageMediaActivity
import mil.nga.giat.mage.glide.transform.LocationAgeTransformation
import mil.nga.giat.mage.location.LocationAccess
import mil.nga.giat.mage.location.LocationPolicy
import mil.nga.giat.mage.map.MapViewModel.FeedState
import mil.nga.giat.mage.map.annotation.MapAnnotation
import mil.nga.giat.mage.map.cache.CacheOverlay
import mil.nga.giat.mage.map.cache.CacheOverlayFilter
import mil.nga.giat.mage.map.cache.CacheOverlayType
import mil.nga.giat.mage.map.cache.CacheProvider
import mil.nga.giat.mage.map.cache.CacheProvider.OnCacheOverlayListener
import mil.nga.giat.mage.map.cache.GeoPackageCacheOverlay
import mil.nga.giat.mage.map.cache.GeoPackageFeatureTableCacheOverlay
import mil.nga.giat.mage.map.cache.GeoPackageTileTableCacheOverlay
import mil.nga.giat.mage.map.cache.URLCacheOverlay
import mil.nga.giat.mage.map.cache.WMSCacheOverlay
import mil.nga.giat.mage.map.cache.XYZDirectoryCacheOverlay
import mil.nga.giat.mage.map.detail.FeatureAction
import mil.nga.giat.mage.map.detail.FeatureDetails
import mil.nga.giat.mage.map.detail.GeoPackageFeatureAction
import mil.nga.giat.mage.map.detail.GeoPackageFeatureDetails
import mil.nga.giat.mage.map.detail.MapStaticFeatureDetails
import mil.nga.giat.mage.map.detail.ObservationAction
import mil.nga.giat.mage.map.detail.ObservationMapDetails
import mil.nga.giat.mage.map.detail.StaticFeatureAction
import mil.nga.giat.mage.map.detail.UserAction
import mil.nga.giat.mage.map.detail.UserMapDetails
import mil.nga.giat.mage.map.detail.UserPhoneAction
import mil.nga.giat.mage.map.detail.UserPhoneDetails
import mil.nga.giat.mage.map.feature.FeatureCollection
import mil.nga.giat.mage.map.feature.FeedCollection
import mil.nga.giat.mage.map.feature.StaticFeatureCollection
import mil.nga.giat.mage.map.navigation.bearing.StraightLineNavigation
import mil.nga.giat.mage.map.preference.MapPreferencesActivity
import mil.nga.giat.mage.observation.ObservationLocation
import mil.nga.giat.mage.observation.edit.ObservationEditActivity
import mil.nga.giat.mage.observation.view.ObservationViewActivity
import mil.nga.giat.mage.profile.ProfileActivity
import mil.nga.giat.mage.sdk.exceptions.UserException
import mil.nga.giat.mage.search.GeocoderResult
import mil.nga.giat.mage.search.SearchResponseType
import mil.nga.giat.mage.ui.search.PlacenameSearch
import mil.nga.giat.mage.ui.sheet.DragHandle
import mil.nga.giat.mage.ui.theme.MageTheme3
import mil.nga.giat.mage.utils.googleMapsUri
import mil.nga.mgrs.MGRS
import mil.nga.mgrs.grid.GridType
import mil.nga.mgrs.tile.MGRSTileProvider
import mil.nga.proj.ProjectionConstants
import mil.nga.proj.ProjectionFactory
import mil.nga.sf.Geometry
import mil.nga.sf.GeometryType
import mil.nga.sf.Point
import java.util.Date
import javax.inject.Inject
import kotlin.collections.set

@AndroidEntryPoint
class MapFragment : Fragment(),
   View.OnClickListener,
   LocationSource,
   OnCacheOverlayListener,
   Observer<android.location.Location>
{
   private enum class LocateState {
      OFF, FOLLOW;

      operator fun next(): LocateState {
         return if (ordinal < entries.size - 1) entries[ordinal + 1] else OFF
      }
   }

   @Inject lateinit var application: Application
   @Inject lateinit var preferences: SharedPreferences
   @Inject lateinit var locationAccess: LocationAccess
   @Inject lateinit var locationPolicy: LocationPolicy
   @Inject lateinit var userLocalDataSource: UserLocalDataSource
   @Inject lateinit var layerLocalDataSource: LayerLocalDataSource
   @Inject lateinit var eventLocalDataSource: EventLocalDataSource
   @Inject lateinit var locationLocalDataSource: LocationLocalDataSource
   @Inject lateinit var cacheProvider: CacheProvider

   private lateinit var binding: FragmentMapBinding

   private val viewModel: MapViewModel by activityViewModels()
   private val landingViewModel: LandingViewModel by activityViewModels()

   private var map: GoogleMap? = null

   private var straightLineNavigation: StraightLineNavigation? = null
   private var locateState = LocateState.OFF
   private var currentUser: User? = null
   private var currentEventId: Long = -1
   private var locationChangedListener: OnLocationChangedListener? = null

   private var locationProvider: LiveData<android.location.Location>? = null
   private var observations: FeatureCollection<Long>? = null
   private var locations: FeatureCollection<Long>? = null
   private var feeds: FeedCollection? = null
   private var staticFeatureCollection: StaticFeatureCollection? = null
   private var searchMarker: Marker? = null
   private var selectedMarker: Marker? = null
   private var feedLiveData: Map<String, LiveData<FeedState>> = emptyMap()
   private var cacheOverlays: MutableMap<String, CacheOverlay?> = HashMap()

   private var cacheBoundingBox: BoundingBox? = null
   private lateinit var geoPackageCache: GeoPackageCache

   private var gridTileOverlay: TileOverlay? = null

   private var showMgrs = false
   private lateinit var mgrsTileProvider: MGRSTileProvider

   private var showGars = false
   private lateinit var garsTileProvider: GARSTileProvider

   private lateinit var searchBottomSheetBehavior: BottomSheetBehavior<View>
   private lateinit var featureBottomSheetBehavior: BottomSheetBehavior<View>

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setHasOptionsMenu(true)
   }

   override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
   ): View {
      binding = FragmentMapBinding.inflate(inflater, container, false)

      viewModel.showMapSearchButton.observe(viewLifecycleOwner) { showMapSearch ->
         binding.mapSearchButton.visibility = if (showMapSearch) {
            VISIBLE
         } else {
            GONE
         }
      }

      binding.searchBottomSheetCompose.setContent {
         MageTheme3 {
            Column {
               DragHandle()
               PlacenameSearch(
                  onSearchResultTap = { type, result ->  onSearchResultTap(type, result) }
               )
            }
         }
      }

      viewModel.observationMap.observe(viewLifecycleOwner) { state ->
         binding.bottomSheetCompose.setContent {
            ObservationMapDetails(state) { onObservationAction(it) }
         }
      }

      viewModel.location.observe(viewLifecycleOwner) { state ->
         binding.bottomSheetCompose.setContent {
            UserMapDetails(state) { onUserAction(it) }
         }
      }

      viewModel.feedItem.observe(viewLifecycleOwner) { state ->
         binding.bottomSheetCompose.setContent {
            FeatureDetails(state, onAction = {
               onFeedItemAction(it)
            })
         }
      }

      viewModel.staticFeature.observe(viewLifecycleOwner) { state ->
         binding.bottomSheetCompose.setContent {
            MapStaticFeatureDetails(state) { onStaticFeatureAction(it) }
         }
      }

      viewModel.geoPackageFeature.observe(viewLifecycleOwner) {
         binding.bottomSheetCompose.setContent {
            GeoPackageFeatureDetails(it) { onGeoPackageFeatureAction(it) }
         }
      }

      binding.userPhoneView.setContent {
         UserPhoneDetails(viewModel.userPhone) { action ->
            when (action) {
               is UserPhoneAction.Call -> onUserCall(action.user)
               is UserPhoneAction.Message -> onUserMessage(action.user)
               is UserPhoneAction.Dismiss -> viewModel.selectUserPhone(null)
            }
         }
      }

      binding.mapSearchButton.setOnClickListener { showSearchBottomSheet() }
      binding.reportLocation.setOnClickListener { onToggleReportLocation() }
      binding.newObservationButton.setOnClickListener { onNewObservation() }

      MapsInitializer.initialize(requireContext())
      binding.mapSettings.setOnClickListener(this)
      val mapState = savedInstanceState?.getBundle(MAP_VIEW_STATE)

      binding.centerCoordinateContainer.setOnClickListener { onCenterCoordinateClick() }

      binding.mapView.onCreate(mapState)

      searchBottomSheetBehavior = BottomSheetBehavior.from(binding.searchBottomSheet)
      searchBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
      searchBottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
         override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
               BottomSheetBehavior.STATE_EXPANDED -> {
                  map?.setPadding(0, 0, 0, bottomSheet.height)
               }
               else -> {
                  map?.setPadding(0, 0, 0, 0)
               }
            }
         }

         override fun onSlide(bottomSheet: View, slideOffset: Float) {}
      })

      featureBottomSheetBehavior = BottomSheetBehavior.from(binding.featureBottomSheet)
      featureBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
      featureBottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
         override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
               deselectMarker()
               viewModel.deselectFeature()
            }
         }

         override fun onSlide(bottomSheet: View, slideOffset: Float) {}
      })

      locationProvider = locationPolicy.bestLocationProvider
      geoPackageCache = GeoPackageCache(GeoPackageFactory.getManager(application))

      mgrsTileProvider = MGRSTileProvider(application)
      garsTileProvider = GARSTileProvider(application)

      return binding.root
   }

   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      super.onViewCreated(view, savedInstanceState)

      viewModel.items.observe(viewLifecycleOwner) {
         onFeedItems(it)
      }

      val isRestore = savedInstanceState != null
      viewLifecycleOwner.lifecycleScope.launch {
         viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val googleMap = binding.mapView.awaitMap()
            map = googleMap
            updateMapView()

            if (!isRestore) {
               googleMap.uiSettings.isMyLocationButtonEnabled = false

               feeds = FeedCollection(application, googleMap, 32)
               observations = FeatureCollection(application, googleMap, 32)
               locations = FeatureCollection(application, googleMap, 42) {
                  mutableListOf(LocationAgeTransformation(application, it.timestamp))
               }
               staticFeatureCollection = StaticFeatureCollection(application, googleMap)

               val sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as? SensorManager
               straightLineNavigation = StraightLineNavigation(
                  sensorManager,
                  googleMap,
                  requireActivity().findViewById(R.id.straight_line_nav_container),
                  requireActivity()
               )  {
                  landingViewModel.stopNavigation()
               }

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
               googleMap.cameraMoveStartedEvents().collect { onCameraMoveStarted(it) }
            }

            launch {
               googleMap.cameraIdleEvents().collect { onCameraIdle() }
            }

            viewModel.observations.observe(viewLifecycleOwner) { annotations ->
               onObservations(annotations)
            }

            viewModel.locations.observe(viewLifecycleOwner) { annotations ->
               onLocations(annotations)
            }

            viewModel.featureLayers.observe(viewLifecycleOwner) { layers ->
               onStaticLayers(layers)
            }

            landingViewModel.navigateTo.observe(viewLifecycleOwner) {
               navigateTo(it)
            }

            launch(Dispatchers.IO) {
               while(isActive) {
                  delay(MARKER_REFRESH_INTERVAL.toLong())

                  if (locations?.isVisible == true) {
                     launch(Dispatchers.Main) {
                        locations?.refreshMarkerIcons()
                     }
                  }
               }
            }
         }
      }

      addBackClickHandler()
   }

   private fun addBackClickHandler() {
      val onBackPressedCallback = object : OnBackPressedCallback(true) {
         override fun handleOnBackPressed() {
            if (searchBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED ||
               searchBottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
               searchBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
            else if (featureBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED ||
               featureBottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
               viewModel.deselectFeature()
               featureBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
            else {
               //if no bottom sheets are consuming the back press, allow default back behavior by disabling this callback and re-triggering the back press
               isEnabled = false
               requireActivity().onBackPressedDispatcher.onBackPressed()
               isEnabled = true //re-enable for next time
            }
         }
      }

      //add back handler
      requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
   }

   private fun onObservationAction(action: Any) {
      when(action) {
         is ObservationAction.Favorite -> onObservationFavorite(action.observation)
         is ObservationAction.Location -> onLocation(action.geometry)
         is ObservationAction.Details -> onObservationDetails(action.id)
         is ObservationAction.Directions -> {
            onDirections(
               LandingViewModel.Navigable(
                  action.id,
                  NavigableType.OBSERVATION,
                  action.geometry,
                  action.image
               )
            )
         }
      }
   }

   private fun onObservationDetails(id: Long) {
      featureBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

      val intent = Intent(context, ObservationViewActivity::class.java)
      intent.putExtra(ObservationViewActivity.OBSERVATION_ID_EXTRA, id)
      intent.putExtra(ObservationViewActivity.INITIAL_LOCATION_EXTRA, map?.cameraPosition?.target)
      intent.putExtra(ObservationViewActivity.INITIAL_ZOOM_EXTRA, map?.cameraPosition?.zoom)
      startActivity(intent)
   }

   private fun onDirections(navigable: LandingViewModel.Navigable<Any>) {
      featureBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
      searchBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

      // present a dialog to pick between android system map and straight line
      AlertDialog.Builder(requireActivity())
         .setTitle( application.resources.getString(R.string.navigation_choice_title))
         .setItems(R.array.navigationOptions) { _: DialogInterface?, which: Int ->
            when (which) {
               0 -> {
                  val intent = Intent(Intent.ACTION_VIEW, navigable.geometry.googleMapsUri())
                  startActivity(intent)
               }
               1 -> {
                  // TODO might be ok to start with null location and let user know
                  val location: android.location.Location? = locationProvider?.value
                  if (location == null) {
                     if (locationAccess.isLocationGranted()) {
                        AlertDialog.Builder(requireActivity())
                           .setTitle(application.resources.getString(R.string.location_missing_title))
                           .setMessage(application.resources.getString(R.string.location_missing_message))
                           .setPositiveButton(android.R.string.ok, null)
                           .show()
                     } else {
                        AlertDialog.Builder(requireActivity())
                           .setTitle(application.resources.getString(R.string.location_access_denied_title))
                           .setMessage(application.resources.getString(R.string.location_access_directions_message))
                           .setPositiveButton(R.string.settings) { _: DialogInterface?, _: Int ->
                              val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                              intent.data = Uri.fromParts("package", application.packageName, null)
                              startActivity(intent)
                           }
                           .setNegativeButton(android.R.string.cancel, null)
                           .show()
                     }
                  } else {
                     landingViewModel.startNavigation(navigable)
                  }
               }
            }
         }
         .setNegativeButton(android.R.string.cancel, null)
         .show()
   }

   private fun navigateTo(navigable: LandingViewModel.Navigable<*>?) {
      if (navigable != null) {
         val location = locationProvider?.value!!
         val centroid = navigable.geometry.centroid
         val latLng = LatLng(centroid.y, centroid.x)

         if (straightLineNavigation?.isNavigating() == true) {
            straightLineNavigation?.updateDestination(latLng)
         } else {
            straightLineNavigation?.startNavigation(location, latLng, navigable.icon)
            featureBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            searchBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            map?.center(Point(location.longitude, location.latitude), 16f)
         }
      }
   }

   private fun onObservationFavorite(observationMap: ObservationMapState) {
      viewModel.toggleFavorite(observationMap)
   }

   private fun onLocation(geometry: Geometry) {
      val point = geometry.centroid
      val location = CoordinateFormatter(requireContext()).format(LatLng(point.y, point.x))

      val clipboard = requireContext().getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as? ClipboardManager
      val clip = ClipData.newPlainText("Observation Location", location)
      if (clipboard == null || clip == null) return
      clipboard.setPrimaryClip(clip)

      Snackbar.make(requireActivity().findViewById(android.R.id.content), R.string.location_text_copy_message, Snackbar.LENGTH_SHORT).show()
   }

   private fun onUserAction(action: Any) {
      when(action) {
         is UserAction.Details -> onUserDetails(action.id)
         is UserAction.Location -> onLocation(action.geometry)
         is UserAction.Phone -> onUserPhone(action.user)
         is UserAction.Email -> onUserEmail(action.user)
         is UserAction.Directions -> {
            onDirections(
               LandingViewModel.Navigable(
                  action.id,
                  NavigableType.USER,
                  action.geometry,
                  action.icon
               )
            )
         }
      }
   }

   private fun onUserDetails(id: Long) {
      featureBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

      val profileView = Intent(context, ProfileActivity::class.java)
      profileView.putExtra(ProfileActivity.USER_ID_EXTRA, id)
      activity?.startActivity(profileView)
   }

   private fun onUserEmail(user: UserMapState) {
      val intent = Intent(Intent.ACTION_VIEW)
      intent.data = Uri.parse("mailto:" + user.email)
      startActivity(intent)
   }

   private fun onUserPhone(user: UserMapState) {
      featureBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
      searchBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
      viewModel.selectUserPhone(user)
   }

   private fun onUserCall(user: User) {
      val callIntent = Intent(Intent.ACTION_DIAL)
      callIntent.data = Uri.parse("tel:" + user.primaryPhone)
      startActivity(callIntent)
   }

   private fun onUserMessage(user: User) {
      val intent = Intent(Intent.ACTION_VIEW)
      intent.data = Uri.parse("sms:" + user.primaryPhone)
      startActivity(intent)
   }

   private fun onFeedItemAction(action: Any) {
      when(action) {
         is FeatureAction.Details<*> -> onFeedItemDetails(action.id)
         is FeatureAction.Location -> onLocation(action.geometry)
         is FeatureAction.Directions<*> -> {
            onDirections(
               LandingViewModel.Navigable(
                  action.id,
                  NavigableType.FEED,
                  action.geometry,
                  action.image
               )
            )
         }
      }
   }

   private fun onFeedItemDetails(feedItemId: Any?) {
      (feedItemId as? FeedItemId)?.let { id ->
         featureBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

         val intent = FeedItemActivity.intent(application, id.feedId, id.itemId)
         startActivity(intent)
      }
   }

   private fun onGeoPackageFeatureAction(action: Any) {
      when(action) {
         is GeoPackageFeatureAction.Location -> onLocation(action.geometry)
         is GeoPackageFeatureAction.Media -> onGeoPackageMedia(action)
         is GeoPackageFeatureAction.Directions -> {
            onDirections(
               LandingViewModel.Navigable(
                  "geopackage",
                  NavigableType.OTHER,
                  action.geometry,
                  action.icon
               )
            )
         }
      }
   }

   private fun onGeoPackageMedia(action: GeoPackageFeatureAction.Media) {
      val intent = GeoPackageMediaActivity.intent(
         application,
         action.geoPackage,
         action.mediaTable,
         action.mediaId
      )

      startActivity(intent)
   }

   private fun onStaticFeatureAction(action: Any) {
      when(action) {
         is StaticFeatureAction.Location -> onLocation(action.geometry)
         is StaticFeatureAction.Directions -> {
            onDirections(
               LandingViewModel.Navigable(
                  "feature",
                  NavigableType.OTHER,
                  action.geometry,
                  action.icon
               )
            )
         }
      }
   }

   override fun onChanged(value: android.location.Location) {
      locationChangedListener?.onLocationChanged(value)
      straightLineNavigation?.updateUserLocation(value)

      if (locateState == LocateState.FOLLOW) {
         val cameraPosition = CameraPosition.Builder()
            .target(LatLng(value.latitude, value.longitude))
            .zoom(17f)
            .bearing(value.bearing)
            .build()
         map?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
      }

      if (preferences.getBoolean(application.resources.getString(R.string.showHeadingKey), false)) {
         startHeading()
      }
   }

   override fun onDestroyView() {
      super.onDestroyView()
      binding.mapView.onDestroy()

      observations?.clear()
      observations = null

      locations?.clear()
      locations = null

      feeds?.clear()
      feeds = null

      searchMarker?.remove()

      gridTileOverlay?.remove()
      gridTileOverlay = null

      geoPackageCache.closeAll()
      cacheOverlays.clear()

      staticFeatureCollection?.clear()

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
      val currentEvent = eventLocalDataSource.currentEvent
      var currentEventId = currentEventId
      if (currentEvent != null) {
         currentEventId = currentEvent.id
      }

      if (this.currentEventId != currentEventId) {
         this.currentEventId = currentEventId
         observations?.clear()
         locations?.clear()
      }

      cacheProvider.registerCacheOverlayListener(this)

      binding.zoomButton.setOnClickListener { zoomToLocation() }

      // Set visibility on map markers as preferences may have changed
      observations?.setVisibility(preferences.getBoolean(resources.getString(R.string.showObservationsKey), true))
      locations?.setVisibility(preferences.getBoolean(resources.getString(R.string.showLocationsKey), true))

      // maybe need to turn off heading
      if (!preferences.getBoolean( resources.getString(R.string.showHeadingKey), false)) {
         straightLineNavigation?.stopHeading()
      }

      // Check if any map preferences changed that I care about
      try {
         if (locationAccess.isLocationGranted()) {
            googleMap.isMyLocationEnabled = true
            googleMap.setLocationSource(this)
         } else {
            googleMap.isMyLocationEnabled = false
            googleMap.setLocationSource(null)
         }
      } catch (_: SecurityException) {}

      if (showMgrs || showGars) {
         val tileProvider = if (showMgrs) mgrsTileProvider else garsTileProvider
         gridTileOverlay = googleMap.addTileOverlay(TileOverlayOptions().tileProvider(tileProvider))
      }
      landingViewModel.setFilterText(getFilterTitle())

      currentEvent?.id?.let {
         viewModel.setEvent(it)
      }
   }

   private fun onSearchResultTap(type: SearchResponseType, result: GeocoderResult) {
      searchMarker?.remove()

      searchMarker = map?.addMarker(
         MarkerOptions()
            .title(result.name)
            .snippet(result.address)
            .position(result.location)
            .icon(BitmapDescriptorFactory.defaultMarker(211F))
      )
      searchMarker?.showInfoWindow()

      val zoom = when (type) {
         SearchResponseType.MGRS -> {
            val gridType = MGRS.precision(result.address)
            mgrsTileProvider.getGrid(gridType)?.let { grid ->
               val maxZoom = if (gridType == GridType.GZD) {
                  mgrsTileProvider.getGrid(GridType.HUNDRED_KILOMETER).minZoom.minus(1)
               } else {
                  grid.linesMaxZoom
               }
               zoomLevel(grid.linesMinZoom, maxZoom)
            }
         }
         SearchResponseType.GARS -> {
            val gridType = GARS.precision(result.address)
            garsTileProvider.getGrid(gridType)?.let { grid ->
               zoomLevel(grid.linesMinZoom, grid.linesMaxZoom)
            }
         }
         else -> null
      } ?: 18

      val position = CameraPosition.builder()
         .target(result.location)
         .zoom(zoom.toFloat()).build()

      map?.animateCamera(CameraUpdateFactory.newCameraPosition(position))
   }

   private fun zoomLevel(minZoom: Int, maxZoom: Int?) : Int {
      var zoomLevel = 18
      val mapZoom = map?.cameraPosition?.zoom
      if (mapZoom != null) {
         zoomLevel = mapZoom.toInt()
         if (mapZoom < minZoom) {
            zoomLevel = minZoom
         } else if (maxZoom != null && mapZoom >= maxZoom + 1) {
            zoomLevel = maxZoom
         }
      }
      return zoomLevel
   }

   private fun onObservations(annotations: List<MapAnnotation<Long>>) {
      observations?.add(annotations)

      landingViewModel.navigateTo.value?.let { navigable ->
         if (navigable.type == NavigableType.OBSERVATION) {
            annotations.find { it.id == navigable.id }?.let { annotation ->
               val centroid = annotation.geometry.centroid
               straightLineNavigation?.updateDestination(LatLng(centroid.y, centroid.x))
            }
         }
      }
   }

   private fun onLocations(annotations: List<MapAnnotation<Long>>) {
      locations?.add(annotations)

      landingViewModel.navigateTo.value?.let { navigable ->
         if (navigable.type == NavigableType.USER) {
            annotations.find { it.id == navigable.id }?.let { annotation ->
               val centroid = annotation.geometry.centroid
               straightLineNavigation?.updateDestination(LatLng(centroid.y, centroid.x))
            }
         }
      }
   }

   private fun updateReportLocationButton() {
      val serverLocationServiceDisabled = preferences.getBoolean("gLocationServiceDisabled", false)
      val memberOfEvent = userLocalDataSource.isCurrentUserPartOfCurrentEvent()
      binding.preciseLocationDenied.visibility = View.GONE

      if (serverLocationServiceDisabled || !memberOfEvent) {
         binding.reportLocation.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(application, R.color.md_grey_500))
         binding.reportLocation.setImageResource(R.drawable.ic_outline_location_disabled_24)
      } else if (locationAccess.isLocationDenied()) {
         binding.reportLocation.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(application, R.color.md_red_500))
         binding.reportLocation.setImageResource(R.drawable.ic_outline_location_disabled_24)
      } else {
         val reportingLocation = preferences.getBoolean(resources.getString(R.string.reportLocationKey), false)
         if (reportingLocation) {
            val tint = if (locationAccess.isPreciseLocationGranted()) {
               ContextCompat.getColor(application, R.color.md_green_500)
            } else ContextCompat.getColor(application, R.color.md_amber_700)
            binding.reportLocation.imageTintList = ColorStateList.valueOf(tint)
            binding.reportLocation.setImageResource(R.drawable.ic_my_location_white_24dp)

            if (!locationAccess.isPreciseLocationGranted()) {
               binding.preciseLocationDenied.visibility = View.VISIBLE
            }
         } else {
            binding.reportLocation.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(application, R.color.md_red_500))
            binding.reportLocation.setImageResource(R.drawable.ic_outline_location_disabled_24)
         }
      }
   }

   private fun zoomToLocation() {
      if (locationAccess.isLocationDenied()) {
         AlertDialog.Builder(requireActivity())
            .setTitle(application.resources.getString(R.string.location_access_denied_title))
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
         LocateState.OFF -> binding.zoomButton.isSelected = false
         LocateState.FOLLOW -> {
            binding.zoomButton.isSelected = true
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

   private fun onToggleReportLocation() {
      if (!userLocalDataSource.isCurrentUserPartOfCurrentEvent()) {
         AlertDialog.Builder(requireActivity())
            .setTitle(application.resources.getString(R.string.no_event_title))
            .setMessage(application.resources.getString(R.string.location_no_event_message))
            .setPositiveButton(android.R.string.ok, null)
            .show()

         return
      }

      if (locationAccess.isLocationDenied()) {
         AlertDialog.Builder(requireActivity())
            .setTitle(application.resources.getString(R.string.location_access_denied_title))
            .setMessage(application.resources.getString(R.string.location_access_report_message))
            .setPositiveButton(R.string.settings) { _: DialogInterface?, _: Int ->
               val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
               intent.data = Uri.fromParts("package", application.packageName, null)
               startActivity(intent)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
         return
      } else
         if (!locationAccess.isPreciseLocationGranted()) {
         val reportingLocation = preferences.getBoolean(resources.getString(R.string.reportLocationKey), false)
         val reportingButtonText = if (reportingLocation) {
            application.resources.getString(R.string.location_stop_coarse_reporting)
         } else {
            application.resources.getString(R.string.location_start_coarse_reporting)
         }

         AlertDialog.Builder(requireActivity())
            .setTitle(application.resources.getString(R.string.location_precise_denied_access_title))
            .setMessage(application.resources.getString(R.string.location_precise_denied_settings_message))
            .setPositiveButton(R.string.location_enable_settings) { _: DialogInterface?, _: Int ->
               val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
               intent.data = Uri.fromParts("package", application.packageName, null)
               startActivity(intent)
            }
            .setNegativeButton(reportingButtonText) { _: DialogInterface?, _: Int ->
               toggleReportLocation()
            }
            .show()

      } else {
         toggleReportLocation()
      }
   }

   private fun toggleReportLocation() {
      val serverLocationServiceDisabled: Boolean = preferences.getBoolean("gLocationServiceDisabled", false)
      val message = if (serverLocationServiceDisabled) {
         resources.getString(R.string.report_location_disabled)
      } else {
         val key = resources.getString(R.string.reportLocationKey)
         val reportLocation = !preferences.getBoolean(key, false)
         preferences.edit().putBoolean(key, reportLocation).apply()

         if (reportLocation) resources.getString(R.string.report_location_start) else resources.getString(
            R.string.report_location_stop
         )
      }

      updateReportLocationButton()

      val snackbar = Snackbar.make(requireActivity().findViewById(R.id.coordinator_layout), message, Snackbar.LENGTH_SHORT)
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
      binding.mapView.onLowMemory()
   }

   override fun onResume() {
      super.onResume()

      updateReportLocationButton()
      toggleCoordinateTarget()

      try {
         currentUser = userLocalDataSource.readCurrentUser()
      } catch (ue: UserException) {
         Log.e(LOG_NAME, "Could not find current user.", ue)
      }

      binding.mapView.onResume()
      showMgrs = preferences.getBoolean(resources.getString(R.string.showMGRSKey), false)
      showGars = preferences.getBoolean(resources.getString(R.string.showGARSKey), false)

      val event = eventLocalDataSource.currentEvent
      val available = layerLocalDataSource.readByEvent(event).any { !it.isLoaded }

      binding.availableLayerDownloads.visibility = if (available) View.VISIBLE else View.GONE
   }

   override fun onPause() {
      super.onPause()

      binding.mapView.onPause()

      observations?.clear()
      locations?.clear()
      feeds?.clear()
      staticFeatureCollection?.clear()

      feedLiveData.values.forEach {
         it.removeObservers(viewLifecycleOwner)
      }

      cacheProvider.unregisterCacheOverlayListener(this)

      map?.let {
         saveMapView()
         it.setLocationSource(null)
         gridTileOverlay?.remove()
      }
   }

   private fun onFeedItems(feedLiveData: Map<String, LiveData<FeedState>>) {
      feedLiveData.values.forEach {
         it.removeObservers(viewLifecycleOwner)
      }

      this.feeds?.clear()
      this.feedLiveData = feedLiveData

      feedLiveData.values.forEach {
         it.observe(viewLifecycleOwner) { items: FeedState? ->
            onFeedItems(items)
         }
      }
   }

   private fun onFeedItems(feedState: FeedState?) {
      if (feedState != null) {
         feeds?.add(feedState)

         landingViewModel.navigateTo.value?.let { navigable ->
            if (navigable.type == NavigableType.FEED) {
               feedState.items.find { FeedItemId(feedState.feed.id, it.id) == navigable.id }?.let { annotation ->
                  val centroid = annotation.geometry.centroid
                  straightLineNavigation?.updateDestination(LatLng(centroid.y, centroid.x))
               }
            }
         }
      }
   }

   private fun onNewObservation() {
      var location: ObservationLocation? = null

      val user = userLocalDataSource.readCurrentUser() ?: return

      // if there is not a location from the location service, then try to pull one from the database.
      if (locationProvider?.value == null) {
         val locations = locationLocalDataSource.getCurrentUserLocations(user, 1, true)
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

      if (!userLocalDataSource.isCurrentUserPartOfCurrentEvent()) {
         AlertDialog.Builder(requireActivity())
            .setTitle(application.resources.getString(R.string.no_event_title))
            .setMessage(application.resources.getString(R.string.observation_no_event_message))
            .setPositiveButton(android.R.string.ok, null)
            .show()
      } else if (location != null) {
         newObservation(location)
      } else {
         if (locationAccess.isLocationGranted()) {
            AlertDialog.Builder(requireActivity())
               .setTitle(application.resources.getString(R.string.location_missing_title))
               .setMessage(application.resources.getString(R.string.location_missing_message))
               .setPositiveButton(android.R.string.ok, null)
               .show()
         } else {
            AlertDialog.Builder(requireActivity())
               .setTitle(application.resources.getString(R.string.location_access_denied_title))
               .setMessage(application.resources.getString(R.string.location_access_observation_message))
               .setPositiveButton(R.string.settings) { _: DialogInterface?, _: Int ->
                  val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                  intent.data = Uri.fromParts("package", application.packageName, null)
                  startActivity(intent)
               }
               .setNegativeButton(android.R.string.cancel, null)
               .show()
         }
      }
   }

   private fun onCenterCoordinateClick() {
      val clipboard = context?.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager?
      clipboard?.let {
         val center = map?.cameraPosition?.target ?: LatLng(0.0, 0.0)
         val coordinate = CoordinateFormatter(requireContext()).format(center)
         val clip = ClipData.newPlainText("Coordinate", coordinate)
         clipboard.setPrimaryClip(clip)
         Snackbar.make(binding.coordinatorLayout, R.string.location_text_copy_message, Snackbar.LENGTH_SHORT).show()
      }
   }

   private fun toggleCoordinateTarget() {
      val visible = preferences.getBoolean(getString(R.string.showMapCenterCoordinateKey), resources.getBoolean(R.bool.showMapCenterCoordinateDefaultValue))
      binding.centerCoordinateContainer.visibility = if (visible) View.VISIBLE else View.GONE
      binding.centerCoordinateIcon.visibility = if (visible) View.VISIBLE else View.GONE
   }

   private fun showSearchBottomSheet() {
      searchBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
      featureBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
   }

   private fun showObservationBottomSheet(annotation: MapAnnotation<Long>) {
      viewModel.selectObservation(annotation.id)
      searchBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
      featureBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
   }

   private fun showUserBottomSheet(annotation: MapAnnotation<Long>) {
      viewModel.selectUser(annotation.id)
      searchBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
      featureBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
   }

   private fun showFeedItemBottomSheet(annotation: MapAnnotation<String>) {
      viewModel.selectFeedItem(FeedItemId(annotation.layer, annotation.id))
      searchBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
      featureBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
   }

   private fun showStaticFeatureBottomSheet(annotation: MapAnnotation<Long>) {
      viewModel.selectStaticFeature(StaticFeatureId(annotation.layer.toLong(), annotation.id))
      searchBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
      featureBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
   }

   private fun showGeopackageBottomSheet(featureMap: GeoPackageFeatureMapState) {
      viewModel.selectGeoPackageFeature(featureMap)
      searchBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
      featureBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
   }

   private fun onMarkerClick(marker: Marker) {
      if (selectedMarker?.id == marker.id) {
         return
      }

      hideKeyboard()
      deselectMarker()

      if (searchMarker?.id == marker.id) {
         searchMarker?.showInfoWindow()
         return
      }

      observations?.mapAnnotation(marker, "observation")?.let { annotation ->
         selectedMarker = marker
         showObservationBottomSheet(annotation)
         return
      }

      locations?.mapAnnotation(marker, "location")?.let { annotation ->
         selectedMarker = marker
         showUserBottomSheet(annotation)
         return
      }

      staticFeatureCollection?.onMarkerClick(marker)?.let { annotation ->
         selectedMarker = marker
         showStaticFeatureBottomSheet(annotation)
         return
      }

      feeds?.onMarkerClick(marker)?.let { annotation ->
         selectedMarker = marker
         showFeedItemBottomSheet(annotation)
         return
      }

      featureBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
   }

   private fun onMapClick(latLng: LatLng) {
      hideKeyboard()
      deselectMarker()

      observations?.onMapClick(latLng)?.let {
         showObservationBottomSheet(it)
         return
      }

      staticFeatureCollection?.onMapClick(latLng)?.let {
         showStaticFeatureBottomSheet(it)
         return
      }

      val features = cacheOverlays.values.flatMap { overlay ->
         overlay?.getFeaturesNearClick(latLng, binding.mapView, map, application) ?: emptyList()
      }

      features.firstOrNull()?.let { feature ->
         feature.geometry?.let { map?.center(it) }
         showGeopackageBottomSheet(feature)

         return
      }

      // TODO unless search is visible
      featureBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
   }

   private fun deselectMarker() {
      selectedMarker = null

      locations?.offMarkerClick()
      observations?.offMarkerClick()
      feeds?.offMarkerClick()
      staticFeatureCollection?.offMarkerClick()
   }

   private fun onMapLongClick(point: LatLng) {
      hideKeyboard()
      if (!userLocalDataSource.isCurrentUserPartOfCurrentEvent()) {
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
      setCenterCoordinateText()
   }

   private fun onCameraMoveStarted(reason: Int) {
      if (reason == OnCameraMoveStartedListener.REASON_GESTURE) {
         locateState = LocateState.OFF
         binding.zoomButton.isSelected = false
      }
   }

   private fun setCenterCoordinateText() {
      val center = map?.cameraPosition?.target ?: LatLng(0.0, 0.0)
      val coordinate = CoordinateFormatter(requireContext()).format(center)
      binding.centerCoordinateText.text = coordinate
   }

   private fun onStaticLayers(layers: Map<Long, List<MapAnnotation<Long>>>) {
      staticFeatureCollection?.clear()
      layers.forEach { (id, annotations) ->
         staticFeatureCollection?.add(id.toString(), annotations)
      }
   }

   override fun onCacheOverlay(cacheOverlays: List<CacheOverlay>) {
      // Add all overlays that are in the preferences
      val currentEvent = eventLocalDataSource.currentEvent
      val layers = layerLocalDataSource.readByEvent(currentEvent, "GeoPackage")

      val overlays = CacheOverlayFilter(application, layers).filter(cacheOverlays)

      // Track enabled cache overlays
      val enabledCacheOverlays: MutableMap<String, CacheOverlay?> = HashMap()

      // Track enabled GeoPackages
      val enabledGeoPackages: MutableSet<String> = HashSet()

      // Reset the bounding box for newly added caches
      cacheBoundingBox = null
      for (cacheOverlay in overlays) {
         // If this cache overlay potentially replaced by a new version
         if (cacheOverlay.isAdded && cacheOverlay.type == CacheOverlayType.GEOPACKAGE) {
            geoPackageCache.close(cacheOverlay.name)
         }

         // The user has asked for this overlay
         if (cacheOverlay.isEnabled) {
            when (cacheOverlay) {
               is URLCacheOverlay -> addURLCacheOverlay(enabledCacheOverlays, cacheOverlay)
               is GeoPackageCacheOverlay -> addGeoPackageCacheOverlay(enabledCacheOverlays, enabledGeoPackages, cacheOverlay)
               is XYZDirectoryCacheOverlay -> addXYZDirectoryCacheOverlay(enabledCacheOverlays, cacheOverlay)
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

            featureTiles.maxFeaturesPerTile = if (featureDao.geometryType == GeometryType.POINT) {
               resources.getInteger(R.integer.geopackage_feature_tiles_max_points_per_tile)
            } else {
               resources.getInteger(R.integer.geopackage_feature_tiles_max_features_per_tile)
            }

            // Adjust the max features number tile draw paint attributes here as needed to
            // change how tiles are drawn when more than the max features exist in a tile
            featureTiles.maxFeaturesTileDraw = NumberFeaturesTile(activity)

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
      private const val MARKER_REFRESH_INTERVAL: Int = 300 * 1000
   }
}