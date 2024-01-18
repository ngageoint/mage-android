package mil.nga.giat.mage.ui.coordinate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.google.android.gms.maps.model.LatLng
import mil.nga.giat.mage.coordinate.CoordinateFormatter

@Composable
fun CoordinateText(
   latLng: LatLng,
   icon: @Composable (() -> Unit)? = null,
   onCopiedToClipboard: (String) -> Unit
) {
   val context = LocalContext.current
   val clipboardManager = LocalClipboardManager.current

   val formatter = CoordinateFormatter(context)
   val text =  formatter.format(latLng)

   Row(verticalAlignment = Alignment.CenterVertically) {
      icon?.invoke()

      Text(
         text = text,
         color = MaterialTheme.colorScheme.primary,
         modifier = Modifier.clickable {
            clipboardManager.setText(AnnotatedString.Builder(text).toAnnotatedString())
            onCopiedToClipboard(text)
         }
      )
   }

}