package mil.nga.giat.mage.network.api

import mil.nga.giat.mage.sdk.datastore.location.Location
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface LocationService {

    @GET("/api/events/{eventId}/locations/users")
    suspend fun getLocations(@Path("eventId") eventId: String?): Response<List<Location>>

}