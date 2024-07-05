package mil.nga.giat.mage.data.repository.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.TypedValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.google.maps.android.geometry.Bounds
import mil.nga.giat.mage.data.datasource.DataSource
import mil.nga.giat.mage.data.datasource.observation.ObservationLocationLocalDataSource
import mil.nga.giat.mage.data.datasource.observation.ObservationMapItem
import mil.nga.giat.mage.data.repository.event.EventRepository
import mil.nga.giat.mage.data.repository.observation.icon.ObservationIconRepository
import mil.nga.giat.mage.map.annotation.ShapeStyle
import mil.nga.giat.mage.ui.map.overlay.DataSourceImage
import mil.nga.giat.mage.ui.map.overlay.DataSourceTileProvider
import mil.nga.sf.Geometry
import mil.nga.sf.LineString
import mil.nga.sf.Point
import mil.nga.sf.Polygon
import java.io.FileInputStream
import java.util.Date
import javax.inject.Inject

class ObservationTileRepository @Inject constructor(
    private val observationId: Long,
    private val localDataSource: ObservationLocationLocalDataSource
): TileRepository {
    override suspend fun getTileableItems(
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double
    ): List<DataSourceImage> {
        return localDataSource.observationLocations(
            observationId = observationId,
            minLatitude = minLatitude,
            maxLatitude = maxLatitude,
            minLongitude = minLongitude,
            maxLongitude = maxLongitude
        ).map {
            ObservationMapImage(ObservationMapItem(it))
        }
    }
}

class ObservationsTileRepository @Inject constructor(
    private val localDataSource: ObservationLocationLocalDataSource,
    private val observationIconRepository: ObservationIconRepository,
    private val eventRepository: EventRepository,
    private val context: Context
): TileRepository {

    private val _refresh = MutableLiveData(Date())
    val refresh: LiveData<Date> = _refresh

    override suspend fun getTileableItems(
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double
    ): List<DataSourceImage> {
        val event = eventRepository.getCurrentEvent()
        val items = event?.remoteId?.let { currentEventId ->
            localDataSource.observationLocations(
                currentEventId,
                minLatitude,
                maxLatitude,
                minLongitude,
                maxLongitude
            ).map {
                val mapItem = ObservationMapItem(it)
                val formDefinition = mapItem.formId?.let { formId ->
                    eventRepository.getForm(formId)
                }
                val style = ShapeStyle.fromObservationProperties(
                    event = event,
                    formDefinition = formDefinition,
                    primaryPropertyValue = mapItem.primaryFieldText,
                    secondaryPropertyValue = mapItem.secondaryFieldText,
                    context = context)

                ObservationMapImage(
                    mapItem = mapItem,
                    shapeStyle = style
                )
            }
        }

        items?.let {
            return it
        }
        return emptyList()
    }

    private fun getScreenScale(): Float {
        val displayMetrics = context.resources.displayMetrics
        return displayMetrics.density
    }

    suspend fun getObservationMapItems(
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double,
        latitudePerPixel: Float,
        longitudePerPixel: Float,
        zoom: Float,
        precise: Boolean,
        tolerance: Double
    ): List<ObservationMapItem>? {
        // determine widest and tallest icon at this zoom level pixels
        val iconPixelSize = observationIconRepository.getMaxHeightAndWidth(zoom)

        // this is how many degrees to add and subtract to ensure we query for the item around the tap location
        val iconToleranceHeightDegrees = latitudePerPixel * iconPixelSize.first
        val iconToleranceWidthDegrees = longitudePerPixel * iconPixelSize.second

        val queryLocationMinLongitude = minLongitude - iconToleranceWidthDegrees
        val queryLocationMaxLongitude = maxLongitude + iconToleranceWidthDegrees
        val queryLocationMinLatitude = minLatitude - iconToleranceHeightDegrees
        val queryLocationMaxLatitude = maxLatitude + iconToleranceHeightDegrees

        eventRepository.getCurrentEvent()?.let { event ->
            val items = localDataSource.observationLocations(
                event.remoteId,
                queryLocationMinLatitude,
                queryLocationMaxLatitude,
                queryLocationMinLongitude,
                queryLocationMaxLongitude
            )

            if (precise) {
                val matchedItems: MutableList<ObservationMapItem> = mutableListOf()
                items.asSequence()
                    .forEach { item ->
                        val observationTileRepo = ObservationTileRepository(item.observationId, localDataSource)
                        val tileProvider = DataSourceTileProvider(context, observationTileRepo)
                        when (item.geometry) {
                            is Point -> {
                                val include = markerHitTest(
                                    location = LatLng(
                                        maxLatitude - ((maxLatitude - minLatitude) / 2.0),
                                        maxLongitude - ((maxLongitude - minLongitude) / 2.0)
                                    ),
                                    zoom = zoom,
                                    tileProvider = tileProvider
                                )
                                if (include) {
                                    matchedItems.add(ObservationMapItem(item))
                                }
                            }
                            is Polygon -> {
                                val include = PolyUtil.containsLocation(
                                    LatLng(
                                        maxLatitude - ((maxLatitude - minLatitude) / 2.0),
                                        maxLongitude - ((maxLongitude - minLongitude) / 2.0)
                                    ),
                                    item.geometry.getRing(0).points.map { point -> LatLng(point.y, point.x) },
                                    false
                                )
                                if (include) {
                                    matchedItems.add(ObservationMapItem(item))
                                }
                            }
                            is LineString -> {
                                val include = PolyUtil.isLocationOnPath(
                                    LatLng(
                                        maxLatitude - ((maxLatitude - minLatitude) / 2.0),
                                        maxLongitude - ((maxLongitude - minLongitude) / 2.0)
                                    ),
                                    item.geometry.points.map { point -> LatLng(point.y, point.x) },
                                    false,
                                    tolerance
                                )
                                if (include) {
                                    matchedItems.add(ObservationMapItem(item))
                                }
                            }
                            else -> { }
                        }
                    }
                return matchedItems
            }
            return items.map {
                ObservationMapItem(it)
            }
        }
        return emptyList()
    }
}

