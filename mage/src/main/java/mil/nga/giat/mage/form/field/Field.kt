package mil.nga.giat.mage.form.field

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import mil.nga.giat.mage.form.FormField

abstract class Field<T: Any> @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyle, defStyleRes) {

    abstract fun bind(formField: FormField<T>)

    open fun validate(enforceRequired: Boolean = true): Boolean {
        return true
    }
}
