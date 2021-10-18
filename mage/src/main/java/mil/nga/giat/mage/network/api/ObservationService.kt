package mil.nga.giat.mage.network.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ObservationService {

    @GET("/api/events/{eventId}/form/icons.zip")
    suspend fun getObservationIcons(@Path("eventId") eventId: String?): Response<ResponseBody>

}