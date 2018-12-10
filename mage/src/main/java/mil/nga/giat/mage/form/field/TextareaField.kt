package mil.nga.giat.mage.form.field

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import kotlinx.android.synthetic.main.view_form_edit_textarea.view.*
import mil.nga.giat.mage.databinding.ViewFormEditTextareaBinding
import mil.nga.giat.mage.databinding.ViewFormTextareaBinding
import mil.nga.giat.mage.form.FormField

class EditTextarea @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : Field<String>(context, attrs, defStyle, defStyleRes) {

    private val binding: ViewFormEditTextareaBinding
    private var required = false

    init {
        binding = ViewFormEditTextareaBinding.inflate(LayoutInflater.from(context), this, true)
    }

    override fun bind(formField: FormField<String>) {
        binding.field = formField

        required = formField.required
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
