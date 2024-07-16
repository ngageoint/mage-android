package mil.nga.giat.mage.ui.map

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mil.nga.giat.mage.data.datasource.DataSource

@Serializable
@Parcelize
data class MapAnnotation2(
    val key: Key,
    val latitude: Double?,
    val longitude: Double?
) : Parcelable {
    enum class Type(val color: Color/*, val icon : Int*/) {
        OBSERVATION_LOCATION(DataSource.Observation.color),
        LOCATION(DataSource.Location.color),
        GEOPACKAGE(DataSource.GeoPackage.color)
//        ASAM(DataSource.ASAM.color, R.drawable.ic_asam_marker_24dp),
//        MODU(DataSource.MODU.color, R.drawable.ic_modu_marker_24dp),
//        LIGHT(DataSource.LIGHT.color, R.drawable.ic_light_marker_24dp),
//        PORT(DataSource.PORT.color, R.drawable.ic_port_marker_24dp),
//        RADIO_BEACON(DataSource.RADIO_BEACON.color, R.drawable.ic_beacon_marker_24dp),
//        DGPS_STATION(DataSource.DGPS_STATION.color, R.drawable.ic_dgps_marker_24dp),
//        NAVIGATIONAL_WARNING(DataSource.NAVIGATION_WARNING.color, R.drawable.ic_navigationwarning_marker_24dp),
//        GEOPACKAGE(DataSource.GEOPACKAGE.color, R.drawable.ic_feature_marker_24dp),
//        ROUTE(DataSource.ROUTE.color, DataSource.ROUTE.icon)
    }

    @Serializable
    @Parcelize
    data class Key(
        val id: String,
        val type: Type
    ) : Parcelable
}