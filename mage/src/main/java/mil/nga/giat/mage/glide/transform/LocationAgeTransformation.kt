package mil.nga.giat.mage.glide.transform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.ContextCompat
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import mil.nga.giat.mage.R
import java.nio.charset.Charset
import java.security.MessageDigest

class LocationAgeTransformation(
    private val context: Context,
    private val timestamp: Long?,
) : BitmapTransformation() {
    override fun transform(pool: BitmapPool, toTransform: Bitmap, width: Int, height: Int): Bitmap {
        val dot = createDot(pool, context)

        val combinedWidth = dot.width.coerceAtLeast(toTransform.width)
        val combinedHeight = dot.height / 2 + toTransform.height

        val bitmap = pool.get(combinedWidth, combinedHeight, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        canvas.drawBitmap(dot, (combinedWidth - dot.width) / 2f, (combinedHeight - dot.height).toFloat(), null)
        canvas.drawBitmap(toTransform, (combinedWidth - toTransform.width) / 2f, 0f, null)

        return bitmap
    }

    private fun createDot(pool: BitmapPool, context: Context): Bitmap {
        val color = locationColor()

        val density = context.resources.displayMetrics.density
        val dimension = (DOT_DIMENSION * density).toInt()
        val radius = (DOT_RADIUS * density).toInt()

        val bitmap = pool.get(dimension, dimension, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        val paint = Paint()

        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = color

        canvas.drawCircle(dimension / 2f, dimension / 2f, radius.toFloat(), paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = Color.WHITE

        canvas.drawCircle(dimension / 2f, dimension / 2f, radius.toFloat(), paint)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return bitmap
    }

    fun locationColor(): Int {
        val interval = (System.currentTimeMillis() - (timestamp ?: 0)) / 1000L

        return when {
            interval <= MIN_BOUND_SECONDS -> {
                ContextCompat.getColor(context, R.color.location_circle_fill_min)
            }
            interval <= MAX_BOUND_SECONDS -> {
                ContextCompat.getColor(context, R.color.location_circle_fill_intermediate)
            }
            else -> {
                ContextCompat.getColor(context, R.color.location_circle_fill_max)
            }
        }
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(("$ID.${locationColor()}").toByteArray(Charset.forName("UTF-8")))
    }

    override fun equals(other: Any?): Boolean {
        return if (other is LocationAgeTransformation) {
            locationColor() == other.locationColor()
        } else false
    }

    override fun hashCode(): Int {
        return locationColor().hashCode()
    }

    companion object {
        private const val ID = "mil.nga.mage.LocationAgeTransformation"

        private const val MIN_BOUND_SECONDS = 600
        private const val MAX_BOUND_SECONDS = 1200

        private const val DOT_DIMENSION = 18
        private const val DOT_RADIUS = 8
    }
}
