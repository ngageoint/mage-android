package mil.nga.giat.mage.ui.map.overlay

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import com.google.maps.android.geometry.Bounds
import kotlinx.coroutines.runBlocking
import mil.nga.giat.mage.data.datasource.DataSource
import mil.nga.giat.mage.data.repository.map.TileRepository
import mil.nga.giat.mage.ui.location.webMercatorToWgs84
import mil.nga.giat.mage.ui.location.wgs84ToWebMercator
import mil.nga.sf.Point
import mil.nga.sf.geojson.Feature
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow

interface DataSourceImage {
    val feature: Feature
    val dataSource: DataSource

    fun image(
        context: Context,
        zoom: Int,
        tileBounds: Bounds,
        tileSize: Double
    ): List<Bitmap>
}

open class DataSourceTileProvider(
    private val application: Application,
    private val repository: TileRepository
) : TileProvider {
    override fun getTile(x: Int, y: Int, z: Int): Tile {
        if (z < 3) return TileProvider.NO_TILE

        val width = (application.resources.displayMetrics.density * 256).toInt()
        val height = (application.resources.displayMetrics.density * 256).toInt()

        val minTileLon = longitude(x = x, zoom = z)
        val maxTileLon = longitude(x = x + 1, zoom = z)
        val minTileLat = latitude(y = y + 1, zoom = z)
        val maxTileLat = latitude(y = y, zoom = z)

        val neCorner3857 = Point(maxTileLon, maxTileLat).wgs84ToWebMercator()
        val swCorner3857 = Point(minTileLon, minTileLat).wgs84ToWebMercator()
        val minTileX = swCorner3857.x
        val minTileY = swCorner3857.y
        val maxTileX = neCorner3857.x
        val maxTileY = neCorner3857.y

        // Border tile by 40 miles, biggest light in MSI.
        // Border has to be at least 256 pixels as well
        val tolerance = max(40.0 * 1852, ((maxTileX - minTileX) / (width / 2)) * 40)

        val neCornerTolerance = Point(maxTileX + tolerance, maxTileY + tolerance).webMercatorToWgs84()
        val swCornerTolerance = Point(minTileX - tolerance, minTileY - tolerance).webMercatorToWgs84()
        val minQueryLon = swCornerTolerance.x
        val maxQueryLon = neCornerTolerance.x
        val minQueryLat = swCornerTolerance.y
        val maxQueryLat = neCornerTolerance.y

        val tileBounds = Bounds(
            swCorner3857.x,
            neCorner3857.x,
            swCorner3857.y,
            neCorner3857.y
        )

        val items = runBlocking {
            repository.getTileableItems(
                minLatitude = minQueryLat,
                maxLatitude = maxQueryLat,
                minLongitude = minQueryLon,
                maxLongitude = maxQueryLon
            )
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        items.forEach { item ->
            item.image(application, z, tileBounds, width.toDouble()).forEach { image ->
                val translate = !(image.height == height && image.width == width)
                val destination = if (translate) {
                    val centroid = item.feature.geometry.geometry.centroid
                    val webMercator = Point(centroid.x, centroid.y).wgs84ToWebMercator()
                    val xPosition = (((webMercator.x - minTileX) / (maxTileX - minTileX)) * width)
                    val yPosition = height - (((webMercator.y - minTileY) / (maxTileY - minTileY)) * height)
                    Rect(
                        xPosition.toInt() - image.width,
                        yPosition.toInt() - image.height,
                        xPosition.toInt() + image.width,
                        yPosition.toInt() + image.height
                    )
                } else Rect(0, 0, width, height)

                canvas.drawBitmap(image, null, destination, null)
            }
        }

        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        return Tile(width, height, output.toByteArray())
    }

    private fun longitude(x: Int, zoom: Int): Double {
        return x.toDouble() / 2.0.pow(zoom.toDouble()) * 360.0 - 180.0
    }

    private fun latitude(y: Int, zoom: Int): Double {
        val n = PI - 2.0 * PI * y / 2.0.pow(zoom.toDouble())
        return 180.0 / PI * atan(0.5 * (exp(n) - exp(-n)))
    }
}