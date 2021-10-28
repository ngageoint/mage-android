package mil.nga.giat.mage.network.gson

import android.content.Context
import android.util.Log
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import mil.nga.giat.mage.sdk.datastore.user.*
import mil.nga.giat.mage.sdk.exceptions.RoleException
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory
import org.apache.commons.lang3.StringUtils
import java.io.IOException
import java.text.ParseException
import java.util.*

class UserDeserializer(val context: Context): TypeAdapter<User>() {
   private val iso8601Format = ISO8601DateFormatFactory.ISO8601()
   private val roleHelper = RoleHelper.getInstance(context)

   override fun write(out: JsonWriter, value: User) {
      throw UnsupportedOperationException()
   }

   override fun read(reader: JsonReader): User {
      val user = User()

      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
         return user
      }

      reader.beginObject()
      while(reader.hasNext()) {
         when(reader.nextName()) {
            "id" -> user.remoteId = reader.nextString()
            "username" -> user.username = reader.nextString()
            "displayName" -> user.displayName = reader.nextString()
            "email" -> user.email = reader.nextString()
            "avatarUrl" -> user.avatarUrl = reader.nextString()
            "iconUrl" -> user.iconUrl = reader.nextString()
            "phones" -> user.primaryPhone = parsePrimaryPhone(reader)
            "role" -> user.role = readRole(reader)
            "roleId" -> user.role = readRoleId(reader)
            "recentEventIds" -> user.setRecentEventIds(readRecentEventIds(reader))
            "lastUpdated" -> {
               try {
                  user.lastModified = iso8601Format.parse(reader.nextString())
               } catch (e: ParseException) { }
            }
            else -> reader.skipValue()
         }
      }

      reader.endObject()

      return user
   }

   @Throws(IOException::class)
   private fun parsePrimaryPhone(reader: JsonReader): String? {
      if (reader.peek() != JsonToken.BEGIN_ARRAY) {
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
   private fun readRoleId(reader: JsonReader): Role? {
      val roleId = reader.nextString()
      var role: Role? = null
      try {
         role = roleHelper.read(roleId)
      } catch (e: RoleException) {
         Log.e(LOG_NAME, "Could not find matching role for user.")
      }

      return role
   }

   @Throws(IOException::class)
   private fun readRole(reader: JsonReader): Role? {
      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
         return null
      }

      val role = Role()

      reader.beginObject()

      while (reader.hasNext()) {
         when(reader.nextName()) {
            "id" -> role.remoteId = reader.nextString()
            "name" -> role.name = reader.nextString()
            "description" -> role.description = reader.nextString()
            "permissions" -> readPermissions(reader)
            else -> reader.skipValue()
         }
      }

      roleHelper.createOrUpdate(role)

      reader.endObject()

      return role
   }

   @Throws(IOException::class)
   private fun readPermissions(reader: JsonReader): Permissions? {
      val permissions: MutableCollection<Permission> = ArrayList()

      if (reader.peek() != JsonToken.BEGIN_ARRAY) {
         return null
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
      private val LOG_NAME = UserDeserializer::class.java.name
   }
}