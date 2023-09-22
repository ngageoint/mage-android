package mil.nga.giat.mage.compat.server5.login

import android.content.Context
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserResourceServer5 @Inject constructor(
   @ApplicationContext val context: Context,
   val retrofit: Retrofit
) {

   interface UserServiceServer5 {
      @POST("/api/users")
      fun createUser(@Body body: JsonObject?): Call<JsonObject>
   }

   @Throws(Exception::class)
    fun create(username: String, displayName: String, email: String, phone: String, password: String, callback: Callback<JsonObject>) {
      val json = JsonObject()
      json.addProperty("username", username)
      json.addProperty("displayName", displayName)
      json.addProperty("email", email)
      json.addProperty("phone", phone)
      json.addProperty("password", password)
      json.addProperty("passwordconfirm", password)

      val service = retrofit.create(UserServiceServer5::class.java)
      service.createUser(json).enqueue(callback)
   }

}