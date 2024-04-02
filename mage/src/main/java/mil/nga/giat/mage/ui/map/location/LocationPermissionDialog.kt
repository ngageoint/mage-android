package mil.nga.giat.mage.ui.map.location

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationDisabled
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import mil.nga.giat.mage.R

@Composable
fun LocationPermissionDialog(
   visible: Boolean,
   onDismiss: () -> Unit,
   onSettings:() -> Unit
) {
   if (visible) {
      AlertDialog(
         icon = {
            Icon(
               imageVector = Icons.Default.LocationDisabled,
               tint = MaterialTheme.colorScheme.primary,
               contentDescription = "Location Disabled"
            )
         },
         title = {
            val title = LocalContext.current.resources.getString(R.string.location_access_title)
            Text(text = title)
         },
         text = {
            val text = LocalContext.current.resources.getString(R.string.location_access_message)
            Text(text = text)
         },
         onDismissRequest = { onDismiss() },
         dismissButton = {
            TextButton(onClick = { onDismiss() }) {
               Text(text = "Cancel")
            }
         },
         confirmButton = {
            TextButton(onClick = { onSettings() }) {
               Text("Settings")
            }
         }
      )
   }
}