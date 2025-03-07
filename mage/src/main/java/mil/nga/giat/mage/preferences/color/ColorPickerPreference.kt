package mil.nga.giat.mage.preferences.color

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.util.AttributeSet
import androidx.core.graphics.drawable.DrawableCompat
import androidx.preference.DialogPreference

class ColorPickerPreference(
    context: Context,
    attrs: AttributeSet?
) : DialogPreference(context, attrs) {

    private var defaultValue: String? = null

    var color: String
        get(): String {
            return getPersistedString(defaultValue)
        }
        set(value) {
            persistString(value)
            notifyChanged()
            icon?.let {
                DrawableCompat.setTint(it, Color.parseColor(value))
            }
        }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
        defaultValue = a.getString(index)
        return defaultValue
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)
        icon?.let {
            DrawableCompat.setTint(it, Color.parseColor(color))
        }
    }
}