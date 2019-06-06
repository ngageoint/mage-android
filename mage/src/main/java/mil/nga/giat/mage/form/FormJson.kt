package mil.nga.giat.mage.form

import android.databinding.BaseObservable
import android.databinding.Bindable
import com.android.databinding.library.baseAdapters.BR
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.*
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import mil.nga.giat.mage.observation.ObservationLocation
import mil.nga.giat.mage.sdk.gson.serializer.GeometrySerializer
import mil.nga.giat.mage.sdk.jackson.deserializer.GeometryDeserializer
import mil.nga.giat.mage.sdk.utils.GeometryUtility
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory
import mil.nga.wkb.geom.Geometry
import java.io.Serializable
import java.lang.reflect.Type
import java.text.ParseException
import java.util.*

class Form(
        @SerializedName("id") val id: Long,
        @SerializedName("name") val name: String,
        @SerializedName("description") val description: String?,
        @SerializedName("archived") val archived: Boolean,
        @SerializedName("fields") val fields: List<FormField<Any>>
) {
    companion object {
        private val gson = GsonBuilder()
                .registerTypeAdapterFactory(ListTypeAdapterFactory())
                .registerTypeAdapter(Date::class.java, DateTypeAdapter())
                .registerTypeAdapter(FormField::class.java, FormFieldDeserializer())
                .create()

        fun fromJson(jsonObject: JsonObject?): Form {
            return gson.fromJson(jsonObject, Form::class.java)
        }
    }
}

open class FormField<T> (
        @SerializedName("id") val id: Long,
        @SerializedName("type") val type: FieldType,
        @SerializedName("name") val name: String,
        @SerializedName("title") val title: String,
        @SerializedName("required") val required: Boolean,
        @SerializedName("archived") val archived: Boolean
): BaseObservable() {

    @Bindable
    @SerializedName("value")
    open var value: T? = null
        set(value) {
            field = value
            notifyPropertyChanged(BR.value)
        }

    open fun serialize(): Serializable? {
        (value as? Serializable)?.let {
            return it
        }

        return null
    }

    open fun hasValue(): Boolean {
        return value != null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other == null || javaClass != other.javaClass) return false

        val rhs = other as FormField<T>
        val check = name.equals(rhs.name) &&
                value?.equals(rhs.value) ?: (rhs.value == null)

        return check
    }

    override fun hashCode(): Int {
        return name.hashCode() + (value?.hashCode() ?: 0)
    }
}

enum class FieldType(val typeClass: Class<out FormField<out Any>>) {
    @SerializedName("date") DATE(DateFormField::class.java),
    @SerializedName("geometry") GEOMETRY(GeometryFormField::class.java),
    @SerializedName("textarea") TEXTAREA(TextFormField::class.java),
    @SerializedName("textfield") TEXTFIELD(TextFormField::class.java),
    @SerializedName("numberfield") NUMBERFIELD(NumberFormField::class.java),
    @SerializedName("email") EMAIL(TextFormField::class.java),
    @SerializedName("password") PASSWORD(TextFormField::class.java),
    @SerializedName("radio") RADIO(SingleChoiceFormField::class.java),
    @SerializedName("checkbox") CHECKBOX(BooleanFormField::class.java),
    @SerializedName("dropdown") DROPDOWN(SingleChoiceFormField::class.java),
    @SerializedName("multiselectdropdown") MULTISELECTDROPDOWN(MultiChoiceFormField::class.java);
}

class TextFormField(
        id: Long,
        type: FieldType,
        name: String,
        title: String,
        required: Boolean,
        archived: Boolean
): FormField<String>(id, type, name, title, required, archived) {
    override fun hasValue(): Boolean {
        return !value.isNullOrEmpty()
    }
}

class BooleanFormField(
        id: Long,
        type: FieldType,
        name: String,
        title: String,
        required: Boolean,
        archived: Boolean
): FormField<Boolean>(id, type, name, title, required, archived)

class NumberFormField(
        id: Long,
        type: FieldType,
        name: String,
        title: String,
        required: Boolean,
        archived: Boolean,
        @SerializedName("min") val min: Number?,
        @SerializedName("max") val max: Number?
): FormField<Number>(id, type, name, title, required, archived)

class DateFormField(
        id: Long,
        type: FieldType,
        name: String,
        title: String,
        required: Boolean,
        archived: Boolean
): FormField<Date>(id, type, name, title, required, archived)

class GeometryFormField(
        id: Long,
        type: FieldType,
        name: String,
        title: String,
        required: Boolean,
        archived: Boolean
): FormField<ObservationLocation>(id, type, name, title, required, archived) {
    override fun serialize(): Serializable? {
        value?.geometry?.let {
            return GeometryUtility.toGeometryBytes(it);
        }

        return null
    }
}

