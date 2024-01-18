package mil.nga.giat.mage.ui.sheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DragHandle() {
   Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
         .fillMaxWidth()
         .padding(vertical = 16.dp)
   ) {
      Surface(
         shape = MaterialTheme.shapes.extraLarge,
         color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
      ) {
         Box(
            modifier = Modifier
               .height(4.dp)
               .width(32.dp)
         )
      }
   }
}

