package mil.nga.giat.mage.feed

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.google.accompanist.glide.rememberGlidePainter
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.gms.maps.model.LatLng
import mil.nga.giat.mage.R
import mil.nga.giat.mage.coordinate.CoordinateFormatter
import mil.nga.giat.mage.ui.theme.MageTheme
import mil.nga.giat.mage.ui.theme.linkColor
import mil.nga.giat.mage.ui.theme.topAppBarBackground
import java.util.*

sealed class FeedItemAction {
   class Click(val item: FeedItemState): FeedItemAction()
   class Location(val text: String): FeedItemAction()
   class Directions(val item: FeedItemState): FeedItemAction()
}

@Composable
fun FeedScreen(
   viewModel: FeedViewModel,
   onClose: (() -> Unit)? = null,
   onRefresh: (() -> Unit)? = null,
   onItemAction: ((FeedItemAction) -> Unit)? = null
) {
   val feed by viewModel.feed.observeAsState()
   val feedItems by viewModel.feedItems.observeAsState()
   val snackbar by viewModel.snackbar.collectAsState()
   val scaffoldState = rememberScaffoldState()
   val isRefreshing by viewModel.isRefreshing.collectAsState()

   LaunchedEffect(scaffoldState.snackbarHostState, snackbar) {
      if (snackbar.message.isNotEmpty()) {
         scaffoldState.snackbarHostState.showSnackbar(snackbar.message, duration = SnackbarDuration.Short)
      }
   }

   MageTheme {
      Scaffold(
         scaffoldState = scaffoldState,
         topBar = {
            FeedItemTopBar(
               title = feed?.title,
               onClose = { onClose?.invoke() }
            )
         },
         content = {
            SwipeRefresh(
               state = rememberSwipeRefreshState(isRefreshing),
               onRefresh = { onRefresh?.invoke() },
               indicator = { state, trigger ->
                  SwipeRefreshIndicator(
                     state = state,
                     refreshTriggerDistance = trigger,
                     contentColor = MaterialTheme.colors.primary,
                  )
               }
            ) {
               feedItems?.collectAsLazyPagingItems()?.let { items ->
                  if (items.itemCount == 0) {
                     FeedNoContent()
                  } else {
                     FeedContent(items, onItemAction)
                  }
               }
            }
         }
      )
   }
}

@Composable
fun FeedItemTopBar(
   title: String?,
   onClose: () -> Unit
) {
   TopAppBar(
      backgroundColor = MaterialTheme.colors.topAppBarBackground,
      contentColor = Color.White,
      title = {
         Text(
            text = title ?: "",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
         )
      },
      navigationIcon = {
         IconButton(onClick = { onClose.invoke() }) {
            Icon(Icons.Default.ArrowBack, "Back To MAGE")
         }
      }
   )
}

@Composable
private fun FeedContent(
   feedItems: LazyPagingItems<FeedItemState>,
   onItemAction: ((FeedItemAction) -> Unit)?
) {
   LazyColumn(
      modifier = Modifier
         .background(Color(0x19000000))
         .padding(horizontal = 8.dp),
      contentPadding = PaddingValues(top = 16.dp)
   ) {
      items(feedItems) { item ->
         if (item != null) {
            FeedItemContent(
               itemState = item,
               onLocationClick = { onItemAction?.invoke(FeedItemAction.Location(it)) },
               onDirectionsClick = {
                  onItemAction?.invoke(
                     FeedItemAction.Directions(
                        item
                     )
                  )
               },
               onItemClick = { onItemAction?.invoke(FeedItemAction.Click(item)) }
            )
         }
      }
   }
}

