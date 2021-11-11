package mil.nga.giat.mage.glide.target

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker

class MarkerTarget(context: Context, private val marker: Marker?, width: Int, height: Int) :
   CustomTarget<Bitmap>(
      TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, width.toFloat(), context.resources.displayMetrics).toInt(),
      TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height.toFloat(), context.resources.displayMetrics).toInt()
   ) {

   override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
      setIcon(resource)
   }

   override fun onLoadStarted(placeholder: Drawable?) {
      super.onLoadStarted(placeholder)

      if (placeholder != null) {
         setIcon(placeholder.toBitmap())
      }
   }

   override fun onLoadFailed(errorDrawable: Drawable?) {
      if (errorDrawable != null) {
         setIcon(errorDrawable.toBitmap())
      }
   }

   override fun onLoadCleared(placeholder: Drawable?) {
   }

   private fun setIcon(resource: Bitmap) {
      if (marker?.tag != null) {  // if tag is null marker has been removed from map
         marker.setIcon(BitmapDescriptorFactory.fromBitmap(resource))
         marker.isVisible = true
      }
   }
}