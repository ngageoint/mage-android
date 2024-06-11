package mil.nga.giat.mage.ui.sheet

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import mil.nga.giat.mage.map.detail.ObservationAction
import mil.nga.giat.mage.map.detail.UserAction
import mil.nga.giat.mage.observation.view.ObservationViewActivity
import mil.nga.giat.mage.ui.location.LocationSheetScreen
import mil.nga.giat.mage.ui.location.LocationViewModel
import mil.nga.giat.mage.ui.map.MapAnnotation2
import mil.nga.giat.mage.ui.observation.ObservationSheetScreen
import mil.nga.giat.mage.ui.observation.ObservationViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomSheet(
    onDetails: (MapAnnotation2) -> Unit,
    onShare: (Pair<String, String>) -> Unit,
    onDismiss: () -> Unit,
    viewModel: DataSourceSheetViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mapAnnotations by viewModel.mapAnnotations.observeAsState(emptyList())
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        pageCount = { mapAnnotations.size }
    )

    var badgeColor = remember(pagerState.currentPage, mapAnnotations) {
        mapAnnotations.getOrNull(pagerState.currentPage)?.key?.type?.color ?: Color.Transparent
    }

    Row(Modifier.height(400.dp)) {
        Box(
            Modifier
                .width(6.dp)
                .fillMaxHeight()
                .background(badgeColor)
        )

        Column {
            if (pagerState.pageCount > 1) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(60.dp)
                        .fillMaxWidth()
                ) {
                    val previousEnabled = pagerState.currentPage > 0
                    IconButton(
                        enabled = previousEnabled,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        })
                    {
                        Icon(
                            Icons.Default.ChevronLeft,
                            tint = if (previousEnabled) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.38f),
                            contentDescription = "Previous Page"
                        )
                    }

                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                        Text(
                            text = "${pagerState.currentPage + 1} of ${mapAnnotations.size ?: 0}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .padding(8.dp)
                        )
                    }
                    val nextEnabled = pagerState.currentPage < pagerState.pageCount - 1
                    IconButton(
                        enabled = nextEnabled,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }) {
                        Icon(
                            Icons.Default.ChevronRight,
                            tint = if (nextEnabled) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.38f),
                            contentDescription = "Next Page"
                        )
                    }
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val annotation = mapAnnotations[pagerState.currentPage]

                viewModel.annotationProvider.setMapAnnotation(annotation)
                badgeColor = annotation.key.type.color

                Column(modifier = Modifier.fillMaxWidth()) {
                    when (annotation.key.type) {
                        MapAnnotation2.Type.OBSERVATION -> {
                            ObservationPage(
                                id = annotation.key.id,
                                onDetails = { onDetails(annotation) },
                                onShare = { onShare(it) },
                                onAction = { action ->
                                    when (action) {
                                        is ObservationAction.Details -> {
                                            onDismiss()
                                            val intent = Intent(context, ObservationViewActivity::class.java)
                                            intent.putExtra(ObservationViewActivity.OBSERVATION_ID_EXTRA, action.id)
                                            context.startActivity(intent)
                                        }
                                        is ObservationAction.Directions -> TODO()
                                        is ObservationAction.Favorite -> TODO()
                                        is ObservationAction.Location -> TODO()
                                    }

                                }
                            )
                        }

                        MapAnnotation2.Type.LOCATION -> {
                            LocationPage(
                                id = annotation.key.id,
                                onDetails = { onDetails(annotation) },
                                onShare = { onShare(it) },
                                onAction = { action ->
                                }
                            )
                        }
                    }
                }
            }


        }
    }
}

@Composable
private fun ObservationPage(
    id: String,
    onDetails: () -> Unit,
    onShare: (Pair<String, String>) -> Unit,
    onAction: (ObservationAction) -> Unit
) {
    Log.d("ObservationPage", "id: $id")
    val longId = id.toLongOrNull()
    if (longId != null) {
        val viewModel = hiltViewModel<ObservationViewModel>()
        viewModel.setObservationId(longId)
        ObservationSheetScreen(
            onDetails = { onDetails() },
            onAction = onAction,
            modifier = Modifier.fillMaxHeight(),
            viewModel = viewModel
        )
    }
}

@Composable
private fun LocationPage(
    id: String,
    onDetails: () -> Unit,
    onShare: (Pair<String, String>) -> Unit,
    onAction: (UserAction) -> Unit
) {
    Log.d("LocationPage", "id: $id")
    val longId = id.toLongOrNull()
    if (longId != null) {
        val viewModel = hiltViewModel<LocationViewModel>()
        viewModel.setLocationId(longId)
        LocationSheetScreen(
            onDetails = { onDetails() },
            onAction = onAction,
            modifier = Modifier.fillMaxHeight(),
            viewModel = viewModel
        )
    }
}
