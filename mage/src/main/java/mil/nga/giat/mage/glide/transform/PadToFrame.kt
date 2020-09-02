package mil.nga.giat.mage.glide.transform

import android.graphics.Bitmap
import android.graphics.Canvas
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.nio.charset.Charset
import java.security.MessageDigest

class PadToFrame : BitmapTransformation() {
   companion object {
      private const val ID = "mil.nga.mage.PadToFrame"
      private val ID_BYTES = ID.toByteArray(Charset.forName("UTF-8"))
   }

   override fun transform(pool: BitmapPool, toTransform: Bitmap, width: Int, height: Int): Bitmap {
      val toReuse = pool.get(width, height, Bitmap.Config.ARGB_8888)

      val centerX: Float = (width - toTransform.width) * 0.5f
      val centerY: Float = (height - toTransform.height) * 0.5f

      val canvas = Canvas(toReuse)
      canvas.drawBitmap(toTransform, centerX, centerY, null)

      return toReuse
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