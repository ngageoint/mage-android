package mil.nga.giat.mage.form.field

import android.content.Context
import android.databinding.InverseMethod
import android.support.design.widget.TextInputEditText
import android.util.AttributeSet
import android.view.LayoutInflater
import kotlinx.android.synthetic.main.view_form_edit_number.view.*
import mil.nga.giat.mage.databinding.ViewFormEditNumberBinding
import mil.nga.giat.mage.databinding.ViewFormNumberBinding
import mil.nga.giat.mage.form.FormField
import mil.nga.giat.mage.form.NumberFormField
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParseException

object NumberConverter {
    @JvmStatic
    @InverseMethod("toDouble")
    fun toString(view: TextInputEditText, oldValue: Number?, value: Number?): String {
        if (value == null) return ""

        val format = numberFormat()
        try {
            val parsed = format.parse(view.text.toString()).toDouble()
            if (parsed == value) {
                return view.text.toString()
            }
        } catch (e: ParseException) {}

        return format.format(value)
    }

    @JvmStatic
    fun toDouble(view: TextInputEditText, oldValue: Number?, value: String): Number? {
        if (value.isEmpty()) return null

        try {
            return numberFormat().parse(value).toDouble()
        } catch (e: ParseException) {
            view.setText("")
            return null
        }
    }

    private fun numberFormat(): NumberFormat {
        val format = NumberFormat.getNumberInstance()
        if (format is DecimalFormat) {
            format.setGroupingUsed(false)
        }

        return format
    }
}

class ViewNumber @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : Field<Number>(context, attrs, defStyle, defStyleRes) {

    private val binding: ViewFormNumberBinding

    init {
        binding = ViewFormNumberBinding.inflate(LayoutInflater.from(context), this, true)
    }

    override fun bind(formField: FormField<Number>) {
        binding.field = formField
    }
}

class EditNumber @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : Field<Number>(context, attrs, defStyle, defStyleRes)  {

    private val binding: ViewFormEditNumberBinding
    private var required = false
    private var minValue: Double? = null
    private var maxValue: Double? = null

    init {
        binding = ViewFormEditNumberBinding.inflate(LayoutInflater.from(context), this, true)
    }

    override fun bind(formField: FormField<Number>) {
        binding.field = formField

        val numberField = (formField as NumberFormField)
        required = numberField.required
        minValue = numberField.min?.toDouble()
        maxValue = numberField.max?.toDouble()
    }

    override fun validate(enforceRequired: Boolean): Boolean {
        if (enforceRequired && required && editText.text.isNullOrBlank()) {
            textInputLayout.error  = "Required, cannot be blank"
            return false
        } else {
            textInputLayout.isErrorEnabled = false
        }

        val value = editText.text.toString()

        if (value.isEmpty()) {
            return true
        }

        try {
            val number = value.toDouble()
            minValue?.let {
                if (number < it) {
                    textInputLayout.error = "Must be greater than $minValue"
                    return false
                }
            }

            maxValue?.let {
                if (number > it) {
                    textInputLayout.error = "Must be less than $maxValue"
                    return false
                }
            }
        } catch (e: Exception) {
            textInputLayout.error = "Value must be a number"
            return false
        }

        return true
    }
}
