package mil.nga.giat.mage.login

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.LayerDrawable
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v7.widget.AppCompatButton
import android.util.AttributeSet
import android.util.Base64
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import mil.nga.giat.mage.R
import org.json.JSONException
import org.json.JSONObject

class AuthenticationButton : AppCompatButton {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)


    fun bind(strategy: JSONObject?) {
        configure(strategy)
    }

    private fun configure(strategy: JSONObject?) {
        try {
            if (strategy?.has("title") == true) {
                this.text = String.format("Sign In With %s", strategy.getString("title"))
            }

            if (strategy?.has("textColor") == true) {
                this.setTextColor(Color.parseColor(strategy.getString("textColor")))
            }

            if (strategy?.has("buttonColor") == true) {
                val csl = ColorStateList(arrayOf(intArrayOf()), intArrayOf(Color.parseColor(strategy.getString("buttonColor"))))
                supportBackgroundTintList = csl
            }

            if (strategy?.has("icon") == true) {
                val decodedString = Base64.decode(strategy.getString("icon"), Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
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
            } else if (strategy?.has("buttonColor") == true) {
                // No icon from server, color the default icon the same color as the button color
                val defaultIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_security_white_18dp)
                val paint = Paint()
                paint.colorFilter = PorterDuffColorFilter(Color.parseColor(strategy.getString("buttonColor")), PorterDuff.Mode.SRC_IN)
                val coloredIcon = Bitmap.createBitmap(defaultIcon.width, defaultIcon.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(coloredIcon)
                canvas.drawBitmap(defaultIcon, 0f, 0f, paint)

                val drawable = RoundedBitmapDrawableFactory.create(resources, coloredIcon)
                drawable.gravity = Gravity.CENTER

                val ld = ContextCompat.getDrawable(context, R.drawable.authentication_remote_icon)!!.mutate() as LayerDrawable
                ld.setDrawableByLayerId(R.id.icon, drawable)
                this.setCompoundDrawablesWithIntrinsicBounds(ld, null, null, null)
            }

        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
}