package mil.nga.giat.mage.ui.login

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import mil.nga.giat.mage.ui.theme.onSurfaceDisabled

@Composable
fun AppVersion(version: String) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceDisabled) {
        Text(
            text = "App Version: $version",
            style = MaterialTheme.typography.bodySmall
        )
    }
}