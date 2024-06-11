package mil.nga.giat.mage.ui.location

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mil.nga.giat.mage.map.ObservationMapState
import mil.nga.giat.mage.map.UserMapState
import mil.nga.giat.mage.map.detail.ObservationAction
import mil.nga.giat.mage.map.detail.ObservationMapDetails
import mil.nga.giat.mage.map.detail.UserMapDetails
import mil.nga.giat.mage.map.detail.UserAction

@Composable
fun LocationSheetScreen(
    modifier: Modifier = Modifier,
    onDetails: (() -> Unit)? = null,
    onAction: (UserAction) -> Unit,
    viewModel: LocationViewModel = hiltViewModel()
) {
    val location by viewModel.location.observeAsState()

    Column(modifier = modifier) {
        LocationSheetContent(
            location = location,
            onDetails = onDetails,
            onAction = onAction
        )
    }
}

@Composable
private fun LocationSheetContent(
    location: UserMapState?,
    onDetails: (() -> Unit)? = null,
    onAction: (UserAction) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
        location?.let {
            UserMapDetails(
                userState = it,
                onAction = onAction,
            )
        }
    }
}