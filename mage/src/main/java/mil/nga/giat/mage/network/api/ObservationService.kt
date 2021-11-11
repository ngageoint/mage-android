package mil.nga.giat.mage.network.api

import com.google.gson.JsonObject
import mil.nga.giat.mage.sdk.datastore.observation.Observation
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ObservationService {
    @GET("/api/events/{eventId}/observations")
    suspend fun getObservations(
        @Path("eventId") eventId: String,
        @Query("startDate") startDate: String): Response<List<Observation>>

    @GET("/api/events/{eventId}/form/icons.zip")
    suspend fun getObservationIcons(@Path("eventId") eventId: String?): Response<ResponseBody>

    @POST("/api/events/{eventId}/observations/id")
    suspend fun createObservationId(@Path("eventId") eventId: String): Response<Observation>

    @PUT("/api/events/{eventId}/observations/{observationId}")
    suspend fun updateObservation(
        @Path("eventId") eventId: String,
        @Path("observationId") observationId: String,
        @Body observation: Observation): Response<Observation>

    @POST("/api/events/{eventId}/observations/{observationId}/states")
    suspend fun archiveObservation(
        @Path("eventId") eventId: String?,
        @Path("observationId") observationId: String?,
        @Body state: JsonObject?
    ): Response<JsonObject>

    @PUT("/api/events/{eventId}/observations/{observationId}/important")
    suspend fun addImportant(
        @Path("eventId") eventId: String,
        @Path("observationId") observationId: String,
        @Body important: JsonObject
    ): Response<Observation>

    @DELETE("/api/events/{eventId}/observations/{observationId}/important")
    suspend fun removeImportant(
        @Path("eventId") eventId: String,
        @Path("observationId") observationId: String
    ): Response<Observation>

    @PUT("/api/events/{eventId}/observations/{observationId}/favorite")
    suspend fun favoriteObservation(
        @Path("eventId") eventId: String,
        @Path("observationId") observationId: String
    ): Response<Observation>

    @DELETE("/api/events/{eventId}/observations/{observationId}/favorite")
    suspend fun unfavoriteObservation(
        @Path("eventId") eventId: String,
        @Path("observationId") observationId: String
    ): Response<Observation>
}