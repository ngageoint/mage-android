package mil.nga.giat.mage.ui.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Location
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLocation
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.repository.map.MapLocation
import mil.nga.giat.mage.glide.transform.LocationAgeTransformation
import mil.nga.giat.mage.map.annotation.MapAnnotation
import mil.nga.giat.mage.ui.map.location.LocationPermission
import mil.nga.giat.mage.ui.map.location.LocationPermissionDialog
import mil.nga.giat.mage.ui.map.location.NonMemberDialog
import mil.nga.giat.mage.ui.map.location.ReportLocationButton
import mil.nga.giat.mage.ui.map.location.ZoomToLocationButton
import mil.nga.giat.mage.ui.map.search.SearchButton
import mil.nga.giat.mage.ui.map.sheet.LocationBottomSheet
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
   val mapOrigin by viewModel.mapLocation.observeAsState()
   val cameraPositionState = rememberCameraPositionState()
   var destination by remember { mutableStateOf<MapPosition?>(MapPosition(location = position)) }
   var located by remember { mutableStateOf(false) }
   val location by viewModel.locationPolicy.bestLocationProvider.observeAsState()
   val locations by viewModel.locations.observeAsState(emptyList())
   val locationState by viewModel.locationStatus.observeAsState()
   val availableLayerDownloads by viewModel.availableLayerDownloads.observeAsState(false)
   val userLocation by viewModel.location.observeAsState()

   var origin by remember { mutableStateOf(mapOrigin) }
   if (origin == null) {
      origin = mapOrigin
   }

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
            origin = origin,
            locations = locations,
            locationSource = locationSource,
            locationEnabled = locationPermissionState.status.isGranted,
            destination = destination,
            cameraPositionState = cameraPositionState,
            onMapMove = { cameraPosition, cameraMoveReason ->
               if (cameraMoveReason == com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                  located = false
                  destination = null
               }

               scope.launch {
                  viewModel.setMapLocation(cameraPosition)
               }
            },
            onLocationTap = { annotation ->
               viewModel.selectUser(annotation.id)
               val centroid = annotation.geometry.centroid
               destination = MapPosition(location = LatLng(centroid.y, centroid.x))
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

   LocationBottomSheet(
      state = userLocation,
      onDismiss = { viewModel.selectUser(null) },
      modifier = Modifier.padding(bottom = 32.dp)
   )
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
private fun Map(
   baseMap: MapType?,
   origin: MapLocation?,
   locations: List<MapAnnotation<Long>>,
   locationEnabled: Boolean,
   locationSource: LocationSource,
   destination: MapPosition?,
   cameraPositionState: CameraPositionState,
   onMapMove: (CameraPosition, Int) -> Unit,
   onLocationTap: (MapAnnotation<Long>) -> Unit
) {
   val scope = rememberCoroutineScope()
   val context = LocalContext.current

   val locationIcons = remember { mutableMapOf<Long, Bitmap>() }
   val locationStates = remember { mutableMapOf<Long, MarkerState>() }

   var isMapLoaded by remember { mutableStateOf(false) }
   var cameraMoveReason by remember { mutableIntStateOf(0) }

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

   GoogleMap(
      cameraPositionState = cameraPositionState,
      onMapLoaded = { isMapLoaded = true },
      properties = properties,
      uiSettings = uiSettings,
      locationSource = locationSource
   ) {
      MapEffect(origin) {
         origin?.let { origin ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(origin.latitude, origin.longitude), origin.zoom.toFloat())
         }
      }

      MapEffect(destination) { map ->
         map.setOnCameraMoveStartedListener { reason ->
            cameraMoveReason = reason
         }

         map.setOnCameraMoveListener {
            onMapMove(map.cameraPosition, cameraMoveReason)
         }

         destination?.location?.let { location ->
            scope.launch {
               val latLng = LatLng(location.latitude, location.longitude)
               val update = CameraUpdateFactory.newLatLngZoom(latLng, 16f)
               cameraPositionState.animate(update)
            }
         }
      }

      MapEffect(locations) {
         locations.forEach { location ->
            val position = location.geometry.centroid
            val state = locationStates[location.id]
            if (state == null) {
               locationStates[location.id] = MarkerState(
                  position = LatLng(position.y, position.x)
               )
            } else {
               state.position = LatLng(position.y, position.x)
            }
         }
      }

      locationStates.forEach { (id, state) ->
//         val icon = location.icon?.let { BitmapDescriptorFactory.fromBitmap(it) }
         Marker(
            state = state,
//            icon = icon,
            onClick = {
//               onLocationTap(location)
               true
            }
         )
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

private suspend fun locationIcon(
   context: Context,
   annotation: MapAnnotation<*>
) = suspendCoroutine { continuation ->
   val transformation = LocationAgeTransformation(context, annotation.timestamp)

   Glide.with(context)
      .asBitmap()
      .load(annotation)
      .error(R.drawable.default_marker)
      .transform(transformation)
      .into(object : CustomTarget<Bitmap>(40, 40) {
         override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
            continuation.resume(resource)
         }

         override fun onLoadCleared(placeholder: Drawable?) {}
      })
}