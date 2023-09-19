package mil.nga.giat.mage.network.user

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import mil.nga.giat.mage.database.model.user.Phone
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.network.gson.nextStringOrNull
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory
import org.apache.commons.lang3.StringUtils
import java.io.IOException
import java.text.ParseException
import java.util.*

data class UserWithRoleId(
   var user: User,
   var roleId: String,
)

class UserWithRoleIdTypeAdapter: TypeAdapter<UserWithRoleId>() {

   override fun write(out: JsonWriter, value: UserWithRoleId) {
      throw UnsupportedOperationException()
   }

   override fun read(reader: JsonReader): UserWithRoleId {
      val user = User()
      var roleId = ""

      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
         reader.skipValue()
         return UserWithRoleId(user, roleId)
      }

      reader.beginObject()
      while(reader.hasNext()) {
         when(reader.nextName()) {
            "id" -> user.remoteId = reader.nextString()
            "username" -> user.username = reader.nextString()
            "displayName" -> user.displayName = reader.nextString()
            "email" -> user.email = reader.nextStringOrNull()
            "avatarUrl" -> user.avatarUrl = reader.nextStringOrNull()
            "iconUrl" -> user.iconUrl = reader.nextStringOrNull()
            "phones" -> user.primaryPhone = parsePrimaryPhone(reader)
            "roleId" -> roleId = reader.nextString()
            "recentEventIds" -> user.setRecentEventIds(readRecentEventIds(reader))
            "lastUpdated" -> {
               try {
                  user.lastModified = ISO8601DateFormatFactory.ISO8601().parse(reader.nextString())
               } catch (_: ParseException) { }
            }
            else -> reader.skipValue()
         }
      }

      user.role

      reader.endObject()

      return UserWithRoleId(user, roleId)
   }

   @Throws(IOException::class)
   private fun parsePrimaryPhone(reader: JsonReader): String? {
      if (reader.peek() != JsonToken.BEGIN_ARRAY) {
         reader.skipValue()
         return null
      }

      reader.beginArray()

      var primaryPhone: String? = null
      while(reader.hasNext()) {
         val phone = readPhone(reader)
         if (primaryPhone == null) {
            primaryPhone = phone.number
         }
      }

      reader.endArray()

      return primaryPhone
   }

   @Throws(IOException::class)
   private fun readPhone(reader: JsonReader): Phone {
      val phone = Phone()

      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
         reader.skipValue()
         return phone
      }

      reader.beginObject()

      while(reader.hasNext()) {
         val name = reader.nextName()
         if ("number" == name) {
            phone.number = reader.nextString()
         } else {
            reader.skipValue()
         }
      }

      reader.endObject()

      return phone
   }

   @Throws(IOException::class)
   private fun readRecentEventIds(reader: JsonReader): String {
      if (reader.peek() != JsonToken.BEGIN_ARRAY) {
         reader.skipValue()
         return ""
      }

      val recentEventIds = mutableListOf<String>()

      reader.beginArray()

      while (reader.hasNext()) {
         recentEventIds.add(reader.nextString())
      }

      reader.endArray()

      return StringUtils.join(recentEventIds, ",")
   }
}