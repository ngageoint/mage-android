package mil.nga.giat.mage.network.device

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface DeviceService {
   @POST("/auth/token")
   suspend fun authorize(
      @Header("Authorization") authorization: String?,
      @Header("user-agent") userAgent: String?,
      @Body body: JsonObject
   ): Response<JsonObject>
}