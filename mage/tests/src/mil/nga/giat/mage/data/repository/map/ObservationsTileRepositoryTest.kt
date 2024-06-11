package mil.nga.giat.mage.data.repository.map

import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.util.DisplayMetrics
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.geometry.Bounds
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import junit.framework.TestCase
import mil.nga.giat.mage.data.datasource.observation.ObservationLocationLocalDataSource
import mil.nga.giat.mage.data.repository.observation.icon.ObservationIconRepository
import mil.nga.giat.mage.database.model.observation.ObservationLocation
import mil.nga.sf.Point
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlinx.coroutines.test.runTest
import mil.nga.giat.mage.data.repository.event.EventRepository
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.sf.util.GeometryUtils
import org.junit.Assert
import org.junit.Before
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow

class ObservationsTileRepositoryTest {

    private lateinit var tileRepository: ObservationsTileRepository

    lateinit var context: Context

    @Before
    fun setUp() {
        InstrumentationRegistry.getInstrumentation().context
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun longitudeFromTile(x: Int, zoom: Int): Double {
        return x / Math.pow(2.0, zoom.toDouble()) * 360.0 - 180.0
    }

    private fun latitudeFromTile(y: Int, zoom: Int): Double {
        val yLocation = Math.PI - 2.0 * Math.PI * y / 2.0.pow(zoom.toDouble())
        return 180.0 / Math.PI * Math.atan(0.5 * (exp(yLocation) - exp(-yLocation)))
    }

    @Test
    fun testStuff() = runTest {
        val location = LatLng(39.62601343172716,-104.90165054798126)
        val zoom = 13.7806
        val tile = location.toTile(13)

        val minTileLon = longitudeFromTile(tile.first, zoom.toInt())
        val maxTileLon = longitudeFromTile(tile.first + 1, zoom.toInt())
        val minTileLat = latitudeFromTile(tile.second + 1, zoom.toInt())
        val maxTileLat = latitudeFromTile(tile.second, zoom.toInt())

        val neCorner3857 = GeometryUtils.degreesToMeters(maxTileLon, maxTileLat)
        val swCorner3857 = GeometryUtils.degreesToMeters(minTileLon, minTileLat)

        val minTileX = swCorner3857.x
        val minTileY = swCorner3857.y
        val maxTileX = neCorner3857.x
        val maxTileY = neCorner3857.y

        val tileBitmapWidth = 720

        val pixel = location.toPixel(Bounds(minTileX, maxTileX, minTileY, maxTileY), tileBitmapWidth.toDouble())

        val boundsRect = Rect(
            floor(pixel.x).toInt(),
            floor(pixel.y).toInt(),
            ceil(pixel.x).toInt(),
            ceil(pixel.y).toInt()
        )
        println(boundsRect)
    }

    @Test
    fun testGetObservationMapItems() = runTest {

        // This does not work.  It cannot mock the method of the class.  I give up.
        val inputStream = javaClass.classLoader!!.getResourceAsStream("assets/110.png")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        mockkConstructor(ObservationMapImage::class)
        every { anyConstructed<ObservationMapImage>().getBitmap(any()) } returns bitmap

        val location = ObservationLocation(
            id = 1,
            eventRemoteId = "1",
            observationId = 1,
            geometry = Point(-104.90241, 39.62691),
            latitude = 39.62691,
            longitude = -104.90241,
            maxLatitude = 39.62691,
            maxLongitude = -104.90241,
            minLatitude = 39.62691,
            minLongitude = -104.90241
        )

        location.accuracy = null
        location.fieldName = null
        location.formId = 1
        location.order = 1
        location.provider = null
//        location.iconPath = Uri.parse("110.png")

        val mapItems: List<ObservationLocation> = listOf(location)

        val observationLocationDataStoreMock = mockkClass(ObservationLocationLocalDataSource::class)
        every { observationLocationDataStoreMock.observationLocations("1", any(), any(), any(), any()) } returns mapItems
        every { observationLocationDataStoreMock.observationLocations(1, any(), any(), any(), any()) } returns mapItems

        val observationIconRepositoryMock = mockkClass(ObservationIconRepository::class)
        every { observationIconRepositoryMock.getMaximumIconHeightToWidthRatio(any()) } returns Pair<Int, Int>(191, 260)

//        val observationTileRepositoryMock = mockkClass(ObservationsTileRepository::class)
        coEvery { observationIconRepositoryMock.getMaxHeightAndWidth(any()) } returns Pair<Int, Int>(178, 227)

        val eventRepositoryMock = mockkClass(EventRepository::class)
        val event = Event()
        event.remoteId = "1"
        coEvery { eventRepositoryMock.getCurrentEvent() } returns event

//        mockkConstructor(ObservationsTileRepository::class)
//        every { anyConstructed<ObservationsTileRepository>().testForMocking() } returns 2

//        val observationTileRepositoryMock = mockkClass(ObservationsTileRepository::class)
//        every { observationTileRepositoryMock.testForMocking() } returns 2

//        val navigator = mockk<ObservationsTileRepository>()
//        every { navigator.testForMocking() } returns 2
//
//        val mockedThing2 = navigator.testForMocking()
//        Assert.assertEquals(2, mockedThing2)

        val tileRepository = ObservationsTileRepository(
            observationLocationDataStoreMock,
            observationIconRepositoryMock,
            eventRepositoryMock,
            InstrumentationRegistry.getInstrumentation().context
        )

//        val mockedThing = tileRepository.testForMocking()
//        Assert.assertEquals(2, mockedThing)

//        coEvery { tileRepository.getMaxHeightAndWidth(any()) } returns Pair<Int, Int>(178, 227)

        val items = tileRepository.getObservationMapItems(
            minLatitude = 39.628632488021879,
            maxLatitude = 39.628632488021879,
            minLongitude = -104.90231457859423,
            maxLongitude = -104.90231457859423,
            latitudePerPixel = 0.000058806721412885429f,
            longitudePerPixel = 0.000085830109961996306f,
            zoom = 14.0f,
            precise = true
        )
        Assert.assertNotNull(items)
        Assert.assertEquals(items!!.size, 1)
        /**
         * let localDataSource = ObservationLocationStaticLocalDataSource()
         *         let localIconDataSource = ObservationIconStaticLocalDataSource()
         *         let iconRepository = ObservationIconRepository(localDataSource: localIconDataSource)
         *         let tileRepository = ObservationsTileRepository(
         *             localDataSource: localDataSource,
         *             observationIconRepository: iconRepository
         *         )
         *
         *         localDataSource.list.append(ObservationMapItem(
         *             observationId: URL(string: "https://test/observationId"),
         *             geometry: SFPoint(xValue: -104.90241, andYValue: 39.62691),
         *             iconPath: OHPathForFile("110.png", type(of: self)),
         *             maxLatitude:  39.62691,
         *             maxLongitude: -104.90241,
         *             minLatitude: 39.62691,
         *             minLongitude: -104.90241
         *         ))
         *
         *         let itemKeys = await tileRepository.getItemKeys(
         *             minLatitude: 39.628632488021879,
         *             maxLatitude: 39.628632488021879,
         *             minLongitude: -104.90231457859423,
         *             maxLongitude: -104.90231457859423,
         *             latitudePerPixel: 0.000058806721412885429,
         *             longitudePerPixel: 0.000085830109961996306,
         *             zoom: 14,
         *             precise: true
         *         )
         *         // this should hit one
         *
         *         XCTAssertEqual(itemKeys.count, 1)
         *
         *         // this should not hit
         *         let noItemKeys = await tileRepository.getItemKeys(
         *             minLatitude: 39.627465124235037,
         *             maxLatitude: 39.627465124235037,
         *             minLongitude: -104.90363063984378,
         *             maxLongitude: -104.90363063984378,
         *             latitudePerPixel: 0.000058806721412885429,
         *             longitudePerPixel: 0.000085830109961996306,
         *             zoom: 14,
         *             precise: true
         *         )
         *
         *         XCTAssertEqual(noItemKeys.count, 0)
         */
    }
}