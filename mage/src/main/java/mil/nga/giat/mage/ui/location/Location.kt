package mil.nga.giat.mage.ui.location

import android.location.Location
import mil.nga.sf.Point
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.tan

fun Location.generalDirection(location: Location): String {
    val directions = listOf("N","NNE","NE","ENE","E","ESE","SE","SSE","S","SSW","SW","WSW","W","WNW", "NW", "NNW")
    val bearingCorrection = 360.0 / directions.size * 2.0
    val indexDegrees = 360.0 / directions.size

    var bearing = bearingTo(location).toDouble()
    bearing += bearingCorrection
    if (bearing < 0) {
        bearing += 360
    }
    if (bearing > 360) {
        bearing -= 360
    }
    val index = (bearing / indexDegrees).roundToInt() % directions.size
    return directions[index]
}

fun Point.wgs84ToWebMercator(): Point {
    val a = 6378137.0
    val lambda = x / 180 * PI
    val phi = y / 180 * PI
    val x = a * lambda
    val y = a * ln(tan(PI / 4 + phi / 2))

    return Point(x, y)
}

fun Point.webMercatorToWgs84(): Point {
    val a = 6378137.0
    val d = -y / a
    val phi = PI / 2 - 2 * atan(exp(d))
    val lambda = x / a
    val latitude = phi / PI * 180
    val longitude = lambda / PI * 180

    return Point(longitude, latitude)
}

fun Double.toDegrees(): Double {
    return this * 180.0 / PI
}
