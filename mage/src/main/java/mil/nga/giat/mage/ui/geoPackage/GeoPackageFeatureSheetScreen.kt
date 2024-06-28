package mil.nga.giat.mage.ui.geoPackage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mil.nga.giat.mage.map.GeoPackageFeatureMapState
import mil.nga.giat.mage.map.detail.GeoPackageFeatureDetails

@Composable
fun GeoPackageFeatureSheetScreen(
    modifier: Modifier = Modifier,
    onDetails: (() -> Unit)? = null,
    onAction: (Any) -> Unit,
    viewModel: GeoPackageFeatureViewModel = hiltViewModel()
) {
    val feature by viewModel.feature.observeAsState()

    Column(modifier = modifier) {
        GeoPackageFeatureSheetContent(
            feature = feature,
            onDetails = onDetails,
            onAction = onAction
        )
    }
}

@Composable
private fun GeoPackageFeatureSheetContent(
    feature: GeoPackageFeatureMapState?,
    onDetails: (() -> Unit)? = null,
    onAction: (Any) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
        feature?.let {
            GeoPackageFeatureDetails(featureMapState = feature, onAction = onAction)
        }
    }
}