class ObservationMapImage(private val mapItem: ObservationMapItem, private val shapeStyle: ShapeStyle? = null) : DataSourceImage {
    override val dataSource: DataSource = DataSource.Observation
    override val geometry: Geometry? = mapItem.geometry

    override fun image(
        context: Context,
        zoom: Int,
        tileBounds: Bounds,
        tileSize: Double
    ): List<Bitmap> {

        when (geometry) {
            is Point -> {
                val bitmap = getBitmap(context)
                var width = (zoom.toFloat() / 18.0f).coerceIn(0.3f, 0.7f) * 32

                width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, width, context.resources.displayMetrics)
                val image = bitmap?.resizeBitmapToWidthAspectScaled(width)

                return listOfNotNull(image)
            }
            is Polygon -> {
                return listOfNotNull(polygonImage(
                    context = context,
                    polygon = geometry,
                    mapZoom = zoom,
                    tileBounds = tileBounds,
                    tileSize = tileSize,
                    fillColor = shapeStyle?.fillColor,
                    strokeColor = shapeStyle?.strokeColor,
                    stroke = shapeStyle?.strokeWidth
                ))
            }
            is LineString -> {
                return listOfNotNull(lineImage(
                    context = context,
                    lineString = geometry,
                    mapZoom = zoom,
                    tileBounds = tileBounds,
                    tileSize = tileSize,
                    strokeColor = shapeStyle?.strokeColor,
                    stroke = shapeStyle?.strokeWidth
                ))
            }
            else -> {
                return emptyList()
            }
        }
    }

    fun getBitmap(context: Context): Bitmap? {
        return mapItem.getIcon(context)?.let {
            val inputStream = FileInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            // Close the input stream
            inputStream.close()
            bitmap
        }
    }
}

fun Bitmap.resizeBitmapToWidthAspectScaled(newWidth: Float): Bitmap {
    val aspectRatio = this.height.toFloat() / this.width
    val newHeight = (newWidth * aspectRatio)

    val matrix = Matrix()
    matrix.postScale(newWidth / this.width, newHeight / this.height)

    return Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
}