package mil.nga.giat.mage.network.attachment

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import mil.nga.giat.mage.network.gson.nextLongOrNull
import mil.nga.giat.mage.network.gson.nextStringOrNull
import mil.nga.giat.mage.database.model.observation.Attachment

class AttachmentTypeAdapter: TypeAdapter<Attachment>() {
   override fun write(out: JsonWriter, value: Attachment) {
      throw UnsupportedOperationException()
   }

   override fun read(reader: JsonReader): Attachment {
      val attachment = Attachment()
      attachment.isDirty = false

      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
         reader.skipValue()
         return attachment
      }

      reader.beginObject()

      while(reader.hasNext()) {
         when(reader.nextName()) {
            "id" -> attachment.remoteId = reader.nextString()
            "action" -> attachment.action = reader.nextStringOrNull()
            "observationFormId" -> attachment.observationFormId = reader.nextString()
            "fieldName" -> attachment.fieldName = reader.nextString()
            "contentType" -> attachment.contentType = reader.nextStringOrNull()
            "size" -> attachment.size = reader.nextLongOrNull()
            "name" -> attachment.name = reader.nextStringOrNull()
            "url" -> attachment.url = reader.nextStringOrNull()
            "localPath" -> attachment.localPath = reader.nextStringOrNull()
            "relativePath" -> attachment.remotePath = reader.nextStringOrNull()
            else -> reader.skipValue()
         }
      }

      reader.endObject()

      return attachment
   }
}