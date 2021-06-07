package mil.nga.giat.mage.form.field

import mil.nga.giat.mage.observation.ObservationLocation
import mil.nga.giat.mage.sdk.utils.GeometryUtility
import java.io.Serializable

sealed class FieldValue {
  data class Boolean(val boolean: kotlin.Boolean) : FieldValue()
  data class Date(val date: java.util.Date) : FieldValue()
  data class Location(val location: ObservationLocation) : FieldValue()
  data class Multi(val choices: List<String>) : FieldValue()
  data class Number(val number: String) : FieldValue()
  data class Text(val text: String) : FieldValue()

  fun serialize(): Serializable {
    return when (this) {
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