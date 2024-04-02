package mil.nga.giat.mage.ui.map.location

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun ZoomToLocationButton(
   enabled: Boolean,
   onTap: () -> Unit,
) {
   val contentColor = if (enabled) {
      MaterialTheme.colorScheme.onPrimary
   } else {
      MaterialTheme.colorScheme.primary
   }

   val containerColor = if (enabled) {
      MaterialTheme.colorScheme.primary
   } else {
      MaterialTheme.colorScheme.surface
   }

   SmallFloatingActionButton(
      contentColor = contentColor,
      containerColor = containerColor,
      elevation = FloatingActionButtonDefaults.elevation( defaultElevation = 0.dp),
      onClick = { onTap() }
   ) {
      Icon(
         Icons.Outlined.NearMe,
         contentDescription = "Report Location"
      )
   }
}