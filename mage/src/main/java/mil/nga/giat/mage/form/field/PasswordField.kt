package mil.nga.giat.mage.form.field

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import kotlinx.android.synthetic.main.view_form_edit_number.view.*
import mil.nga.giat.mage.databinding.ViewFormEditPasswordBinding
import mil.nga.giat.mage.form.FormField
import mil.nga.giat.mage.form.TextFormField

class PasswordField @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : Field<String>(context, attrs, defStyle, defStyleRes) {

    private val binding: ViewFormEditPasswordBinding
    private var required = false

    init {
        binding = ViewFormEditPasswordBinding.inflate(LayoutInflater.from(context), this, true)
    }

    override fun bind(formField: FormField<String>) {
        binding.field = formField as TextFormField
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
