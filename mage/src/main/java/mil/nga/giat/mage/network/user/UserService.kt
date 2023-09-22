package mil.nga.giat.mage.network.user

import com.google.gson.JsonObject
import mil.nga.giat.mage.database.model.user.User
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.PartMap
import retrofit2.http.Path

interface UserService {

   @POST("/auth/{strategy}/signin")
   suspend fun signin(
      @Path("strategy") strategy: String,
      @Body parameters: JsonObject): Response<JsonObject>

   @POST("/api/logout")
   fun signout(): Call<Void>

   @POST("/api/users/signups")
   suspend fun signup(@Body body: JsonObject): Response<JsonObject>

   @POST("/api/users/signups/verifications")
   suspend fun signupVerify(
      @Header("Authorization") authorization: String,
      @Body body: JsonObject
   ): Response<JsonObject>

   @GET("/api/users/{userId}")
   suspend fun getUser(@Path("userId") userId: String): Response<UserWithRole>

   @PUT("/api/users/myself/password")
   suspend fun changePassword(@Body body: JsonObject): Response<JsonObject>

   @GET("/api/users/{userId}/icon")
   suspend fun getIcon(@Path("userId") userId: String?): Response<ResponseBody>

   @POST("/api/users/{userId}/events/{eventId}/recent")
   suspend fun addRecentEvent(@Path("userId") userId: String, @Path("eventId") eventId: String): Response<UserWithRoleId>

   @Multipart
   @PUT("/api/users/myself")
   suspend fun createAvatar(@PartMap parts: Map<String, RequestBody>): Response<UserWithRole>
}