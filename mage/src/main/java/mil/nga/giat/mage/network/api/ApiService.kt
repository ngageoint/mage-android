package mil.nga.giat.mage.network.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface ApiService {
   @GET
   suspend fun getApi(@Url url: String): Response<ResponseBody>
}