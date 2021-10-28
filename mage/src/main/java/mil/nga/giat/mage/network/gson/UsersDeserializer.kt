package mil.nga.giat.mage.network.gson

import android.content.Context
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import mil.nga.giat.mage.sdk.datastore.user.User

class UsersDeserializer(val context: Context): TypeAdapter<List<User>>() {
   private val userDeserializer = UserDeserializer(context)

   override fun write(out: JsonWriter, value: List<User>) {
      throw UnsupportedOperationException()
   }

   override fun read(reader: JsonReader): List<User> {
      val users = mutableListOf<User>()

      if (reader.peek() != JsonToken.BEGIN_ARRAY) {
         return users
      }

      reader.beginArray()

      while (reader.hasNext()) {
         users.add(userDeserializer.read(reader))
      }

      reader.endArray()

      return users
   }

   companion object {
      private val LOG_NAME = UsersDeserializer::class.java.name
   }
}