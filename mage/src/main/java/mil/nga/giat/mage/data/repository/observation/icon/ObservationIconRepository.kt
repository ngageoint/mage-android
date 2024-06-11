package mil.nga.giat.mage.data.repository.observation.icon

import android.content.Context
import android.graphics.BitmapFactory
import android.util.TypedValue
import dagger.hilt.android.qualifiers.ApplicationContext
import mil.nga.giat.mage.data.repository.event.EventRepository
import java.io.File
import java.io.FileInputStream
import java.util.LinkedList
import javax.inject.Inject
import kotlin.math.ceil

class ObservationIconRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventRepository: EventRepository,
) {

    companion object {
        private const val DEFAULT_MARKER_ASSET = "markers/default.png"
    }

    var eventIdToMaxIconSize: MutableMap<Long, Pair<Int, Int>> = mutableMapOf()

    suspend fun getMaximumIconHeightToWidthRatio(): Pair<Int, Int> {
        eventRepository.getCurrentEvent()?.let { event ->
            eventIdToMaxIconSize[event.id]?.let {
                return it
            }
            val size = getMaximumIconHeightToWidthRatio(eventId = event.id)
            eventIdToMaxIconSize[event.id] = size
            return size
        }

        return Pair(0,0)
    }

    suspend fun getMaxHeightAndWidth(zoom: Float): Pair<Int, Int> {
        // icons should be a max of 35 wide
//        val pixelWidthTolerance = 0.3.coerceAtLeast((zoom / 18.0)) * 35
        var pixelWidthTolerance = (zoom.toDouble() / 18.0).coerceIn(0.3, 0.7) * 32

        pixelWidthTolerance = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pixelWidthTolerance.toFloat(), context.resources.displayMetrics).toDouble()
        // if the icon is pixelWidthTolerance wide, the max height is this
        val pixelHeightTolerance = (pixelWidthTolerance / getMaximumIconHeightToWidthRatio().first) * getMaximumIconHeightToWidthRatio().second
        return Pair(ceil(pixelWidthTolerance * getScreenScale()).toInt(), ceil(pixelHeightTolerance * getScreenScale()).toInt())
    }

    private fun getScreenScale(): Float {
        val displayMetrics = context.resources.displayMetrics
        return displayMetrics.density
    }

    fun getMaximumIconHeightToWidthRatio(
        eventId: Long,
    ): Pair<Int, Int> {
        // start with the default marker
        var currentLargest = Pair<Int, Int>(Int.MAX_VALUE, 0)
        val stream = context.assets.open(ObservationIconRepository.DEFAULT_MARKER_ASSET)
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(stream, null, options)
        stream.close()
        currentLargest = Pair(options.outWidth, options.outHeight)

        val path = File(File(File(context.filesDir.absolutePath + EventRepository.OBSERVATION_ICON_PATH), eventId.toString()), "icons")
        val pending = LinkedList<File>()
        pending.add(path)

        while (pending.isNotEmpty()) {
            val currentDirectory = pending.removeFirst()

            // Get the files in the current directory
            val files = currentDirectory.listFiles() ?: continue

            // Print the files in the current directory
            for (file in files) {
                if (file.isFile) {
                    val size = getImageDimensions(file)
                    val heightToWidthRatio = size.second.toFloat() / size.first
                    val currentRatio = currentLargest.second.toFloat() / currentLargest.first
                    if (heightToWidthRatio > currentRatio) {
                        currentLargest = size
                    }
                } else if (file.isDirectory) {
                    pending.add(file)
                }
            }
        }

        return currentLargest
    }

    private fun getImageDimensions(imageFile: File): Pair<Int, Int> {
        // Get the input stream from the content resolver
        val inputStream = FileInputStream(imageFile)

        // Decode the image dimensions from the input stream
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(inputStream, null, options)

        // Close the input stream
        inputStream?.close()

        // Return the image dimensions
        return Pair(options.outWidth, options.outHeight)
    }
}
