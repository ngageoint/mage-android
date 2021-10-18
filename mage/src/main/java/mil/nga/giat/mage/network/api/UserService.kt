package mil.nga.giat.mage.network.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface UserService {

   @GET("/api/users/{userId}/icon")
   suspend fun getIcon(@Path("userId") userId: String?): Response<ResponseBody>

}