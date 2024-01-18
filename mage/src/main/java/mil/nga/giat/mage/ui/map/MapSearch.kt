package mil.nga.giat.mage.ui.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import mil.nga.giat.mage.R
import mil.nga.giat.mage.search.GeocoderResult
import mil.nga.giat.mage.ui.search.PlacenameSearch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapSearch(
   onApply: (LatLng) -> Unit,
   onDismiss: () -> Unit,
   viewModel: MapSearchViewModel = hiltViewModel()
) {
   val density = LocalDensity.current
   var bottomSheetHeight by remember { mutableStateOf(0.dp) }

   val scaffoldState = rememberBottomSheetScaffoldState(
      bottomSheetState = rememberStandardBottomSheetState(
         initialValue = SheetValue.Expanded
      )
   )

   val baseMap by viewModel.baseMap.observeAsState()
   val searchResult by viewModel.searchResult.observeAsState()

   Column(Modifier.fillMaxSize()) {
      TopAppBar(
         title = {
            Text(
               text = "Search",
               maxLines = 1,
               overflow = TextOverflow.Ellipsis,
               style = MaterialTheme.typography.titleMedium
            )
         },
         navigationIcon = {
            IconButton(onClick = { onDismiss() }) {
               Icon(Icons.Default.Close, contentDescription = "dismiss")
            }
         },
         actions = {
            TextButton(
               onClick = { searchResult?.let{ onApply(it.location) } },
               enabled = searchResult != null,
               colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
               Text("Apply")
            }
         },
         colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            actionIconContentColor = Color.White,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
         )
      )

      BottomSheetScaffold(
         scaffoldState = scaffoldState,
         sheetPeekHeight = 128.dp,
         sheetContent = {
            Column(
               Modifier.onGloballyPositioned {
                  bottomSheetHeight = with(density) {
                     it.size.height.toDp()
                  }
               }
            ) {
               PlacenameSearch(
                  onSearchResultTap = { _, result ->
                     viewModel.setSearchResult(result)
                  }
               )
            }
         }) { _ ->
         Map(
            baseMap = baseMap,
            contentPadding = PaddingValues(bottom = bottomSheetHeight),
            searchResult = searchResult
         )
      }
   }
}

@Composable
private fun Map(
   baseMap: MapType?,
   contentPadding: PaddingValues,
   searchResult: GeocoderResult?
) {
   val cameraPositionState = rememberCameraPositionState()

   LaunchedEffect(searchResult) {
      searchResult?.let {
         cameraPositionState.move(
            update = CameraUpdateFactory.newLatLngZoom(it.location, 16f)
         )
      }
   }

   val uiSettings = MapUiSettings(
      zoomControlsEnabled = false,
      compassEnabled = false
   )

   val mapStyleOptions = if (isSystemInDarkTheme()) {
      MapStyleOptions.loadRawResourceStyle(LocalContext.current, R.raw.map_theme_night)
   } else null

   val properties = baseMap?.let { mapType ->
      MapProperties(
         mapType = mapType,
         mapStyleOptions = mapStyleOptions
      )
   } ?: MapProperties()

   GoogleMap(
      cameraPositionState = cameraPositionState,
      properties = properties,
      uiSettings = uiSettings,
      modifier = Modifier.fillMaxSize(),
      contentPadding = contentPadding
   ) {
      searchResult?.let {
         Marker(
            state = MarkerState(position = it.location),
            title = it.name,
            snippet = it.address,
            icon = BitmapDescriptorFactory.defaultMarker(211f)
         )
      }
   }
}