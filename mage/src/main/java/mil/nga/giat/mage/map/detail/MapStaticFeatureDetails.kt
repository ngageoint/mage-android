package mil.nga.giat.mage.map.detail

import android.content.Context
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import mil.nga.giat.mage.map.StaticFeatureMapState
import mil.nga.giat.mage.ui.theme.MageTheme
import mil.nga.sf.Geometry
import mil.nga.giat.mage.R
import mil.nga.giat.mage.utils.ThemeUtils

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
                     is FeatureAction.Directions<*> -> {
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
      val context = LocalContext.current

      val configuration = LocalConfiguration.current
      val backgroundColor = remember(configuration) {
         getBackgroundColor(context)
      }

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

      //recreate WebView on orientation change to prevent scaling issues
      key(LocalConfiguration.current.orientation) {
         AndroidView(
            modifier = Modifier.padding(horizontal = 16.dp),
            factory = { context ->
               android.webkit.WebView(context).apply {
                  settings.javaScriptEnabled = false
                  setBackgroundColor(backgroundColor)
               }
            },
            update = { webView ->
               webView.setBackgroundColor(backgroundColor)

               webView.loadDataWithBaseURL(
                  null,
                  content,
                  "text/html; charset=utf-8",
                  "UTF-8",
                  null
               )
            }
         )
      }
   }
}

private fun getBackgroundColor(context: Context): Int {
   val prefs = PreferenceManager.getDefaultSharedPreferences(context)
   val themeCode = prefs.getInt(context.getString(R.string.dayNightThemeKey), 1)
   val isDarkMode = ThemeUtils.isDarkMode(context, themeCode)

   return if (isDarkMode) {
      ContextCompat.getColor(context, R.color.md_grey_400)
   } else {
      android.graphics.Color.TRANSPARENT
   }
}