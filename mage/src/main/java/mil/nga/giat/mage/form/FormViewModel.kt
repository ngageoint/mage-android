package mil.nga.giat.mage.form

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import mil.nga.giat.mage.observation.ObservationLocation
import mil.nga.giat.mage.sdk.utils.GeometryUtility
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory
import java.util.*

enum class FormMode {
    VIEW,
    EDIT;
}

class FormViewModel : ViewModel() {

    var formMode = FormMode.VIEW
    private lateinit var fieldMap: Map<String, FormField<Any>>

    private val timestamp = MutableLiveData<FormField<Date>>()
    private val location = MutableLiveData<FormField<ObservationLocation>>()
    private val form = MutableLiveData<Form>()

    fun getTimestamp(): LiveData<FormField<Date>> {
        return timestamp
    }

    fun setTimestamp(timestamp: FormField<Date>) {
        this.timestamp.value = timestamp
    }

    fun getLocation(): LiveData<FormField<ObservationLocation>> {
        return location
    }

    fun setLocation(location: FormField<ObservationLocation>) {
        this.location.value = location
    }

    fun getForm(): LiveData<Form> {
        return form
    }

    fun setForm(form: Form, defaults: Map<String, Any?> = emptyMap()) {
        setUserDefaults(form, defaults)

        form.fields.map { it.name to it }.toMap().let {
            fieldMap = it
        }

        this.form.value = form
    }

    fun getField(key: String): FormField<Any>? {
        return fieldMap[key]
    }

    private fun setUserDefaults(form: Form, defaults: Map<String, Any?>) {
        if (defaults.isEmpty()) return

        form.fields?.let { fields ->
            for (field in fields) {
                setValue(field, defaults.get(field.name))
            }
        }
    }

    private fun setValue(field: FormField<Any>, value: Any?) {
        when (field.type) {
            FieldType.DATE ->
                if (value is String) {
                    field.value = ISO8601DateFormatFactory.ISO8601().parse(value)
                } else if (value is Date) {
                    field.value = value
                }
            FieldType.GEOMETRY ->
                if (value is ByteArray) {
                    field.value = ObservationLocation(ObservationLocation.MANUAL_PROVIDER, GeometryUtility.toGeometry(value))
                }
            else -> field.value = value
        }
    }
}