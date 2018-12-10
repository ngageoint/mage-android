package mil.nga.giat.mage.form.field

import android.content.Context
import android.databinding.BindingAdapter
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import kotlinx.android.synthetic.main.view_form_edit_date.view.*
import mil.nga.giat.mage.databinding.ViewFormDateBinding
import mil.nga.giat.mage.databinding.ViewFormEditDateBinding
import mil.nga.giat.mage.form.FormField
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory
import mil.nga.giat.mage.utils.DateFormatFactory
import java.util.*

@BindingAdapter("date")
fun date(view: TextView, value: Any?) {
    if (value == null) {
        return
    }

    val dateFormat = DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), view.context)

    if (value is String) {
        val date = ISO8601DateFormatFactory.ISO8601().parse(value);
        view.setText(dateFormat.format(date))
    } else if (value is Date) {
        view.setText(dateFormat.format(value))
    }
}

class ViewDate @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : Field<Date>(context, attrs, defStyle, defStyleRes) {

    private val binding: ViewFormDateBinding

    init {
        binding = ViewFormDateBinding.inflate(LayoutInflater.from(context), this, true)
    }

    override fun bind(formField: FormField<Date>) {
        binding.field = formField
    }
}

class EditDate @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : Field<Date>(context, attrs, defStyle, defStyleRes) {

    private var clickListener: ((field: FormField<Date>) -> Unit)? = null
    private var binding: ViewFormEditDateBinding
    private var required = false

    init {
        binding = ViewFormEditDateBinding.inflate(LayoutInflater.from(context), this, true)
    }

    override fun bind(formField: FormField<Date>) {
        binding.field = formField
        binding.clickListener = {
            clickListener?.invoke(formField)
        }

        required = formField.required
    }

    fun setOnEditDateClickListener(clickListener: (field: FormField<Date>) -> Unit) {
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
