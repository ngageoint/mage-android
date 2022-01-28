package mil.nga.giat.mage.glide.transform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import mil.nga.giat.mage.R
import java.nio.charset.Charset
import java.security.MessageDigest

class VideoOverlayTransformation(private val context: Context) : BitmapTransformation() {

    companion object {
        private const val ID = "mil.nga.mage.PlayBitmapOverlayTransformation"
        private val ID_BYTES = ID.toByteArray(Charset.forName("UTF-8"))
    }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, width: Int, height: Int): Bitmap {
        val bitmap = pool.getDirty(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        canvas.drawBitmap(toTransform, Matrix(), null)

        val minDimension = width.coerceAtMost(height)
        val play = BitmapFactory.decodeResource(context.resources, R.drawable.ic_play_circle_filled_100dp)
        val playScaled = Bitmap.createScaledBitmap(play, minDimension / 2, minDimension / 2, false)
        canvas.drawBitmap(playScaled, ((canvas.width - playScaled.width) / 2).toFloat(), ((canvas.height - playScaled.height) / 2).toFloat(), null)

        return bitmap
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)
    }

    override fun equals(other: Any?): Boolean {
        return other is VideoOverlayTransformation
    }

    override fun hashCode(): Int {
        return ID.hashCode()
    }
}
