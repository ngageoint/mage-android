package mil.nga.giat.mage.form.field

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.view_form_edit_geometry.view.*
import mil.nga.giat.mage.coordinate.CoordinateFormatter
import mil.nga.giat.mage.coordinate.CoordinateSystem
import mil.nga.giat.mage.databinding.ViewFormEditGeometryBinding
import mil.nga.giat.mage.databinding.ViewFormGeometryBinding
import mil.nga.giat.mage.form.FormField
import mil.nga.giat.mage.observation.ObservationLocation

@BindingAdapter("geometryHint")
fun geometryHint(view: TextInputLayout, value: String) {
    val coordinateSystem = CoordinateFormatter(view.context).coordinateSystem
    view.hint = "${value} (${if (coordinateSystem == CoordinateSystem.WGS84) "Lat, Lng" else "MGRS"})"
}

@BindingAdapter("geometryText")
fun geometryText(view: TextView, value: ObservationLocation?) {
    if (value == null) {
        view.text = ""
        return
    }

    view.text = CoordinateFormatter(view.context).format(value.centroidLatLng)
}

class ViewGeometry @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : Field<ObservationLocation>(context, attrs, defStyle, defStyleRes) {

    private val binding: ViewFormGeometryBinding

    init {
        binding = ViewFormGeometryBinding.inflate(LayoutInflater.from(context), this, true)
    }

    override fun bind(formField: FormField<ObservationLocation>) {
        binding.field = formField
    }
}

class EditGeometry @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : Field<ObservationLocation>(context, attrs, defStyle, defStyleRes) {

    private var binding: ViewFormEditGeometryBinding
    private var clickListener: ((field: FormField<ObservationLocation>) -> Unit)? = null
    private var required = false

    init {
        binding = ViewFormEditGeometryBinding.inflate(LayoutInflater.from(context), this, true)
    }

    override fun bind(formField: FormField<ObservationLocation>) {
        binding.field = formField
        binding.clickListener = {
            clickListener?.invoke(formField)
        }

        required = formField.required
    }

    fun setOnEditGeometryClickListener(clickListener: (field: FormField<ObservationLocation>) -> Unit) {
        this.clickListener = clickListener
    }

    override fun validate(enforceRequired: Boolean): Boolean {
        if (enforceRequired && required && editText.text.isNullOrBlank()) {
            textInputLayout.error  = "Required, cannot be blank"
            return false
        } else {
            textInputLayout.isErrorEnabled = false
        }

        return true
    }
}
