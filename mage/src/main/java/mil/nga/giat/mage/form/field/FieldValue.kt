package mil.nga.giat.mage.form.field
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import mil.nga.giat.mage.observation.ObservationLocation
import mil.nga.giat.mage.sdk.utils.GeometryUtility
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory
import java.io.Serializable

class Media {
  companion object {
    const val ATTACHMENT_ADD_ACTION = "add"
    const val ATTACHMENT_DELETE_ACTION = "delete"
  }
}

sealed class FieldValue {
  class Attachment(attachments: List<mil.nga.giat.mage.sdk.datastore.observation.Attachment>) : FieldValue() {
    val attachments by mutableStateOf(attachments)
  }

  data class Date(val date: java.util.Date) : FieldValue() {
    companion object {
      fun parseValue(value: Any?): java.util.Date? {
        return when (value) {
          is java.util.Date -> value
          is String -> {
            try {
              ISO8601DateFormatFactory.ISO8601().parse(value)
            } catch(e: Exception) { null }
          }
          else -> null
        }
      }
    }
  }

  data class Boolean(val boolean: kotlin.Boolean) : FieldValue()
  data class Location(val location: ObservationLocation) : FieldValue()
  data class Multi(val choices: List<String>) : FieldValue()
  data class Number(val number: String) : FieldValue()
  data class Text(val text: String) : FieldValue()

  fun serialize(): Serializable {
    return when (this) {
      is Attachment -> attachments as Serializable
      is Boolean -> boolean
      is Date -> date
      is Location ->  GeometryUtility.toGeometryBytes(location.geometry)
      is Multi -> choices as Serializable
      is Number -> {
        val value = number.toDouble()
        if (value % 1.0 == 0.0) {
          value.toInt()
        } else {
          value
        }
      }
      is Text -> text
    }
  }
}