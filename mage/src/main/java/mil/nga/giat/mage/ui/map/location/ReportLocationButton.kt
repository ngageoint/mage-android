package mil.nga.giat.mage.ui.map.location

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationDisabled
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import mil.nga.giat.mage.ui.map.LocationState

@Composable
fun ReportLocationButton(
   locationState: LocationState?,
   onTap: () -> Unit,
) {
   SmallFloatingActionButton(
      containerColor = MaterialTheme.colorScheme.surface,
      elevation = FloatingActionButtonDefaults.elevation( defaultElevation = 0.dp),
      onClick = { onTap() }
   ) {
      ReportLocationIcon(locationState)
   }
}

@Composable
private fun ReportLocationIcon(
   locationState: LocationState?,
) {
   val icon = when (locationState) {
      LocationState.ReportingCoarse, LocationState.ReportingPrecise -> {  Icons.Outlined.MyLocation }
      else -> { Icons.Outlined.LocationDisabled}
   }

   // TODO define these colors in viewmodel or theme
   val tint = when (locationState) {
      LocationState.NotEventMember -> Color(0xFF9E9E9E)
      LocationState.ReportingPrecise -> Color(0xFF4CAF50)
      LocationState.ReportingCoarse -> Color(0xFFFFA000)
      else -> Color.Red
   }

   Icon(icon,
      tint = tint,
      contentDescription = "Report Location"
   )
}
