package mil.nga.giat.mage.compat.server5.login

import android.content.Context
import androidx.preference.PreferenceManager
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import mil.nga.giat.mage.R
import mil.nga.giat.mage.sdk.http.HttpClientManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import javax.inject.Inject

class UserResourceServer5 @Inject constructor(
   @ApplicationContext val context: Context
) {

   interface UserServiceServer5 {
      @POST("/api/users")
      fun createUser(@Body body: JsonObject?): Call<JsonObject>
   }

   @Throws(Exception::class)
    fun create(username: String, displayname: String, email: String, phone: String, password: String, callback: Callback<JsonObject>) {

      val baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue))
      val retrofit = Retrofit.Builder()
         .baseUrl(baseUrl)
         .addConverterFactory(GsonConverterFactory.create())
         .client(HttpClientManager.getInstance().httpClient())
         .build()

      val json = JsonObject()
      json.addProperty("username", username)
      json.addProperty("displayName", displayname)
      json.addProperty("email", email)
      json.addProperty("phone", phone)
      json.addProperty("password", password)
      json.addProperty("passwordconfirm", password)

      val service = retrofit.create(UserServiceServer5::class.java)
      service.createUser(json).enqueue(callback)
   }

}