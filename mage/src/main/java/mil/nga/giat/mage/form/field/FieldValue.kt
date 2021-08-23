package mil.nga.giat.mage.form.field

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import mil.nga.giat.mage.observation.ObservationLocation
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import mil.nga.giat.mage.sdk.utils.GeometryUtility
import java.io.Serializable

data class Media(
  var action: String? = null
) : Attachment(), Serializable {

  constructor(attachment: Attachment): this() {
    this.id = attachment.id
    this.remoteId = attachment.remoteId
    this.name = attachment.name
    this.url = attachment.url
    this.contentType = attachment.contentType
    this.localPath = attachment.localPath
    this.fieldName = attachment.fieldName
    this.observationFormId = attachment.observationFormId
  }

  override fun hashCode(): Int {
    return super.hashCode()
  }

  override fun equals(other: Any?): Boolean {
    return super.equals(other)
  }

  companion object {
    const val ATTACHMENT_ADD_ACTION = "add"
    const val ATTACHMENT_DELETE_ACTION = "delete"
  }
}

sealed class FieldValue {
  class Attachment(media: List<Media>) : FieldValue() {
    val media by mutableStateOf(media)
  }
  data class Boolean(val boolean: kotlin.Boolean) : FieldValue()
  data class Date(val date: java.util.Date) : FieldValue()
  data class Location(val location: ObservationLocation) : FieldValue()
  data class Multi(val choices: List<String>) : FieldValue()
  data class Number(val number: String) : FieldValue()
  data class Text(val text: String) : FieldValue()

  fun serialize(): Serializable {
    return when (this) {
      is Attachment -> media as Serializable
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