package mil.nga.giat.mage.map.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.glide.rememberGlidePainter
import com.google.android.gms.maps.model.LatLng
import mil.nga.giat.mage.coordinate.CoordinateFormatter
import mil.nga.giat.mage.map.FeatureMapState
import mil.nga.giat.mage.ui.theme.linkColor
import mil.nga.sf.Geometry

sealed class FeatureAction<T: Any> {
   class Directions<T: Any>(val id: T, val geometry: Geometry, val image: Any?): FeatureAction<Any>()
   class Location(val geometry: Geometry): FeatureAction<Any>()
   class Details<T: Any>(val id: T): FeatureAction<T>()
}

val LocalHeaderColor = compositionLocalOf { Color.Unspecified }

@Composable
fun <I: Any> FeatureDetails(
   featureMapState: FeatureMapState<I>?,
   header: (@Composable () -> Unit)? = null,
   headerColor: Color? = null,
   actions: (@Composable () -> Unit)? = null,
   onAction: ((Any) -> Unit)? = null,
   details: (@Composable () -> Unit)? = null
) {
   FeatureContent(
      featureMapState = featureMapState,
      header = header,
      headerColor = headerColor ?: MaterialTheme.colorScheme.surface,
      actions = actions,
      onAction = onAction,
      details = details
   )
}

@Composable
private fun <I: Any> FeatureContent(
   featureMapState: FeatureMapState<I>?,
   header: (@Composable () -> Unit)? = null,
   headerColor: Color,
   actions: (@Composable () -> Unit)? = null,
   onAction: ((Any) -> Unit)? = null,
   details: (@Composable () -> Unit)? = null
) {
   if (featureMapState != null) {
      Column {
         CompositionLocalProvider(LocalHeaderColor provides headerColor) {
            header?.invoke()
         }

         FeatureHeaderContent(featureMapState, actions, onAction)

         if (details == null) {
            Button(
               modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 8.dp, horizontal = 16.dp),
               onClick = {
                  onAction?.invoke(FeatureAction.Details(featureMapState.id))
               }
            ) {
               Text(text = "MORE DETAILS")
            }
         } else {
            details()
         }
      }
   }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun <I: Any> FeatureHeaderContent(
   featureMapState: FeatureMapState<I>,
   actions: (@Composable () -> Unit)? = null,
   onAction: ((Any) -> Unit)?
) {
   Row(
      Modifier
         .padding(top = 8.dp)
         .fillMaxWidth()
   ) {
      Column {
         Row(Modifier.padding(start = 16.dp)) {
            Column(
               Modifier
                  .weight(1f)
                  .padding(end = 16.dp)
            ) {
               if (featureMapState.title != null) {
                  Column(Modifier.padding(bottom = 16.dp)) {
                     CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                        Text(
                           text = featureMapState.title.uppercase(),
                           fontWeight = FontWeight.SemiBold,
                           style = MaterialTheme.typography.labelSmall,
                           maxLines = 1,
                           overflow = TextOverflow.Ellipsis
                        )
                     }
                  }
               }

               if (featureMapState.primary?.isNotEmpty() == true) {
                  Row {
                     Text(
                        text = featureMapState.primary,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                     )
                  }
               }

               if (featureMapState.secondary?.isNotEmpty() == true) {
                  Row {
                     CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                        Text(
                           text = featureMapState.secondary,
                           style = MaterialTheme.typography.titleMedium,
                           maxLines = 1,
                           overflow = TextOverflow.Ellipsis
                        )
                     }
                  }
               }
            }

            featureMapState.image?.let { image ->
               FeatureIcon(image)
            }
         }

         FeatureActions(featureMapState, actions, onAction)
      }
   }
}

@Composable
private fun <I: Any> FeatureActions(
   featureMapState: FeatureMapState<I>,
   actions: (@Composable () -> Unit)? = null,
   onAction: ((Any) -> Unit)? = null
) {
   Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
         .fillMaxWidth()
         .padding(vertical = 8.dp)
   ) {
      featureMapState.geometry?.let { geometry ->
         Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
               .clickable { onAction?.invoke(FeatureAction.Location(geometry)) }
               .padding(8.dp)
         ) {

            Icon(
               imageVector = Icons.Default.GpsFixed,
               contentDescription = "Location",
               tint = MaterialTheme.colorScheme.linkColor,
               modifier = Modifier
                  .height(24.dp)
                  .width(24.dp)
                  .padding(end = 4.dp)
            )

            val centroid = geometry.centroid
            val locationText = CoordinateFormatter(LocalContext.current).format(LatLng(centroid.y, centroid.x))
            Text(
               text = locationText,
               color = MaterialTheme.colorScheme.linkColor,
               style = MaterialTheme.typography.bodyMedium,
               modifier = Modifier.padding(end = 8.dp)
            )
         }
      }

      Row {
         actions?.invoke()

         featureMapState.geometry?.let { geometry ->
            IconButton(
               modifier = Modifier.padding(end = 8.dp),
               onClick = {
                  onAction?.invoke(FeatureAction.Directions(featureMapState.id, geometry, featureMapState.image))
               }
            ) {
               CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                  Icon(
                     imageVector = Icons.Outlined.Directions,
                     contentDescription = "Directions"
                  )
               }
            }
         }
      }
   }
}

@Composable
private fun FeatureIcon(image: Any) {
   Box(modifier = Modifier
      .padding(end = 8.dp)
      .width(64.dp)
      .height(64.dp)
   ) {
      Image(
         painter = rememberGlidePainter(
            image
         ),
         contentDescription = "Observation Map Icon",
         Modifier.fillMaxSize()
      )
   }
}