package mil.nga.giat.mage.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLocation
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.VisibleRegion
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.repository.map.MapLocation
import mil.nga.giat.mage.ui.map.location.LocationPermission
import mil.nga.giat.mage.ui.map.location.LocationPermissionDialog
import mil.nga.giat.mage.ui.map.location.LocationsMap
import mil.nga.giat.mage.ui.map.location.NonMemberDialog
import mil.nga.giat.mage.ui.map.location.ReportLocationButton
import mil.nga.giat.mage.ui.map.location.ZoomToLocationButton
import mil.nga.giat.mage.ui.map.observation.ObservationsMap
import mil.nga.giat.mage.ui.map.search.SearchButton
import mil.nga.giat.mage.ui.map.sheet.AllBottomSheet
import kotlin.math.abs


data class MapPosition(
   val location: LatLng? = null,
   val bounds: LatLngBounds? = null,
   val name: String? = null
)

fun launchSettingsApplication(context: Context) {
   val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
   intent.data = Uri.fromParts("package", context.packageName, null)
   context.startActivity(intent)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
   position : LatLng? = null,
   onSettings: () -> Unit,
   onMapTap: () -> Unit,
   onAddObservation: (Location?) -> Unit,
   viewModel: MapViewModel = hiltViewModel()
) {
   val scope = rememberCoroutineScope()
   val context = LocalContext.current
   val snackbarHostState = remember { SnackbarHostState() }

   var showNonMemberDialog by remember { mutableStateOf(false) }
   var showLocationPermissionDialog by remember { mutableStateOf(false) }

   var searchExpanded by remember { mutableStateOf(false) }
   val searchResponse by viewModel.searchResponse.observeAsState()

   val baseMap by viewModel.baseMapType.observeAsState()
   val mapOrigin by viewModel.mapLocation.collectAsState(null)
   val cameraPositionState = rememberCameraPositionState()
   var destination by remember { mutableStateOf<MapPosition?>(MapPosition(location = position)) }
   var located by remember { mutableStateOf(false) }
   val location by viewModel.locationPolicy.bestLocationProvider.observeAsState()
   val locationState by viewModel.locationStatus.observeAsState()
   val availableLayerDownloads by viewModel.availableLayerDownloads.observeAsState(false)

   val locationSource = object : LocationSource {
      override fun activate(listener: LocationSource.OnLocationChangedListener) {
         location?.let { listener.onLocationChanged(it) }
      }

      override fun deactivate() {}
   }

   val locationPermissionState: PermissionState = rememberPermissionState(
      Manifest.permission.ACCESS_FINE_LOCATION
   )

   LocationPermission(locationPermissionState)

   Scaffold(
      snackbarHost = {
         SnackbarHost(hostState = snackbarHostState, snackbar = { snackbarData ->
            Snackbar(
               snackbarData = snackbarData,
               actionColor = Color(0xFFFFA000)
            )
         })
      },
      floatingActionButton = {
         AddObservation { onAddObservation(location) }
      }
   ) { paddingValues ->
      Box(Modifier.padding(paddingValues)) {
         Map(
            baseMap = baseMap,
            origin = mapOrigin,
            locationSource = locationSource,
            locationEnabled = locationPermissionState.status.isGranted,
            destination = destination,
            cameraPositionState = cameraPositionState,
            onMapMove = { cameraPosition, cameraMoveReason, visibleRegion ->
               if (cameraMoveReason == com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                  located = false
                  destination = null
               }

               scope.launch {
                  viewModel.setMapLocation(cameraPosition, visibleRegion)
               }
            },
            onMapTap = { latLng, region, mapWidth, mapHeight, zoom ->
               val screenPercentage = 0.04
               val screenRightLong = region.farRight.longitude
               val screenLeftLong = region.farLeft.longitude

               val tolerance = if (screenRightLong > screenLeftLong) {
                  (screenRightLong - screenLeftLong) * screenPercentage
               } else {
                  // 180th meridian is on screen
                  (360 - abs(screenRightLong) - abs(screenLeftLong)) * screenPercentage
               }

               val densityMapWidth = mapWidth / context.resources.displayMetrics.density
               val densityMapHeight = mapHeight / context.resources.displayMetrics.density

               val bounds = LatLngBounds(
                  LatLng(latLng.latitude - tolerance, latLng.longitude - tolerance),
                  LatLng(latLng.latitude + tolerance, latLng.longitude + tolerance)
               )

               val latitudePerPixel = ((region.farRight.latitude - region.farLeft.latitude) / densityMapHeight).toFloat()
               val longitudePerPixel = ((region.farRight.longitude - region.farLeft.longitude) / densityMapWidth).toFloat()

               scope.launch {
                  val count = viewModel.setTapLocation(latLng, bounds, longitudePerPixel, latitudePerPixel, zoom)
                  if (count > 0) {
                     onMapTap()
                  }
               }
            }
         )

         Settings(
            availableLayerDownloads = availableLayerDownloads,
            onTap = { onSettings() },
            modifier = Modifier
               .align(Alignment.TopEnd)
               .padding(16.dp)
         )
         
         Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
               .align(Alignment.TopStart)
               .padding(16.dp)
         ) {
            SearchButton(
               expanded = searchExpanded,
               response = searchResponse,
               onExpand = {
                  searchExpanded = !searchExpanded
               },
               onTextChanged = {
                  viewModel.search(it)
               },
               onLocationTap = {
                  destination = MapPosition(
                     name = "Map",
                     location = LatLng(it.latitude, it.longitude)
                  )
               },
               onLocationCopy = { /* TODO */ }
            )

            ZoomToLocationButton(
               enabled = located,
               onTap = {
                  located = true
                  scope.launch {
                     location?.let {
                        destination = MapPosition(
                           location = LatLng(it.latitude, it.longitude)
                        )
                     }
                  }
               }
            )
            
            ReportLocationButton(
               locationState = locationState,
               onTap = {
                  scope.launch {
                     when (val response = viewModel.toggleReportLocation()) {
                        is LocationToggleResult.Start -> {
                           if (response.precision == LocationPrecision.Precise) {
                              snackbarHostState.showSnackbar(context.resources.getString(R.string.report_location_start))
                           } else {
                              val message = context.resources.getString(R.string.report_location_start_coarse)
                              val result = snackbarHostState.showSnackbar(message, "Settings", duration = SnackbarDuration.Short)
                              when (result) {
                                 SnackbarResult.ActionPerformed -> launchSettingsApplication(context)
                                 else -> {}
                              }
                           }
                        }
                        is LocationToggleResult.Stop -> {
                           snackbarHostState.showSnackbar(context.resources.getString(R.string.report_location_stop))
                        }
                        is LocationToggleResult.NotEventMember -> {
                           showNonMemberDialog = true
                        }
                        is LocationToggleResult.PermissionDenied -> {
                           showLocationPermissionDialog = true
                        }
                     }
                  }
               }
            )
         }
      }
   }

   NonMemberDialog(
      visible = showNonMemberDialog,
      onDismiss = { showNonMemberDialog = false }
   )

   LocationPermissionDialog(
      visible = showLocationPermissionDialog,
      onDismiss = { showLocationPermissionDialog = false },
      onSettings = { launchSettingsApplication(context) }
   )

   AllBottomSheet(
      onDismiss = { viewModel.clearBottomSheetItems() },
      modifier = Modifier.padding(bottom = 32.dp)
   )

}