@Composable
private fun FeedNoContent() {
   LazyColumn(
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
         .padding(horizontal = 32.dp)
         .padding(bottom = 72.dp) // Offset by AppBar height + icon padding to give a centered feel
         .fillMaxHeight()
         .fillMaxWidth()
   ) {
      item {
         Icon(
            imageVector = Icons.Rounded.RssFeed,
            contentDescription = "No Feed Items Icon",
            tint = MaterialTheme.colors.onSurface,
            modifier = Modifier
               .alpha(ContentAlpha.disabled)
               .height(200.dp)
               .width(200.dp)
               .padding(bottom = 16.dp)
         )

         CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
               text = "No Feed Items",
               textAlign = TextAlign.Center,
               style = MaterialTheme.typography.h4,
               modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
               text = "This feed currently contains no data.",
               textAlign = TextAlign.Center,
               style = MaterialTheme.typography.body1
            )
         }
      }
   }
}

@Composable
fun FeedItemContent(
   itemState: FeedItemState,
   onItemClick: (() -> Unit)? = null,
   onDirectionsClick: (() -> Unit)? = null,
   onLocationClick: ((String) -> Unit)? = null
) {
   Card(
      Modifier
         .fillMaxWidth()
         .padding(bottom = 8.dp)
         .clickable { onItemClick?.invoke() }
   ) {
      Column {
         Row(
            modifier = Modifier.padding(vertical = 16.dp)
         ) {
            Column(
               modifier = Modifier
                  .weight(1f)
                  .padding(horizontal = 16.dp),
               verticalArrangement = Arrangement.Center,
            ) {
               if (itemState.date == null && itemState.primary == null && itemState.secondary == null)  {
                  CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
                     Text(
                        text = "No Content",
                        fontSize = 32.sp,
                        style = MaterialTheme.typography.subtitle1
                     )
                  }
               } else {
                  if (itemState.date != null) {
                     Row(modifier = Modifier.padding(bottom = 16.dp)) {
                        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                           Text(
                              text = itemState.date.uppercase(Locale.ROOT),
                              fontWeight = FontWeight.Bold,
                              style = MaterialTheme.typography.overline
                           )
                        }
                     }
                  }

                  if (itemState.primary != null) {
                     Row(modifier = Modifier.padding(bottom = 4.dp)) {
                        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                           Text(
                              text = itemState.primary,
                              style = MaterialTheme.typography.h6,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis
                           )
                        }
                     }
                  }

                  if (itemState.secondary != null) {
                     Row {
                        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                           Text(
                              text = itemState.secondary,
                              style = MaterialTheme.typography.subtitle1,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis
                           )
                        }
                     }
                  }
               }
            }

            FeedItemIcon(
               itemState = itemState,
               modifier = Modifier
                  .padding(end = 16.dp)
                  .height(40.dp)
                  .width(40.dp)
            )
         }

         itemState.geometry?.let { geometry ->
            Row(
               horizontalArrangement = Arrangement.SpaceBetween,
               modifier = Modifier.fillMaxWidth()
            ) {
               val locationText = CoordinateFormatter(LocalContext.current).format(LatLng(geometry.centroid.y, geometry.centroid.x))
               Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                     .padding(8.dp)
                     .clickable { onLocationClick?.invoke(locationText) }
                     .padding(8.dp)
               ) {
                  Icon(
                     imageVector = Icons.Default.GpsFixed,
                     contentDescription = "Location",
                     tint = MaterialTheme.colors.linkColor,
                     modifier = Modifier
                        .height(24.dp)
                        .width(24.dp)
                        .padding(end = 4.dp)
                  )

                  CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                     Text(
                        text = locationText,
                        color = MaterialTheme.colors.linkColor,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.padding(end = 8.dp)
                     )
                  }
               }

               IconButton(
                  modifier = Modifier.padding(end = 8.dp),
                  onClick = { onDirectionsClick?.invoke() }
               ) {
                  Icon(
                     imageVector = Icons.Outlined.Directions,
                     contentDescription = "Directions",
                     tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
                  )
               }
            }
         }
      }
   }
}

@Composable
fun FeedItemIcon(
   itemState: FeedItemState,
   modifier: Modifier = Modifier,
) {
   Box(modifier = modifier) {
      Image(
         painter = rememberGlidePainter(
            itemState.iconUrl,
            fadeIn = true,
            requestBuilder = {
               error(R.drawable.default_marker)
            }
         ),
         contentDescription = "Feed Item Icon",
         Modifier.fillMaxSize()
      )
   }
}