class Choice(
        @SerializedName("id") val id: Int,
        @SerializedName("title") val title: String)

open class ChoiceFormField<T>(
        id: Long,
        type: FieldType,
        name: String,
        title: String,
        required: Boolean,
        archived: Boolean,
        @SerializedName("choices") val choices: List<Choice>
): FormField<T>(id, type, name, title, required, archived)

class SingleChoiceFormField(
        id: Long,
        type: FieldType,
        name: String,
        title: String,
        required: Boolean,
        archived: Boolean,
        choices: List<Choice>
): ChoiceFormField<String>(id, type, name, title, required, archived, choices) {
    override fun hasValue(): Boolean {
        return !value.isNullOrEmpty()
    }
}

class MultiChoiceFormField(
        id: Long,
        type: FieldType,
        name: String,
        title: String,
        required: Boolean,
        archived: Boolean,
        choices: List<Choice>
): ChoiceFormField<List<String>>(id, type, name, title, required, archived, choices) {
    override fun hasValue(): Boolean {
        return !value.isNullOrEmpty()
    }
}

class FormFieldDeserializer : JsonDeserializer<FormField<out Any>>, JsonSerializer<FormField<out Any>> {

    companion object {
        private const val TYPE_FIELD = "type"

        private val gson = GsonBuilder()
                .registerTypeAdapterFactory(ListTypeAdapterFactory())
                .registerTypeAdapter(Date::class.java, DateTypeAdapter())
                .registerTypeAdapter(FormField::class.java, FormFieldDeserializer())
                .registerTypeAdapter(ObservationLocation::class.java, LocationParser())
                .create()
    }

    override fun serialize(src: FormField<out Any>, type: Type, context: JsonSerializationContext): JsonElement {
        return  gson.toJsonTree(src)
    }

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): FormField<out Any>? {
        val type = gson.fromJson<FieldType>(json.asJsonObject.get(TYPE_FIELD), FieldType::class.java)
        if (type == null) {
            return null
        }

        val field = gson.fromJson(json, type.typeClass)
        return if (!field.archived) field else null
    }
}

/**
 * Adapter to filter nulls out of deserialized list.  This prevents null entries in the form field
 * list for type values we do not yet support.
 */
class ListTypeAdapterFactory: TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType != List::class.java) {
            return null
        }

        val delegate = gson.getDelegateAdapter(this, type)
        val listDelegate = gson.getDelegateAdapter(this, type as TypeToken<List<Any?>>)

        return object : TypeAdapter<T>() {
            override fun read(`in`: JsonReader): T {
                return if (`in`.peek() == JsonToken.BEGIN_ARRAY) {
                    return listDelegate.read(`in`).filterNotNull() as T
                } else delegate.read(`in`)
            }

            override fun write(out: JsonWriter?, value: T) {
                delegate.write(out, value)
            }
        }.nullSafe()
    }
}

class DateTypeAdapter: TypeAdapter<Date>() {
    private val dateFormat = ISO8601DateFormatFactory.ISO8601()

    override fun write(out: JsonWriter, value: Date?) {
        if (value == null) {
            out.nullValue()
            return
        }

        val dateString = dateFormat.format(value)
        out.value(dateString)
    }

    override fun read(`in`: JsonReader): Date? {
        if (`in`.peek() === JsonToken.NULL) {
            `in`.nextNull()
            return null
        }

        val json = `in`.nextString()
        try {
            return dateFormat.parse(json)
        } catch (e: ParseException) {
            throw JsonSyntaxException(json, e)
        }
    }
}

class LocationParser: JsonDeserializer<ObservationLocation>, JsonSerializer<ObservationLocation> {
    private val gson = GeometrySerializer.getGsonBuilder()
    private val jsonFactory = JsonFactory()
    private val mapper = ObjectMapper()
    private val geometryDeserializer = GeometryDeserializer()

    init {
        jsonFactory.setCodec(mapper)
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ObservationLocation? {
        var location: ObservationLocation? = null

        val parser = jsonFactory.createParser(json.toString())
        parser.nextToken()
        val geometry = geometryDeserializer.parseGeometry(parser)
        if (geometry != null) {
            location =  ObservationLocation(ObservationLocation.MANUAL_PROVIDER, geometry)
        }

        return location
    }

    override fun serialize(location: ObservationLocation, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement? {
        val geojson = gson.toJsonTree(location.geometry, Geometry::class.java)
        return geojson
    }
}