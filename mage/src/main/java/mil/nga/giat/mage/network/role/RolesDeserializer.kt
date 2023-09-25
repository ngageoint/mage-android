package mil.nga.giat.mage.network.role

import android.util.Log
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import mil.nga.giat.mage.sdk.datastore.user.Permission
import mil.nga.giat.mage.database.model.permission.Role
import mil.nga.giat.mage.sdk.datastore.user.Permissions
import java.lang.reflect.Type

class RolesDeserializer : JsonDeserializer<List<Role>> {

   @Throws(JsonParseException::class)
   override fun deserialize(
      json: JsonElement,
      typeOfT: Type,
      context: JsonDeserializationContext
   ): List<Role> {
      val roles = mutableListOf<Role>()
      val jsonRoles = json.asJsonArray
      for (element in jsonRoles) {
         val role = deserializeRole(element)
         roles.add(role)
      }
      return roles
   }

   private fun deserializeRole(json: JsonElement): Role {
      val jsonRole = json.asJsonObject
      val remoteId = jsonRole["id"].asString
      val name = jsonRole["name"].asString
      val description = jsonRole["description"].asString
      val permissions = mutableListOf<Permission>()

      jsonRole["permissions"].asJsonArray.forEach { element ->
         element?.asString?.let { jsonPermission ->
            try {
               val permission = Permission.valueOf(jsonPermission.uppercase())
               permissions.add(permission)
            } catch (iae: IllegalArgumentException) {
               Log.e(LOG_NAME, "Could not find matching permission, $jsonPermission, for user.")
            }
         }
      }

      return Role(remoteId, name, description, Permissions(permissions))
   }

   companion object {
      private val LOG_NAME = RolesDeserializer::class.java.name
   }
}