package mil.nga.giat.mage.map.navigation.bearing

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableField
import androidx.preference.PreferenceManager
import mil.nga.giat.mage.R
import mil.nga.giat.mage.databinding.ViewCompassBinding
import mil.nga.giat.mage.databinding.ViewCompassMarkerBinding

class CompassData {
    var currentHeading: ObservableField<Double> = ObservableField()
    var targetBearing: ObservableField<Double> = ObservableField()
}

class CompassMarkerData {
    var degreeLabel: ObservableField<String> = ObservableField()
    var bearing: ObservableField<Double> = ObservableField()
    var currentHeading: ObservableField<Double> = ObservableField()
}
@BindingAdapter("android:degreeLabel")
fun CompassMarkerView.bindDegreeLabel(degreeLabel: String) {
    binding.data?.degreeLabel?.set(degreeLabel)
}

@BindingAdapter("android:bearing")
fun CompassMarkerView.bindBearing(bearing: Double) {
    binding.data?.bearing?.set(bearing)
    val height: Int = when {
        bearing % 90.0 == 0.0 -> 25
        bearing % 30 == 0.0 -> 18
        bearing % 10.0 == 0.0 -> 12
        else -> 8
    }

    var width = 5
    if (bearing == 0.0) width = 7

    binding.capsule.layoutParams = binding.capsule.layoutParams.apply {
        this.height = height
        this.width = width
    }
}

@BindingAdapter("android:currentHeading")
fun CompassMarkerView.bindCurrentHeading(currentHeading: Double) {
    var start = (binding.data?.currentHeading?.get()?.toFloat() ?: 0.0f) - (binding.data?.bearing?.get()?.toFloat() ?: 0.0f)
    var end = currentHeading.toFloat() - (binding.data?.bearing?.get()?.toFloat() ?: 0.0f)
    // this means we are rotating from just west of north, to just east of north
    if (start - end > 180) {
        end += 360.0f
    }
    // this means we are rotating from just east of north, to just west of north
    else if (end - start > 180) {
        end -= 360.0f
    }
    val valueAnimator = ValueAnimator.ofFloat(start, end)
    valueAnimator.addUpdateListener {
        val value = it.animatedValue as Float
        binding.degreeLabel.rotation = value
        binding.degreeText.rotation = value
    }
    binding.data?.currentHeading?.set(currentHeading)

    valueAnimator.interpolator = AccelerateDecelerateInterpolator()
    valueAnimator.duration = 500
    valueAnimator.start()
}

open class CompassMarkerView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyle, defStyleRes) {
    val binding: ViewCompassMarkerBinding =
            ViewCompassMarkerBinding.inflate(LayoutInflater.from(context), this, true)
    init {
        binding.data = CompassMarkerData()
        binding.capsule.setBackgroundColor(Color.GRAY)
    }
}

@BindingAdapter("android:bearing")
fun CompassTargetView.bindBearing(bearing: Double) {
    binding.data?.bearing?.set(bearing)
    binding.capsule.setBackgroundColor(relativeBearingColor)
    binding.capsule.layoutParams = binding.capsule.layoutParams.apply {
        height = 50
        width = 20
    }
}

open class CompassTargetView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : CompassMarkerView(context, attrs, defStyle, defStyleRes) {

    val relativeBearingColor: Int
        get(): Int {
            val hexColor = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.resources.getString(R.string.relativeBearingColorKey), context.resources.getString(
                    R.string.relativeBearingColorDefaultValue))
            return try {
                Color.parseColor(hexColor)
            } catch (ignored: IllegalArgumentException) {
                Color.GREEN;
            }
        }

    init {
        binding.degreeText.visibility = INVISIBLE
        binding.capsule.setBackgroundColor(relativeBearingColor)
    }
}

@BindingAdapter("android:currentHeading")
fun CompassBearingView.bindCurrentHeading(currentHeading: Double) {
    binding.data?.currentHeading?.set(currentHeading)
    binding.data?.bearing?.set(currentHeading)
    binding.capsule.setBackgroundColor(headingColor)
    invalidate()
}

open class CompassBearingView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : CompassMarkerView(context, attrs, defStyle, defStyleRes) {

    val headingColor: Int
        get(): Int {
            val hexColor = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.resources.getString(R.string.headingColorKey), context.resources.getString(
                    R.string.headingColorDefaultValue))
            return try {
                Color.parseColor(hexColor)
            } catch (ignored: IllegalArgumentException) {
                Color.RED;
            }
        }

    init {
        binding.capsule.layoutParams = binding.capsule.layoutParams.apply {
            height = 150
            width = 20
        }
        binding.capsule.setBackgroundColor(headingColor)
        binding.degreeText.visibility = GONE
    }
}

@BindingAdapter("android:targetBearing")
fun CompassView.bindTargetBearing(targetBearing: Double) {
    this.binding.data?.targetBearing?.set(targetBearing)
}

@BindingAdapter("android:currentHeading")
fun CompassView.bindCurrentHeading(currentHeading: Double) {
    var start = 360.0f - (binding.data?.currentHeading?.get()?.toFloat() ?: 0.0f)
    var end = 360.0f - currentHeading.toFloat()
    // this means we are rotating from just west of north, to just east of north
    if (start - end > 180) {
        end += 360.0f
    }
    // this means we are rotating from just east of north, to just west of north
    else if (end - start > 180) {
        end -= 360.0f
    }
    val valueAnimator = ValueAnimator.ofFloat(start, end)
    valueAnimator.addUpdateListener {
        val value = it.animatedValue as Float
        binding.markerContainer.rotation = value
    }
    binding.data?.currentHeading?.set(currentHeading)

    valueAnimator.interpolator = AccelerateDecelerateInterpolator()
    valueAnimator.duration = 500
    valueAnimator.start()
}
class CompassView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyle, defStyleRes) {
    val binding: ViewCompassBinding =
            ViewCompassBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        binding.data = CompassData()
    }
}