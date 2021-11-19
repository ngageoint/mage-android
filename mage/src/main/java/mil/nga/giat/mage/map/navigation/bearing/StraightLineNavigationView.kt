package mil.nga.giat.mage.map.navigation.bearing


import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableField
import com.google.android.gms.maps.model.LatLng
import mil.nga.giat.mage.databinding.ViewStraightLineNavigationBinding

class StraightLineNavigationData {
    var heading: ObservableField<Double> = ObservableField()
    var headingColor: ObservableField<Int> = ObservableField()
    var relativeBearing: ObservableField<Double> = ObservableField()
    var bearingColor: ObservableField<Int> = ObservableField()
    var currentLocation: ObservableField<Location> = ObservableField()
    var destinationCoordinate: ObservableField<LatLng> = ObservableField()
    var destinationMarker: Bitmap? = null
    val formattedHeading: ObservableField<String>
        get(): ObservableField<String> {
            return ObservableField(if (heading.get() == null) "" else String.format("%.1f\u00B0", heading.get()))
        }

    val formattedRelativeBearing: ObservableField<String>
        get(): ObservableField<String> {
            relativeBearing.get()?.let {
                if (it < 0.0) {
                    return ObservableField(String.format("%.1f\u00B0", it + 360.0))
                } else if (it > 360.0) {
                    return ObservableField(String.format("%.1f\u00B0", it - 360.0))
                }
                return ObservableField(String.format("%.1f\u00B0", it))
            }
            return ObservableField("")
        }

    val formattedSpeed: ObservableField<String>
        get(): ObservableField<String> {
            return ObservableField(String.format("%.1fkn",
                    (currentLocation.get()?.speed)?.times(1.94384f)
            ))
        }

    val distanceToTarget: ObservableField<String>
        get(): ObservableField<String> {
            val current = currentLocation.get() ?: return ObservableField("")
            val destination = destinationCoordinate.get() ?: return ObservableField("")

            val targetLocation = Location("")
            targetLocation.latitude = destination.latitude
            targetLocation.longitude = destination.longitude
            val metersToDestination = current.distanceTo(targetLocation)
            val nauticalMilesToDestination = metersToDestination / 1852.0
            return ObservableField(String.format("%.1fnmi", nauticalMilesToDestination))
        }
}

class StraightLineNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyle, defStyleRes) {

    var listener: StraightLineNavigationListener? = null
    private val binding: ViewStraightLineNavigationBinding =
            ViewStraightLineNavigationBinding.inflate(LayoutInflater.from(context), this, true)

    fun populate(data: StraightLineNavigationData) {
        binding.data = data

        data.destinationMarker?.let {
            binding.destinationMarkerImage.setImageBitmap(it)
        }

        binding.cancelButton.setOnClickListener {
            listener?.cancelStraightLineNavigation()
        }
    }

    fun rotateDirectionIcon(rotation: Float) {
        binding.destinationDirection.rotation = rotation
    }
}

@BindingAdapter("app:tint")
fun ImageView.setImageTint(@ColorInt color: Int) {
    setColorFilter(color)
}