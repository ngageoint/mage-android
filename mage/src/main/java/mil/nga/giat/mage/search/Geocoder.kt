package mil.nga.giat.mage.search

import android.app.Application
import android.location.Address
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.LineString
import com.mapbox.geojson.MultiLineString
import com.mapbox.geojson.MultiPoint
import com.mapbox.geojson.MultiPolygon
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mil.nga.gars.GARS
import mil.nga.giat.mage.data.repository.settings.SettingsRepository
import mil.nga.giat.mage.database.model.settings.MapSearchType
import mil.nga.giat.mage.network.geocoder.NominatimService
import mil.nga.mgrs.MGRS
import java.io.IOException
import java.text.ParseException
import javax.inject.Inject

enum class SearchResponseType { MGRS, GARS, GEOCODER }

sealed class SearchResponse {
   data class Success(
      val type: SearchResponseType,
      val results: List<GeocoderResult>
   ): SearchResponse()
   data class Error(val message: String): SearchResponse()
}

data class GeocoderResult(
   val name: String,
   val address: String? = null,
   val location: LatLng
) {
   companion object {
      fun fromAddress(address: Address): GeocoderResult {
         return GeocoderResult(
            name = address.featureName,
            address = address.getAddressLine(0)?.toString(),
            location = LatLng(address.latitude, address.longitude)
         )
      }
   }
}

class Geocoder @Inject constructor(
   private val application: Application,
   private val nominatimService: NominatimService,
   private val settingsRepository: SettingsRepository
) {
   suspend fun search(text: String): SearchResponse {
      val settings = settingsRepository.getMapSettings()

      return if (MGRS.isMGRS(text)) {
         try {
            val point = MGRS.parse(text).toPoint()
            val result = GeocoderResult(name = "MGRS", address = text, location = LatLng(point.latitude, point.longitude))
            SearchResponse.Success(SearchResponseType.MGRS, listOf(result))
         } catch (ignore: ParseException) {
            SearchResponse.Error("Failed parsing MGRS text.")
         }
      } else if (GARS.isGARS(text)) {
         try {
            val point = GARS.parse(text).toPoint()
            val result = GeocoderResult(name = "GARS", address = text, location = LatLng(point.latitude, point.longitude))
            SearchResponse.Success(SearchResponseType.GARS, listOf(result))
         } catch (ignore: ParseException) {
            SearchResponse.Error("Failed parsing GARS text.")
         }
      } else {
         when (settings.searchType) {
            MapSearchType.NATIVE -> {
               NativeGeocoder(application).search(text)
            }

            MapSearchType.NOMINATIM -> {
               settingsRepository.getMapSettings().searchUrl?.let { baseUrl ->
                  NominatimGeocoder(baseUrl, nominatimService).search(text)
               } ?: SearchResponse.Error("Invalid geocoder url")
            }

            else -> {
               SearchResponse.Error("Unsupported search type")
            }
         }
      }
   }
}

class NominatimGeocoder(
   private val baseUrl: String,
   private val nominatimService: NominatimService
) {
   suspend fun search(text: String) = withContext(Dispatchers.IO) {
      val url = "${baseUrl}/search?q=${text}&limit=$RESULT_LIMIT&addressdetails=$ADDRESS_DETAILS&format=$RESULT_FORMAT"

      try {
         val response = nominatimService.search(url)
         if (response.isSuccessful) {
            val features = response.body()?.features()
            if (features?.isNotEmpty() == true) {
               val results = features.mapNotNull { feature ->
                  center(feature.geometry())?.let { centroid ->
                     centroid to feature.properties()
                  }
               }.map { (center, properties) ->
                  GeocoderResult(
                     name = properties?.get("name")?.asString ?: text,
                     address = properties?.get("display_name")?.asString,
                     location = center
                  )
               }

               SearchResponse.Success(
                  type = SearchResponseType.GEOCODER,
                  results = results
               )
            } else {
               SearchResponse.Error("Address or location not found.")
            }
         } else {
            SearchResponse.Error("Error accessing place name server.")
         }
      } catch (e: IOException) {
         SearchResponse.Error("Address not found, please check network connectivity.")
      }
   }

   private fun center(geometry: Geometry?): LatLng? {
      return when (geometry) {
         is Point -> { LatLng(geometry.latitude(), geometry.longitude()) }
         is MultiPoint -> { center(geometry.coordinates()) }
         is LineString -> { center(geometry.coordinates()) }
         is MultiLineString -> { center(geometry.coordinates().flatten()) }
         is Polygon -> { center(geometry.coordinates().first()) }
         is MultiPolygon -> {
            center(geometry.coordinates().map {
               it.first()
            }.flatten())
         }
         else -> null
      }
   }

   private fun center(coordinates: List<Point>): LatLng {
      val builder = LatLngBounds.builder()
      coordinates
         .map { LatLng(it.latitude(), it.longitude()) }
         .forEach { builder.include(it) }

      return builder.build().center
   }

   companion object {
      private const val RESULT_LIMIT = 10
      private const val RESULT_FORMAT = "geojson"
      private const val ADDRESS_DETAILS = 1
   }
}

class NativeGeocoder(
   private val application: Application
) {
   suspend fun search(text: String) = withContext(Dispatchers.IO) {
      val geocoder = android.location.Geocoder(application)

      try {
         val locations = geocoder.getFromLocationName(text, 10)
         if (locations?.isNotEmpty() == true) {
            SearchResponse.Success(
               type = SearchResponseType.GEOCODER,
               results = locations.map { GeocoderResult.fromAddress(it) }
            )
         } else {
            SearchResponse.Error("Address or location not found.")
         }
      } catch (e: IOException) {
         Log.e(LOG_NAME, "Problem executing search.", e)
         SearchResponse.Error("Address not found, please check network connectivity.")
      }
   }

   companion object {
      private val LOG_NAME = NativeGeocoder::class.java.name
   }
}