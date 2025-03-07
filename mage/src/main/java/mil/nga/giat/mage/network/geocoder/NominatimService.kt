package mil.nga.giat.mage.network.geocoder

import com.mapbox.geojson.FeatureCollection
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface NominatimService {
   @GET
   suspend fun search(@Url url: String): Response<FeatureCollection>
}