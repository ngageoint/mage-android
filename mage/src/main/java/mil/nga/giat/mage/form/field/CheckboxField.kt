package mil.nga.giat.mage.form.field

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import mil.nga.giat.mage.databinding.ViewFormCheckboxBinding
import mil.nga.giat.mage.databinding.ViewFormEditCheckboxBinding
import mil.nga.giat.mage.form.FormField

class ViewCheckbox @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : Field<Boolean>(context, attrs, defStyle, defStyleRes) {

    private val binding: ViewFormCheckboxBinding

    init {
        binding = ViewFormCheckboxBinding.inflate(LayoutInflater.from(context), this, true)
    }

    override fun bind(formField: FormField<Boolean>) {
        binding.field = formField
    }
}

class EditCheckbox @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : Field<Boolean>(context, attrs, defStyle, defStyleRes)  {
    private val binding: ViewFormEditCheckboxBinding

    init {
        binding = ViewFormEditCheckboxBinding.inflate(LayoutInflater.from(context), this, true)
    }

    override fun bind(formField: FormField<Boolean>) {
        binding.field = formField
    }
}
