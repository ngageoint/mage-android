package mil.nga.giat.mage.form.field

import android.content.Context
import androidx.databinding.BindingAdapter
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import kotlinx.android.synthetic.main.view_form_edit_multiselect.view.*
import mil.nga.giat.mage.databinding.ViewFormEditMultiselectBinding
import mil.nga.giat.mage.databinding.ViewFormMultiselectBinding
import mil.nga.giat.mage.form.FormField

@BindingAdapter("multiSelectText")
fun mulitSelectText(view: TextView, value: Any?) {
    if (value is Collection<*>) {
        view.setText(value.joinToString(", "))
    } else {
        view.setText("")
    }
}
class ViewMultiselect @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : Field<List<String>>(context, attrs, defStyle, defStyleRes) {

    private val binding: ViewFormMultiselectBinding

    init {
        binding = ViewFormMultiselectBinding.inflate(LayoutInflater.from(context), this, true)
    }

    override fun bind(formField: FormField<List<String>>) {
        binding.field = formField
    }
}

class EditMultiSelect @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : Field<List<String>>(context, attrs, defStyle, defStyleRes) {

    private val binding: ViewFormEditMultiselectBinding
    private var clickListener: ((field: FormField<List<String>>) -> Unit)? = null
    private var required = false

    init {
        binding = ViewFormEditMultiselectBinding.inflate(LayoutInflater.from(context), this, true)
    }

    override fun bind(formField: FormField<List<String>>) {
        binding.field = formField
        binding.clickListener = {
            clickListener?.invoke(formField)
        }

        required = formField.required
    }

    fun setOnEditSelectClickListener(clickListener: (field: FormField<List<String>>) -> Unit) {
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
