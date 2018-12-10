package mil.nga.giat.mage.form

import android.content.Context
import android.preference.PreferenceManager
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import mil.nga.giat.mage.sdk.datastore.user.Event

class FormPreferences(var context: Context, var event: Event, var formId: Long) {

    companion object {
        private const val FORM_PREFERENCE_KEY_TEMPLATE = "EVENT_%d_FORM_%d"

        private val gson = GsonBuilder()
                .registerTypeAdapter(FormField::class.java, FormFieldDeserializer())
                .create()

        fun getPreferenceKey(eventId: Long, formId: Long): String {
            return FORM_PREFERENCE_KEY_TEMPLATE.format(eventId, formId)
        }
    }

    val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun saveDefaults(form: Form) {
        val fields = form.fields.filter { !it.archived }
        val json = gson.toJson(fields, object: TypeToken<List<FormField<Any>>>() {}.type)

        val key = getPreferenceKey(event.id, formId)
        preferences.edit().putString(key, json).apply()
    }

    fun getDefaults(): Map<String, Any?> {
        var formDefaults = emptyMap<String, Any?>()

        val key = getPreferenceKey(event.id, formId)
        preferences.getString(key, null)?.let { defaults ->
            val fields = gson.fromJson<List<FormField<Any>>>(defaults, object : TypeToken<List<FormField<Any>>>() {}.type)
            formDefaults = fields.associateTo(mutableMapOf()) {
                it.name to it.value
            }
        }

        return formDefaults
    }

    fun clearDefaults() {
        val key = getPreferenceKey(event.id, formId)
        preferences.edit().remove(key).apply()
    }
}