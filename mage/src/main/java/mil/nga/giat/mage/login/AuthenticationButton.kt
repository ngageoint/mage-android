package mil.nga.giat.mage.login

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.LayerDrawable
import android.util.*
import android.view.Gravity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.ViewCompat
import mil.nga.giat.mage.R
import org.json.JSONException
import org.json.JSONObject

class AuthenticationButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = android.R.attr.buttonStyle
) : AppCompatButton(context, attrs, defStyle) {

    fun bind(strategy: JSONObject?) {
        configure(strategy)
    }

    private fun configure(strategy: JSONObject?) {
        try {
            if (strategy?.has("title") == true) {
                this.text = String.format("Sign In With %s", strategy.getString("title"))
            }

            if (strategy?.has("textColor") == true) {
                try {
                    this.setTextColor(Color.parseColor(strategy.getString("textColor")))
                } catch(e: Exception) {
                    Log.e(LOG_NAME, "Failed to parse text color " + strategy.getString("textColor"), e)
                }
            }

            if (strategy?.has("buttonColor") == true) {
                try {
                    val color: Int = Color.parseColor(strategy.getString("buttonColor")) or 0xFF000000.toInt()
                    val csl = ColorStateList(arrayOf(intArrayOf()), intArrayOf(color))
                    ViewCompat.setBackgroundTintList(this, csl)
                } catch(e: Exception) {
                    Log.e(LOG_NAME, "Failed to parse button color " + strategy.getString("buttonColor"), e)
                }
            }

            var bitmap: Bitmap? = null
            if (strategy?.has("icon") == true) {
                val decodedString = Base64.decode(strategy.getString("icon"), Base64.DEFAULT)
                bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            }

            if (bitmap != null) {
                configureIcon(bitmap)
            } else if (strategy?.has("buttonColor") == true) {
                configureDefaultIcon(strategy.getString("buttonColor"))
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun configureIcon(bitmap: Bitmap) {
        val size = 18 * resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT
        val icon = RoundedBitmapDrawableFactory.create(resources, Bitmap.createScaledBitmap(bitmap, size, size, true))
        icon.gravity = Gravity.CENTER

        val ld = ContextCompat.getDrawable(context, R.drawable.authentication_remote_icon)!!.mutate() as LayerDrawable
        ld.setDrawableByLayerId(R.id.icon, icon)

        ld.setLayerInset(1,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -7f, resources.displayMetrics).toInt(),
                0,
                0,
                0)

        this.setCompoundDrawablesWithIntrinsicBounds(ld, null, null, null)
    }

    private fun configureDefaultIcon(buttonColor: String) {
        val defaultIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_security_white_18dp)
        val paint = Paint()
        paint.colorFilter = PorterDuffColorFilter(Color.parseColor(buttonColor), PorterDuff.Mode.SRC_IN)
        val coloredIcon = Bitmap.createBitmap(defaultIcon.width, defaultIcon.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(coloredIcon)
        canvas.drawBitmap(defaultIcon, 0f, 0f, paint)

        val drawable = RoundedBitmapDrawableFactory.create(resources, coloredIcon)
        drawable.gravity = Gravity.CENTER

        val ld = ContextCompat.getDrawable(context, R.drawable.authentication_remote_icon)!!.mutate() as LayerDrawable
        ld.setDrawableByLayerId(R.id.icon, drawable)
        this.setCompoundDrawablesWithIntrinsicBounds(ld, null, null, null)
    }

    companion object {
        private val LOG_NAME = AuthenticationButton::class.java.name
    }
}