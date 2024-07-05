package mil.nga.giat.mage.ui.map.overlay

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import com.google.maps.android.geometry.Bounds
import kotlinx.coroutines.runBlocking
import mil.nga.giat.mage.data.datasource.DataSource
import mil.nga.giat.mage.data.repository.map.TileRepository
import mil.nga.giat.mage.ui.location.webMercatorToWgs84
import mil.nga.giat.mage.ui.location.wgs84ToWebMercator
import mil.nga.sf.Geometry
import mil.nga.sf.GeometryType
import mil.nga.sf.Point
import mil.nga.sf.LineString
import mil.nga.sf.Polygon
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.tan

interface DataSourceImage {
    val geometry: Geometry?
    val dataSource: DataSource

    fun image(
        context: Context,
        zoom: Int,
        tileBounds: Bounds,
        tileSize: Double
    ): List<Bitmap>

    fun pointImage(
        context: Context,
        mapZoom: Int,
    ): Bitmap {
        val scale = context.resources.displayMetrics.density * 2.5
        val size = ((mapZoom) * scale).toInt()

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val circleSize = size / 2f
        canvas.drawCircle(
            circleSize,
            circleSize,
            circleSize / 2,
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = dataSource.color.toArgb()
            }
        )

        if (mapZoom > 6) {
            val iconSize = (circleSize * .6).toInt()
            val icon = AppCompatResources.getDrawable(context, dataSource.icon)!!
            icon.setBounds(0, 0, iconSize, iconSize)
            canvas.drawBitmap(
                icon.toBitmap(),
                null,
                RectF(
                    ((circleSize / 2) + (circleSize - iconSize) / 2),
                    ((circleSize / 2) + (circleSize - iconSize) / 2),
                    (circleSize + (circleSize / 2) - (circleSize - iconSize) / 2),
                    (circleSize + (circleSize / 2) - (circleSize - iconSize) / 2)
                ),
                null
            )
        }

        return bitmap
    }

    fun lineImage(
        context: Context,
        lineString: LineString,
        strokeColor: Int?,
        stroke: Float?,
        mapZoom: Int,
        tileBounds: Bounds,
        tileSize: Double
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(tileSize.toInt(), tileSize.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val path = Path()
        val firstPoint = lineString.points.first()
        var crosses180th = false
        for(i in 1 until lineString.points.count()){
            if(abs(lineString.points[i].x - lineString.points[i-1].x) > 180){
                crosses180th = true
                break
            }
        }
        val firstPixel = toPixel(LatLng(firstPoint.y, firstPoint.x), tileBounds, tileSize, crosses180th)
        path.moveTo(firstPixel.x.toFloat(), firstPixel.y.toFloat())

        lineString.points.drop(1).forEach { point ->
            val pixel = toPixel(LatLng(point.y, point.x), tileBounds, tileSize, crosses180th)
            path.lineTo(pixel.x.toFloat(), pixel.y.toFloat())
        }

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = stroke ?: 1.0f
            color = strokeColor ?: dataSource.color.toArgb()
        }

        canvas.drawPath(path, paint)

        return bitmap
    }

    fun polygonImage(
        context: Context,
        polygon: Polygon,
        strokeColor: Int?,
        fillColor: Int?,
        stroke: Float?,
        mapZoom: Int,
        tileBounds: Bounds,
        tileSize: Double
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(tileSize.toInt(), tileSize.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val path = Path()
        val lineString = polygon.exteriorRing
        val firstPoint = lineString.points.first()

        var crosses180th = false
        for(i in 1 until lineString.points.count()){
            if(abs(lineString.points[i].x - lineString.points[i-1].x) > 180){
                crosses180th = true
                break
            }
        }
        val firstPixel = toPixel(LatLng(firstPoint.y, firstPoint.x), tileBounds, tileSize, crosses180th)
        path.moveTo(firstPixel.x.toFloat(), firstPixel.y.toFloat())

        lineString.points.drop(1).forEach { point ->
            val pixel = toPixel(LatLng(point.y, point.x), tileBounds, tileSize, crosses180th)
            path.lineTo(pixel.x.toFloat(), pixel.y.toFloat())
        }

        canvas.drawPath(
            path,
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = stroke ?: 1.0f
                color = strokeColor ?: dataSource.color.toArgb()
            }
        )

        canvas.drawPath(
            path,
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                strokeWidth = stroke ?: 1.0f
                color = fillColor ?: dataSource.color.copy(alpha = .3f).toArgb()
            }
        )

        return bitmap
    }

    private fun toPixel(latLng: LatLng, tileBounds3857: Bounds, tileSize: Double, crosses180th: Boolean): Point {
        var object3857Location = to3857(latLng.latitude, latLng.longitude)

        if (crosses180th && (latLng.longitude < -90 || latLng.longitude > 90)) {
            // if the x location has fallen off the left side and this tile is on the other side of the world
            if (object3857Location.x > tileBounds3857.minX && tileBounds3857.minX < 0 && object3857Location.x > 0) {
                object3857Location = to3857(latLng.latitude, latLng.longitude-360)
            }

            // if the x value has fallen off the right side and this tile is on the other side of the world
            if (object3857Location.x < tileBounds3857.maxX && tileBounds3857.maxX > 0 && object3857Location.x < 0) {
                object3857Location = to3857(latLng.latitude, latLng.longitude+360)
            }
        }

        val xPosition = (((object3857Location.x - tileBounds3857.minX) / (tileBounds3857.maxX - tileBounds3857.minX)) * tileSize)
        val yPosition = tileSize - (((object3857Location.y - tileBounds3857.minY) / (tileBounds3857.maxY - tileBounds3857.minY)) * tileSize)
        return Point(xPosition, yPosition)
    }

    private fun to3857(lat: Double, long: Double): Point {
        val a = 6378137.0
        val lambda = long / 180 * Math.PI
        val phi = lat / 180 * Math.PI
        val x = a * lambda
        val y = a * ln(tan(Math.PI / 4 + phi / 2))

        return Point(x, y)
    }
}

