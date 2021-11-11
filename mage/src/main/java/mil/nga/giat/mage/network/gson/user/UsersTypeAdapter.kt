package mil.nga.giat.mage.network.gson.user

import android.content.Context
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import mil.nga.giat.mage.sdk.datastore.user.User

class UsersTypeAdapter(val context: Context): TypeAdapter<List<User>>() {
   private val userTypeAdapter = UserTypeAdapter(context)

   override fun write(out: JsonWriter, value: List<User>) {
      throw UnsupportedOperationException()
   }

   override fun read(reader: JsonReader): List<User> {
      val users = mutableListOf<User>()

      if (reader.peek() != JsonToken.BEGIN_ARRAY) {
         reader.skipValue()
         return users
      }

      reader.beginArray()

      while (reader.hasNext()) {
         users.add(userTypeAdapter.read(reader))
      }

      reader.endArray()

      return users
   }
}