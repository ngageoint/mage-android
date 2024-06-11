package mil.nga.giat.mage.map.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mil.nga.giat.mage.map.ObservationMapState
import mil.nga.giat.mage.ui.theme.importantBackground
import mil.nga.sf.Geometry

sealed class ObservationAction {
   class Details(val id: Long): ObservationAction()
   class Directions(val id: Long, val geometry: Geometry, val image: Any?): ObservationAction()
   class Favorite(val observation: ObservationMapState): ObservationAction()
   class Location(val geometry: Geometry): ObservationAction()
}

@Composable
fun ObservationMapDetails(
   observationMapState: ObservationMapState?,
   onAction: ((ObservationAction) -> Unit)? = null
) {
   if (observationMapState != null) {
      val headerColor = if (observationMapState.importantState != null) {
         MaterialTheme.colors.importantBackground
      } else {
         Color.Unspecified
      }

      FeatureDetails(
         observationMapState,
         header = {
            ObservationImportantHeader(observationMapState)
         },
         headerColor = headerColor,
         actions = {
            ObservationFavorite(observationMapState) {
               onAction?.invoke(ObservationAction.Favorite(observationMapState))
            }
         },
         onAction = { action ->
            when (action) {
               is FeatureAction.Details<*> -> {
                  onAction?.invoke(ObservationAction.Details(observationMapState.id))
               }
               is FeatureAction.Directions<*> -> {
                  onAction?.invoke(ObservationAction.Directions(observationMapState.id, action.geometry, action.image))
               }
               is FeatureAction.Location -> {
                  onAction?.invoke(ObservationAction.Location(action.geometry))
               }
            }
         }
      )
   }
}

@Composable
private fun ObservationFavorite(
   observationMap: ObservationMapState,
   onFavorite: () -> Unit
) {
   Row {
      val favoriteTint = if (observationMap.favorite) {
         Color(0XFF7ED31F)
      } else {
         MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
      }

      IconButton(
         modifier = Modifier.padding(end = 8.dp),
         onClick = { onFavorite() }
      ) {
         Icon(
            imageVector = if (observationMap.favorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
            tint = favoriteTint,
            contentDescription = "Favorite",
         )
      }
   }
}

@Composable
private fun ObservationImportantHeader(
   observationMap: ObservationMapState
) {
   val important = observationMap.importantState
   if (important != null) {
      Column(
         modifier = Modifier
            .fillMaxWidth()
            .background(LocalHeaderColor.current)
            .padding(horizontal = 16.dp)
      ) {
         Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
         ) {
            Icon(
               imageVector = Icons.Default.Flag,
               contentDescription = "Important Flag",
               modifier = Modifier
                  .height(40.dp)
                  .width(40.dp)
                  .padding(end = 8.dp)
            )

            Column {
               CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                  Text(
                     text = "Flagged by ${important.user}".uppercase(),
                     style = MaterialTheme.typography.overline,
                     fontWeight = FontWeight.SemiBold,
                  )
               }

               if (important.description != null) {
                  Text(
                     text = important.description
                  )
               }
            }
         }
      }
   }
}