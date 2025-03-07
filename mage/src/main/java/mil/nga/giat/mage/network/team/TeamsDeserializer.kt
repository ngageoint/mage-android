package mil.nga.giat.mage.network.team

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.stream.JsonReader
import mil.nga.giat.mage.database.model.team.Team
import mil.nga.giat.mage.network.user.UserWithRoleId
import mil.nga.giat.mage.network.user.UserWithRoleIdTypeAdapter
import java.io.StringReader
import java.lang.reflect.Type

class TeamsDeserializer : JsonDeserializer<Map<Team, List<UserWithRoleId>>> {
   private val userTypeAdapter = UserWithRoleIdTypeAdapter()
   private val teamDeserializer = TeamDeserializer.gsonBuilder

   @Throws(JsonParseException::class)
   override fun deserialize(
      json: JsonElement,
      typeOfT: Type,
      context: JsonDeserializationContext
   ): Map<Team, List<UserWithRoleId>> {
      val teams = mutableMapOf<Team, List<UserWithRoleId>>()
      for (element in json.asJsonArray) {
         val jsonTeam = element.asJsonObject
         val team = teamDeserializer.fromJson(jsonTeam, Team::class.java)
         val users = deserializeUsers(jsonTeam.getAsJsonArray("users"))
         teams[team] = users
      }
      return teams
   }

   private fun deserializeUsers(jsonUsers: JsonArray): List<UserWithRoleId> {
      val users: MutableList<UserWithRoleId> = ArrayList()
      for (userElement in jsonUsers) {
         val jsonUser = userElement.asJsonObject
         try {
            val reader = JsonReader(StringReader(jsonUser.toString()))
            users.add(userTypeAdapter.read(reader))
         } catch (e: Exception) {
            Log.e(LOG_NAME, "Error deserializing user", e)
         }
      }
      return users
   }

   companion object {
      private val LOG_NAME = TeamsDeserializer::class.java.name
   }
}