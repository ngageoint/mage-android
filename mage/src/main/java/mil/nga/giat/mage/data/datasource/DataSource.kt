package mil.nga.giat.mage.data.datasource

import androidx.compose.ui.graphics.Color
import mil.nga.giat.mage.R

enum class DataSource(
    val mappable: Boolean,
    val color: Color,
    val icon: Int,
    val label: String,
    val labelPlural: String = "${label}s",
    val tableName: String? = null,
) {
    Observation(
        mappable = true,
        color = Color(0xFF000000),
        icon = R.drawable.observation_form_icon,
        label = "Observation",
        tableName = "observations"
    ),
    Location(
        mappable = true,
        color = Color(0xFF000000),
        icon = R.drawable.observation_form_icon,
        label = "Location",
        tableName = "locations"
    ),
    GeoPackage(
        mappable = true,
        color = Color(0xFF000000),
        icon = R.drawable.observation_form_icon,
        label = "GeoPackage"
    )
}