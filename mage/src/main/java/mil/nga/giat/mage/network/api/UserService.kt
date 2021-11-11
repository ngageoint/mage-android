package mil.nga.giat.mage.network.api

import mil.nga.giat.mage.sdk.datastore.user.User
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface UserService {

   @GET("/api/users/{userId}/icon")
   suspend fun getIcon(@Path("userId") userId: String?): Response<ResponseBody>

   @POST("/api/users/{userId}/events/{eventId}/recent")
   suspend fun addRecentEvent(@Path("userId") userId: String, @Path("eventId") eventId: String): Response<User>

}