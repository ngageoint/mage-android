package mil.nga.giat.mage.network.user

import android.util.Log
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import mil.nga.giat.mage.sdk.datastore.user.Permission
import mil.nga.giat.mage.database.model.permission.Role
import mil.nga.giat.mage.database.model.user.Phone
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.network.gson.nextStringOrNull
import mil.nga.giat.mage.sdk.datastore.user.Permissions
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory
import org.apache.commons.lang3.StringUtils
import java.io.IOException
import java.text.ParseException
import java.util.*

data class UserWithRole(
   var user: User,
   var role: Role,
)

class UserWithRoleTypeAdapter: TypeAdapter<UserWithRole>() {

   override fun write(out: JsonWriter, value: UserWithRole) {
      throw UnsupportedOperationException()
   }

   override fun read(reader: JsonReader): UserWithRole {
      val user = User()
      var role = Role()

      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
         reader.skipValue()
         return UserWithRole(user, role)
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
            "role" -> role = readRole(reader)
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

      return UserWithRole(user, role)
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
   private fun readRole(reader: JsonReader): Role {
      val role = Role()

      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
         reader.skipValue()
         return role
      }

      reader.beginObject()

      while (reader.hasNext()) {
         when(reader.nextName()) {
            "id" -> role.remoteId = reader.nextString()
            "name" -> role.name = reader.nextString()
            "description" -> role.description = reader.nextStringOrNull()
            "permissions" -> role.permissions = readPermissions(reader)
            else -> reader.skipValue()
         }
      }

      reader.endObject()

      return role
   }

   @Throws(IOException::class)
   private fun readPermissions(reader: JsonReader): Permissions {
      val permissions = mutableListOf<Permission>()

      if (reader.peek() != JsonToken.BEGIN_ARRAY) {
         reader.skipValue()
         return Permissions()
      }

      reader.beginArray()

      while (reader.hasNext()) {
         val permission = reader.nextString().uppercase()
         try {
            permissions.add(Permission.valueOf(permission))
         } catch (ignore: IllegalArgumentException) {
            Log.w(LOG_NAME, "Permission $permission ignored")
         }
      }

      reader.endArray()

      return Permissions(permissions)
   }

   @Throws(IOException::class)
   private fun readRecentEventIds(reader: JsonReader): String? {
      if (reader.peek() != JsonToken.BEGIN_ARRAY) {
         reader.skipValue()
         return null
      }

      val recentEventIds: MutableList<String?> = ArrayList()

      reader.beginArray()

      while (reader.hasNext()) {
         recentEventIds.add(reader.nextString())
      }

      reader.endArray()

      return StringUtils.join(recentEventIds, ",")
   }

   companion object {
      private val LOG_NAME = UserWithRoleTypeAdapter::class.java.name
   }
}