@SuppressLint("PotentialBehaviorOverride")
@OptIn(MapsComposeExperimentalApi::class)
@Composable
private fun Map(
   baseMap: MapType?,
   origin: MapLocation?,
   locationEnabled: Boolean,
   locationSource: LocationSource,
   destination: MapPosition?,
   cameraPositionState: CameraPositionState,
   onMapMove: (CameraPosition, Int, VisibleRegion) -> Unit,
   onMapTap: (LatLng, VisibleRegion, Float, Float, Float) -> Unit
) {
   val scope = rememberCoroutineScope()
   val context = LocalContext.current

   var isMapLoaded by remember { mutableStateOf(false) }
   var cameraMoveReason by remember { mutableIntStateOf(0) }
   // this will cause the map to query the first time
   var didUserStartMove by remember { mutableStateOf(true) }

   val mapStyleOptions = if (isSystemInDarkTheme()) {
      MapStyleOptions.loadRawResourceStyle(context, R.raw.map_theme_night)
   } else null

   val properties = baseMap?.let { mapType ->
      MapProperties(
         minZoomPreference = 0f,
         mapType = mapType,
         isMyLocationEnabled = locationEnabled,
         mapStyleOptions = mapStyleOptions
      )
   } ?: MapProperties()

   val uiSettings =  MapUiSettings(
      mapToolbarEnabled = false,
      compassEnabled = false,
      zoomControlsEnabled = false,
      myLocationButtonEnabled = false
   )

   // Get local density from composable
   val localDensity = LocalDensity.current

   // Create element height in pixel state
   var mapHeightPx by remember {
      mutableFloatStateOf(0f)
   }

   // Create element height in dp state
   var mapHeightDp by remember {
      mutableStateOf(0.dp)
   }

   // Create element width in pixel state
   var mapWidthPx by remember {
      mutableFloatStateOf(0f)
   }

   // Create element height in dp state
   var mapWidthDp by remember {
      mutableStateOf(0.dp)
   }

   GoogleMap(
      cameraPositionState = cameraPositionState,
      onMapLoaded = { isMapLoaded = true },
      properties = properties,
      uiSettings = uiSettings,
      locationSource = locationSource,
      modifier = Modifier
         .onGloballyPositioned { coordinates ->
            // Set column height and width using the LayoutCoordinates
            mapHeightPx = coordinates.size.height.toFloat()
            mapHeightDp = with(localDensity) { coordinates.size.height.toDp() }
            mapWidthPx = coordinates.size.width.toFloat()
            mapWidthDp = with(localDensity) { coordinates.size.width.toDp() }
         }
   ) {
      ObservationsMap(
         isMapLoaded = isMapLoaded,
         cameraPositionState = cameraPositionState,
         onMapTap = { latLng, visibleRegion ->
            onMapTap(latLng, visibleRegion, mapWidthPx, mapHeightPx, cameraPositionState.position.zoom)
         }
      )

      LocationsMap(
         cameraPositionState = cameraPositionState,
         onMapTap = { latLng, visibleRegion ->
            onMapTap(latLng, visibleRegion, mapWidthPx, mapHeightPx, cameraPositionState.position.zoom)
         }
      )

      MapEffect(origin) { map ->
         origin?.let { origin ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(origin.latitude, origin.longitude), origin.zoom.toFloat())
         }
      }

      MapEffect(destination) { map ->

         map.setOnCameraMoveStartedListener { reason ->
            Log.d("MapScreen", "on camera move start $reason")
            cameraMoveReason = reason
            didUserStartMove = reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE
         }

         map.setOnCameraMoveListener {
            Log.d("MapScreen", "on camera move")
//            onMapMove(map.cameraPosition, cameraMoveReason, map.projection.visibleRegion)
         }

         map.setOnCameraIdleListener {
            Log.d("MapScreen", "on camera idle")
            if (didUserStartMove) {
               onMapMove(map.cameraPosition, cameraMoveReason, map.projection.visibleRegion)
               didUserStartMove = false
            }
         }

         destination?.location?.let { location ->
            scope.launch {
               val latLng = LatLng(location.latitude, location.longitude)
               val update = CameraUpdateFactory.newLatLngZoom(latLng, 16f)
               cameraPositionState.animate(update)
            }
         }

         val mapClickListener = GoogleMap.OnMapClickListener { latLng ->
            Log.d("MapScreen", "onMapClick: $latLng")
            onMapTap(
               latLng,
               map.projection.visibleRegion,
               mapWidthPx,
               mapHeightPx,
               map.cameraPosition.zoom
            )
         }

         map.setOnMapClickListener(mapClickListener)
      }
   }
}

