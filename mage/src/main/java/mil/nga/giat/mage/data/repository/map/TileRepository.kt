package mil.nga.giat.mage.data.repository.map

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.location.Location
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.geometry.Bounds
import mil.nga.giat.mage.ui.map.overlay.DataSourceImage
import mil.nga.giat.mage.ui.map.overlay.DataSourceTileProvider
import mil.nga.sf.Geometry
import mil.nga.sf.Point
import mil.nga.sf.util.GeometryUtils
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tan

interface TileRepository {
    suspend fun getTileableItems(
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double
    ): List<DataSourceImage>

    val maximumZoom: Int
        get() = 7

    private fun longitudeFromTile(x: Int, zoom: Int): Double {
        return x / Math.pow(2.0, zoom.toDouble()) * 360.0 - 180.0
    }

    private fun latitudeFromTile(y: Int, zoom: Int): Double {
        val yLocation = Math.PI - 2.0 * Math.PI * y / 2.0.pow(zoom.toDouble())
        return 180.0 / Math.PI * Math.atan(0.5 * (exp(yLocation) - exp(-yLocation)))
    }

    fun markerHitTest(
        location: LatLng,
        hitBoxSouthWest: LatLng? = null,
        hitBoxNorthEast: LatLng? = null,
        zoom: Float,
        tileProvider: DataSourceTileProvider
    ): Boolean {
        val tile = location.toTile(zoom.toInt())
        val tileBitmap = tileProvider.getBitmap(tile.first, tile.second, zoom.toInt())

        val minTileLon = longitudeFromTile(tile.first, zoom.toInt())
        val maxTileLon = longitudeFromTile(tile.first + 1, zoom.toInt())
        val minTileLat = latitudeFromTile(tile.second + 1, zoom.toInt())
        val maxTileLat = latitudeFromTile(tile.second, zoom.toInt())

        val neCorner3857 = GeometryUtils.degreesToMeters(maxTileLon, maxTileLat)
        val swCorner3857 = GeometryUtils.degreesToMeters(minTileLon, minTileLat)

        // these are the min max x y in meters
        val minTileX = swCorner3857.x
        val minTileY = swCorner3857.y
        val maxTileX = neCorner3857.x
        val maxTileY = neCorner3857.y

        // The pixels that they touched
        var boundsRect: Rect
        if (hitBoxSouthWest != null && hitBoxNorthEast != null) {
            val nePixel =
                hitBoxNorthEast.toPixel(Bounds(minTileX, maxTileX, minTileY, maxTileY), tileBitmap.width.toDouble())
            val swPixel =
                hitBoxSouthWest.toPixel(Bounds(minTileX, maxTileX, minTileY, maxTileY), tileBitmap.width.toDouble())
            boundsRect = Rect(
                floor(nePixel.x).toInt(),
                floor(nePixel.y).toInt(),
                ceil(swPixel.x).toInt(),
                ceil(swPixel.y).toInt()
            )
        } else {
            val pixel = location.toPixel(Bounds(minTileX, maxTileX, minTileY, maxTileY), tileBitmap.width.toDouble())
            Log.d("TileRepository", "pixel: $pixel.x, $pixel.y")
            Log.d("TileRepository", "tile: $tile")
            boundsRect = Rect(
                floor(pixel.x).toInt(),
                floor(pixel.y).toInt(),
                ceil(pixel.x).toInt(),
                ceil(pixel.y).toInt()
            )
        }

        return tileBitmap.hasNonTransparentPixelInBounds(boundsRect)
    }
}

fun LatLng.long2Tile(zoom: Int): Int {
    val zoomExp = 2.0.pow(zoom.toDouble())
    return (zoomExp - 1.0).coerceAtMost(floor((this.longitude + 180.0) / 360.0 * zoomExp)).toInt()
}

/**
 * public func lat2Tile(zoom: Int) -> Int {
 *         let zoomExp = Double(pow(Double(2), Double(zoom)))
 *         return Int(
 *         floor(
 *             ((1.0 - log(tan((latitude * .pi) / 180.0) + 1.0 / cos((latitude * .pi) / 180.0)) / .pi) / 2.0) * zoomExp
 *         )
 *         )
 *     }
 */

fun LatLng.lat2Tile(zoom: Int): Int {
    val zoomExp = 2.0.pow(zoom.toDouble())
    return floor(
        ((1.0 - ln(tan((latitude * PI) / 180.0) + 1.0 / cos((latitude * PI) / 180.0)) / PI) / 2.0) * zoomExp).toInt()
}

fun LatLng.toTile(zoom: Int): Pair<Int, Int> {
    return Pair(long2Tile(zoom), lat2Tile(zoom))
}

/**
 * let xPosition = (
 *             (
 *                 (object3857Location.x - swCorner.x)
 *                 / (neCorner.x - swCorner.x)
 *             )
 *             * tileSize
 *         )
 *         let yPosition = tileSize - (
 *             (
 *                 (object3857Location.y - swCorner.y)
 *                 / (neCorner.y - swCorner.y)
 *             )
 *             * tileSize
 *         )
 *
 */
fun LatLng.toPixel(tileBounds3857: Bounds, tileSize: Double): Point {
    val object3857Location = to3857()
    val xPosition = (
            (
                    (object3857Location.x - tileBounds3857.minX)
                            / (tileBounds3857.maxX - tileBounds3857.minX)
                    )
                    * tileSize
            )
    val yPosition = tileSize - (
            (
                    (object3857Location.y - tileBounds3857.minY)
                            / (tileBounds3857.maxY - tileBounds3857.minY)
                    ) * tileSize)
    return Point(xPosition, yPosition)
}

private fun LatLng.to3857(): Point {
    val a = 6378137.0
    val lambda = longitude / 180 * Math.PI
    val phi = latitude / 180 * Math.PI
    val x = a * lambda
    val y = a * ln(tan(Math.PI / 4 + phi / 2))

    return Point(x, y)
}

fun Bitmap.hasNonTransparentPixelInBounds(bounds: Rect): Boolean {
    for (x in bounds.left until bounds.right) {
        for (y in bounds.top until bounds.bottom) {
            // Get the pixel color
            val color = getPixel(x, y)

            // Check if the alpha channel is not 0 (transparent)
            if (Color.alpha(color) != 0) {
                return true // Non-transparent pixel found
            }
        }
    }

    // No non-transparent pixels found
    return false
}
