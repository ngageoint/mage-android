package mil.nga.giat.mage.network.api

import mil.nga.giat.mage.sdk.datastore.layer.Layer
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface LayerService {

    @GET("/api/events/{eventId}/layers")
    suspend fun getLayers(@Path("eventId") eventId: String?,  @Query("type") type: String?): Response<Collection<Layer>>

}