package mil.nga.giat.mage.data.repository.map

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.util.TypedValue
import androidx.core.net.toFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.geometry.Bounds
import dagger.hilt.android.qualifiers.ApplicationContext
import mil.nga.giat.mage.data.datasource.DataSource
import mil.nga.giat.mage.data.datasource.observation.ObservationLocationLocalDataSource
import mil.nga.giat.mage.data.datasource.observation.ObservationMapItem
import mil.nga.giat.mage.data.repository.event.EventRepository
import mil.nga.giat.mage.data.repository.observation.ObservationRepository
import mil.nga.giat.mage.data.repository.observation.icon.ObservationIconRepository
import mil.nga.giat.mage.database.model.observation.Observation
import mil.nga.giat.mage.ui.map.overlay.DataSourceImage
import mil.nga.giat.mage.ui.map.overlay.DataSourceTileProvider
import mil.nga.sf.Geometry
import mil.nga.sf.Point
import java.io.FileInputStream
import java.util.Date
import javax.inject.Inject
import kotlin.math.ceil

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

//    var eventIdToMaxIconSize: MutableMap<Long, Pair<Int, Int>> = mutableMapOf()

    override suspend fun getTileableItems(
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double
    ): List<DataSourceImage> {
        val items = eventRepository.getCurrentEvent()?.remoteId?.let { currentEventId ->
            localDataSource.observationLocations(
                currentEventId,
                minLatitude,
                maxLatitude,
                minLongitude,
                maxLongitude
            ).map {
                ObservationMapImage(ObservationMapItem(it))
            }
        }

        items?.let {
            return it
        }
        return emptyList()
    }

//    suspend fun getMaximumIconHeightToWidthRatio(): Pair<Int, Int> {
//        eventRepository.getCurrentEvent()?.let { event ->
//            eventIdToMaxIconSize[event.id]?.let {
//                return it
//            }
//            val size = observationIconRepository.getMaximumIconHeightToWidthRatio(eventId = event.id)
//            eventIdToMaxIconSize[event.id] = size
//            return size
//        }
//
//        return Pair(0,0)
//    }

    fun testForMocking(): Int {
        return 1
    }

//    suspend fun getMaxHeightAndWidth(zoom: Float): Pair<Int, Int> {
//        // icons should be a max of 35 wide
////        val pixelWidthTolerance = 0.3.coerceAtLeast((zoom / 18.0)) * 35
//        var pixelWidthTolerance = (zoom.toDouble() / 18.0).coerceIn(0.3, 0.7) * 32
//
//        pixelWidthTolerance = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pixelWidthTolerance.toFloat(), application.resources.displayMetrics).toDouble()
//        // if the icon is pixelWidthTolerance wide, the max height is this
//        val pixelHeightTolerance = (pixelWidthTolerance / getMaximumIconHeightToWidthRatio().first) * getMaximumIconHeightToWidthRatio().second
//        return Pair(ceil(pixelWidthTolerance * getScreenScale()).toInt(), ceil(pixelHeightTolerance * getScreenScale()).toInt())
//    }

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
        precise: Boolean
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
                        if (item.geometry is Point) {
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

class ObservationMapImage(private val mapItem: ObservationMapItem) : DataSourceImage {
    override val dataSource: DataSource = DataSource.Observation
    override val geometry: Geometry? = mapItem.geometry

    override fun image(
        context: Context,
        zoom: Int,
        tileBounds: Bounds,
        tileSize: Double
    ): List<Bitmap> {
        val coordinate = geometry?.centroid
        val bitmap = getBitmap(context)
        var width = (zoom.toDouble() / 18.0).coerceIn(0.3, 0.7) * 32

        width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, width.toFloat(), context.resources.displayMetrics).toDouble()
        Log.w("Whatever", "width: $width density ${context.resources.displayMetrics.density}")
        val image = bitmap?.let {
            resizeBitmapToWidthAspectScaled(it, width.toInt())
        }

        return listOfNotNull(image)
    }

    fun getBitmap(context: Context): Bitmap? {
        return mapItem.getIcon(context)?.let {
            val inputStream = FileInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            // Close the input stream
            inputStream?.close()
            bitmap
        }
    }

    fun resizeBitmapToWidthAspectScaled(bitmap: Bitmap, newWidth: Int): Bitmap {
        val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
        val newHeight = (newWidth * aspectRatio).toInt()

        val matrix = Matrix()
        matrix.postScale(newWidth.toFloat() / bitmap.width, newHeight.toFloat() / bitmap.height)

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

//    companion object {
//        private val LOG_NAME = ObservationMapImage::class.java.simpleName
//    }
}