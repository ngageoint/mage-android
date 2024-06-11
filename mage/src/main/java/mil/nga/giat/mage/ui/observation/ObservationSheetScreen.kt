package mil.nga.giat.mage.ui.observation

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mil.nga.giat.mage.map.ObservationMapState
import mil.nga.giat.mage.map.detail.ObservationAction
import mil.nga.giat.mage.map.detail.ObservationMapDetails

@Composable
fun ObservationSheetScreen(
    modifier: Modifier = Modifier,
    onDetails: (() -> Unit)? = null,
    onAction: (ObservationAction) -> Unit,
    viewModel: ObservationViewModel = hiltViewModel()
) {
    val observation by viewModel.observation.observeAsState()

    Column(modifier = modifier) {
        ObservationSheetContent(
            observation = observation,
            onDetails = onDetails,
            onAction = onAction
        )
    }
}

@Composable
private fun ObservationSheetContent(
    observation: ObservationMapState?,
    onDetails: (() -> Unit)? = null,
    onAction: (ObservationAction) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
        observation?.let {
            ObservationMapDetails(
                observationMapState = it,
                onAction = onAction,
            )
        }
    }
}