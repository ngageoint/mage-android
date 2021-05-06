package mil.nga.giat.mage.preferences.color

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.core.graphics.drawable.DrawableCompat
import androidx.preference.DialogPreference

class ColorPickerPreference(context: Context?, attrs: AttributeSet?) : DialogPreference(context, attrs) {
    var defaultColor: String = "#FF0000"
    var color: String
        get(): String {
            return getPersistedString(defaultColor)
        }
        set(value) {
            persistString(value)
            notifyChanged()
            DrawableCompat.setTint(icon, Color.parseColor(value))
        }

    override fun setDefaultValue(defaultValue: Any?) {
        super.setDefaultValue(defaultValue)
        if (defaultValue != null && defaultValue is String)
            defaultColor = defaultValue
        else
            defaultColor = "#FF0000"
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)
        DrawableCompat.setTint(icon, Color.parseColor(color))
    }
}