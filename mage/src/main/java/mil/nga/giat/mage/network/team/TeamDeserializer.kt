package mil.nga.giat.mage.network.team

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import mil.nga.giat.mage.database.model.team.Team
import java.lang.reflect.Type

class TeamDeserializer : JsonDeserializer<Team> {
   @Throws(JsonParseException::class)
   override fun deserialize(
      json: JsonElement,
      typeOfT: Type,
      context: JsonDeserializationContext
   ): Team {
      val jsonTeam = json.asJsonObject
      val remoteId = jsonTeam["id"].asString
      val name = jsonTeam["name"].asString
      var description = ""
      if (jsonTeam.has("description")) {
         description = jsonTeam["description"].toString()
      }
      return Team(remoteId, name, description)
   }

   companion object {
      val gsonBuilder: Gson
         get() {
            val gsonBuilder = GsonBuilder()
            gsonBuilder.registerTypeAdapter(Team::class.java, TeamDeserializer())
            return gsonBuilder.create()
         }
   }
}