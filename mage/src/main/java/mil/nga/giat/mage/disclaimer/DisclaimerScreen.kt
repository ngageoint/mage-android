package mil.nga.giat.mage.disclaimer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mil.nga.giat.mage.R

@Composable
fun DisclaimerScreen(
   title: String,
   text: String,
   onAgree: () -> Unit,
   onDisagree: () -> Unit
) {
   Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
         .fillMaxSize()
         .windowInsetsPadding(WindowInsets.safeDrawing)
         .padding(horizontal = 24.dp, vertical = 16.dp)
   ) {
      CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
         Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
               .fillMaxWidth()
               .padding(bottom = 16.dp)
         )
      }

      Text(
         text = text,
         style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
         ),
         modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 8.dp)
      )

      Row(
         horizontalArrangement = Arrangement.spacedBy(8.dp),
         modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
      ) {
         OutlinedButton(
            onClick = onDisagree,
            modifier = Modifier
               .weight(1f)
               .fillMaxWidth()
         ) {
            Text(stringResource(R.string.disagree))
         }

         Button(
            onClick = onAgree,
            modifier = Modifier
               .weight(1f)
               .fillMaxWidth()
         ) {
            Text(stringResource(R.string.agree))
         }
      }
   }
}