@Composable
private fun Settings(
   availableLayerDownloads: Boolean,
   onTap: () -> Unit,
   modifier: Modifier = Modifier
) {
   Box(modifier) {
      SmallFloatingActionButton(
         onClick = { onTap() },
         elevation = FloatingActionButtonDefaults.elevation( defaultElevation = 0.dp)
      ) {
         Icon(
            Icons.Outlined.Map,
            tint = MaterialTheme.colorScheme.tertiary,
            contentDescription = "Map Settings"
         )
      }
   }

   if (availableLayerDownloads) {
      Box(modifier.offset(x = 10.dp, y = (-10).dp)) {
         Box(
            Modifier
               .size(24.dp)
               .clip(RoundedCornerShape(12.dp))
               .background(MaterialTheme.colorScheme.tertiary)
         ) {
            Icon(
               Icons.Outlined.Download,
               tint = MaterialTheme.colorScheme.surface,
               modifier = Modifier
                  .size(18.dp)
                  .align(Alignment.Center),
               contentDescription = "Layer Downloads"
            )
         }
      }
   }
}

@Composable
private fun AddObservation(
   onTap: () -> Unit
) {
   FloatingActionButton(
      containerColor = MaterialTheme.colorScheme.secondary,
      contentColor = MaterialTheme.colorScheme.onSecondary,
      onClick = { onTap() }
   ) {
      Icon(
         Icons.Outlined.AddLocation,
         contentDescription = "Add Observation"
      )
   }
}

data class IconMarkerState(
   var markerState: MarkerState,
   var icon: BitmapDescriptor? = null,
   var id: Long
)