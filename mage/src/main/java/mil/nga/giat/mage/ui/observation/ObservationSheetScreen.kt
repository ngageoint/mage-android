package mil.nga.giat.mage.ui.observation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mil.nga.giat.mage.map.ObservationLocationMapState
import mil.nga.giat.mage.map.ObservationMapState
import mil.nga.giat.mage.map.detail.ObservationAction
import mil.nga.giat.mage.map.detail.ObservationLocationMapDetails
import mil.nga.giat.mage.map.detail.ObservationMapDetails

@Composable
fun ObservationLocationSheetScreen(
    modifier: Modifier = Modifier,
    onDetails: (() -> Unit)? = null,
    onAction: (ObservationAction) -> Unit,
    viewModel: ObservationLocationViewModel = hiltViewModel()
) {
    val observationLocation by viewModel.observationLocation.observeAsState()
    if (observationLocation?.isPrimary == true) {
        observationLocation?.observationId?.let {
            val viewModel = hiltViewModel<ObservationLocationViewModel>()
            viewModel.setObservationLocationId(it)
            ObservationSheetScreen(
                modifier = modifier,
                onDetails = onDetails,
                onAction = onAction
            )
        }
    } else {
        Column(modifier = modifier) {
            ObservationLocationSheetContent(
                observationLocation = observationLocation,
                onDetails = onDetails,
                onAction = onAction
            )
        }
    }
}

@Composable
private fun ObservationLocationSheetContent(
    observationLocation: ObservationLocationMapState?,
    onDetails: (() -> Unit)? = null,
    onAction: (ObservationAction) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
        observationLocation?.let {
            ObservationLocationMapDetails(
                observationLocationMapState = it,
                onAction = onAction,
            )
        }
    }
}

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