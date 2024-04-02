package mil.nga.giat.mage.ui.map.location

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GroupOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import mil.nga.giat.mage.R

@Composable
fun NonMemberDialog(
   visible: Boolean,
   onDismiss: () -> Unit
) {
   if (visible) {
      AlertDialog(
         icon = {
            Icon(
               imageVector = Icons.Outlined.GroupOff,
               tint = MaterialTheme.colorScheme.primary,
               contentDescription = "Not Event Member"
            )
         },
         title = {
            val title = LocalContext.current.resources.getString(R.string.no_event_title)
            Text(text = title)
         },
         text = {
            val text = LocalContext.current.resources.getString(R.string.location_no_event_message)
            Text(text = text)
         },
         onDismissRequest = { onDismiss() },
         confirmButton = {
            TextButton(onClick = { onDismiss() }) {
               Text("OK")
            }
         }
      )
   }
}