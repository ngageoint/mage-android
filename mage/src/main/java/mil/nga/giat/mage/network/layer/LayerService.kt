package mil.nga.giat.mage.network.layer

import com.mapbox.geojson.FeatureCollection
import mil.nga.giat.mage.database.model.layer.Layer
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface LayerService {

    @GET("/api/events/{eventId}/layers")
    suspend fun getLayers(@Path("eventId") eventId: String?,  @Query("type") type: String?): Response<List<Layer>>

    @GET("/api/events/{eventId}/layers/{layerId}/features")
    suspend fun getFeatures(
        @Path("eventId") eventId: String,
        @Path("layerId") layerId: String
    ): Response<FeatureCollection>

    @GET
    suspend fun getFeatureIcon(@Url url: String): Response<ResponseBody>
}