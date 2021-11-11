package mil.nga.giat.mage.form.defaults

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import mil.nga.giat.mage.form.Form
import mil.nga.giat.mage.form.FormField
import mil.nga.giat.mage.form.FormFieldDeserializer

class FormPreferences(var context: Context, var eventId: Long, private var formId: Long) {

    companion object {
        private const val FORM_PREFERENCE_KEY_TEMPLATE = "EVENT_%d_FORM_%d"

        private val gson = GsonBuilder()
                .registerTypeAdapter(FormField::class.java, FormFieldDeserializer())
                .create()

        fun getPreferenceKey(eventId: Long, formId: Long): String {
            return FORM_PREFERENCE_KEY_TEMPLATE.format(eventId, formId)
        }
    }

    val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun saveDefaults(form: Form) {
        val json = gson.toJson(form, object: TypeToken<Form>() {}.type)
        val key = getPreferenceKey(eventId, formId)
        preferences.edit().putString(key, json).apply()
    }

    fun getDefaults(): Form? {
        var form: Form? = null

        val key = getPreferenceKey(eventId, formId)
        preferences.getString(key, null)?.let { json ->
            val jsonObject: JsonObject = JsonParser().parse(json).asJsonObject
            form = Form.fromJson(jsonObject)
        }

        return form
    }

    fun clearDefaults() {
        val key = getPreferenceKey(eventId, formId)
        preferences.edit().remove(key).apply()
    }
}