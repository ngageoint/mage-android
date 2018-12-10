package mil.nga.giat.mage.form.field

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import kotlinx.android.synthetic.main.view_form_edit_radio_group.view.*
import mil.nga.giat.mage.R
import mil.nga.giat.mage.databinding.ViewFormEditRadioBinding
import mil.nga.giat.mage.databinding.ViewFormEditRadioGroupBinding
import mil.nga.giat.mage.form.FormField
import mil.nga.giat.mage.form.SingleChoiceFormField

class RadioField @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : Field<String>(context, attrs, defStyle, defStyleRes)  {

    private val binding: ViewFormEditRadioGroupBinding
    private var required = false

    init {
        binding = ViewFormEditRadioGroupBinding.inflate(LayoutInflater.from(context), this, true)
    }

    override fun bind(formField: FormField<String>) {
        binding.field = formField
        createRadioButtons(formField as SingleChoiceFormField)

        required = formField.required
    }

    private fun createRadioButtons(formField: SingleChoiceFormField) {
        val choices = formField.choices
        val inflater = LayoutInflater.from(context)
        for (choice in choices) {
            val binding = ViewFormEditRadioBinding.inflate(inflater, radioGroup, true)
            binding.field = formField
            binding.choice = choice
        }
    }

    override fun validate(enforceRequired: Boolean): Boolean {
        if (enforceRequired && required && radioGroup.checkedRadioButtonId == -1) {
            textInputLayout.error  = "Required, cannot be blank"
            title.setTextAppearance(context, R.style.TextAppearance_AppCompat_Caption_Label_Error)

            return false
        } else {
            textInputLayout.isErrorEnabled = false
            title.setTextAppearance(context, R.style.TextAppearance_AppCompat_Caption_Label)
        }

        return true
    }
}