open class DataSourceTileProvider(
    private val context: Context,
    private val repository: TileRepository
) : TileProvider {

    var maximumZoom: Int = 7

    fun getBitmap(x: Int, y: Int, z: Int): Bitmap {
        val width = (context.resources.displayMetrics.density * 256).toInt()
        val height = (context.resources.displayMetrics.density * 256).toInt()

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

        items
            .asSequence()
            .filter { it.geometry != null } // Filter items with non-null geometry
            .sortedBy { // sort so polygons are below lines and points are drawn on top
                when (it.geometry?.geometryType) {
                    GeometryType.POINT -> 3
                    GeometryType.LINESTRING -> 2
                    GeometryType.POLYGON -> 1
                    GeometryType.MULTIPOINT -> 3
                    GeometryType.MULTILINESTRING -> 2
                    GeometryType.MULTIPOLYGON -> 1
                    else -> 0
                }
            }
            .associateWith { it.image(context, z, tileBounds, width.toDouble()) }
            .filter { it.value.isNotEmpty() } // Filter items with no images
            .forEach { itemToImages ->
                itemToImages.key.geometry?.centroid?.let { centroid ->

                    val (translateImages, images) = itemToImages.value
                        .partition { image ->
                            !(image.height == height && image.width == width)
                        }
                    translateImages.forEach { image ->
                        val webMercator = Point(centroid.x, centroid.y).wgs84ToWebMercator()
                        val xPosition =
                            (((webMercator.x - minTileX) / (maxTileX - minTileX)) * width)
                        val yPosition =
                            height - (((webMercator.y - minTileY) / (maxTileY - minTileY)) * height) - image.height
                        val destination = Rect(
                            xPosition.toInt() - image.width,
                            yPosition.toInt() - image.height,
                            xPosition.toInt() + image.width,
                            yPosition.toInt() + image.height
                        )
                        canvas.drawBitmap(image, null, destination, null)
                    }
                    images.forEach { image ->
                        canvas.drawBitmap(image, null, Rect(0, 0, width, height), null)
                    }
                }
            }
        return bitmap
    }

    override fun getTile(x: Int, y: Int, z: Int): Tile {
        if (z < 3 || z > maximumZoom) return TileProvider.NO_TILE
        val output = ByteArrayOutputStream()
        val bitmap = getBitmap(x, y, z)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        return Tile(bitmap.width, bitmap.height, output.toByteArray())
    }

    private fun longitude(x: Int, zoom: Int): Double {
        return x.toDouble() / 2.0.pow(zoom.toDouble()) * 360.0 - 180.0
    }

    private fun latitude(y: Int, zoom: Int): Double {
        val n = PI - 2.0 * PI * y / 2.0.pow(zoom.toDouble())
        return 180.0 / PI * atan(0.5 * (exp(n) - exp(-n)))
    }
}