package mil.nga.giat.mage.network.location

import mil.nga.giat.mage.database.model.location.Location
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface LocationService {

    @GET("/api/events/{eventId}/locations/users")
    suspend fun getLocations(@Path("eventId") eventId: String?): Response<List<UserLocations>>

    @POST("/api/events/{eventId}/locations")
    @JvmSuppressWildcards
    suspend fun pushLocations(
        @Path("eventId") eventId: String,
        @Body locations: @JvmSuppressWildcards List<Location>
    ): Response<List<Location>>
}