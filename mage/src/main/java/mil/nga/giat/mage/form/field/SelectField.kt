package mil.nga.giat.mage.form.field

import android.content.Context
import androidx.databinding.BindingAdapter
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import kotlinx.android.synthetic.main.view_form_edit_select.view.*
import mil.nga.giat.mage.databinding.ViewFormEditSelectBinding
import mil.nga.giat.mage.form.ChoiceFormField
import mil.nga.giat.mage.form.FormField

@BindingAdapter("selectText")
fun selectText(view: TextView, value: Any?) {
    if (value is String) {
        view.text = value
    } else {
        view.setText("")
    }
}

class EditSelect @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : Field<String>(context, attrs, defStyle, defStyleRes) {

    private val binding: ViewFormEditSelectBinding
    private var required = false
    private var clickListener: ((field: FormField<String>) -> Unit)? = null

    init {
        binding = ViewFormEditSelectBinding.inflate(LayoutInflater.from(context), this, true)
    }

    override fun bind(formField: FormField<String>) {
        binding.field = formField as ChoiceFormField<String>
        binding.clickListener = {
            clickListener?.invoke(formField)
        }

        required = formField.required
    }

    fun setOnEditSelectClickListener(clickListener: (field: FormField<String>) -> Unit) {
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
