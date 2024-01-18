package mil.nga.giat.mage.ui.coordinate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import mil.nga.giat.mage.coordinate.CoordinateFormatter

@Composable
fun CoordinateTextButton(
   latLng: LatLng,
   icon: @Composable (() -> Unit)? = null,
   onCopiedToClipboard: (String) -> Unit
) {
   val context = LocalContext.current
   val clipboardManager = LocalClipboardManager.current

   val formatter = CoordinateFormatter(context)
   val text =  formatter.format(latLng)

   TextButton(
      colors = ButtonDefaults.textButtonColors(
         contentColor = MaterialTheme.colorScheme.tertiary
      ),
      onClick = {
         clipboardManager.setText(AnnotatedString(text))
         onCopiedToClipboard(text)
      }
   ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
         icon?.let {
            Box(Modifier.padding(end = 4.dp)) {
               it()
            }
         }
         Text(text = text)
      }
   }
}