package mil.nga.giat.mage.map.detail

import android.widget.TextView
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import mil.nga.giat.mage.map.StaticFeatureMapState
import mil.nga.giat.mage.ui.theme.MageTheme
import mil.nga.sf.Geometry

sealed class StaticFeatureAction {
   class Directions(val geometry: Geometry, val icon: Any?): StaticFeatureAction()
   class Location(val geometry: Geometry): StaticFeatureAction()
}


@Composable
fun MapStaticFeatureDetails(
   featureMapState: StaticFeatureMapState?,
   onAction: ((Any) -> Unit)? = null
) {
   if (featureMapState != null) {
      MageTheme {
         Surface {
            FeatureDetails(
               featureMapState,
               onAction = { action ->
                  when (action) {
                     is FeatureAction.Directions -> {
                        onAction?.invoke(StaticFeatureAction.Directions(action.geometry, action.image))

                     }
                     is FeatureAction.Location -> {
                        onAction?.invoke(StaticFeatureAction.Location(action.geometry))
                     }
                  }
               },
               details = {
                  StaticFeatureDetails(content = featureMapState.content)
               }
            )
         }
      }
   }
}

@Composable
private fun StaticFeatureDetails(content: String?) {
   if (content != null) {
      Divider(
         color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
         modifier = Modifier.height(8.dp)
      )

      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
         Text(
            text = "DESCRIPTION",
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
         )
      }

      AndroidView(
         modifier = Modifier.padding(horizontal = 16.dp),
         factory = { context -> TextView(context) },
         update = { it.text = HtmlCompat.fromHtml("<div>$content</div>", HtmlCompat.FROM_HTML_MODE_COMPACT) }
      )
   }
}