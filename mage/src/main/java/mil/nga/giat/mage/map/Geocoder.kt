package mil.nga.giat.mage.map

import android.app.Application
import android.location.Geocoder
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mil.nga.gars.GARS
import mil.nga.giat.mage.coordinate.CoordinateSystem
import mil.nga.mgrs.MGRS
import java.io.IOException
import java.text.ParseException
import javax.inject.Inject

class Geocoder @Inject constructor(
   private val application: Application
) {
   sealed class SearchResponse {
      data class Success(val result: SearchResult): SearchResponse()
      data class Error(val message: String): SearchResponse()
   }

   data class SearchResult(
      val markerOptions: MarkerOptions,
      val zoom: Int,
   )

   suspend fun search(text: String) = withContext(Dispatchers.IO) {
      if (MGRS.isMGRS(text)) {
         try {
            val point = MGRS.parse(text).toPoint()
            val options = MarkerOptions()
               .position(LatLng(point.latitude, point.longitude))
               .title(CoordinateSystem.MGRS.name)
               .snippet(text)

            SearchResponse.Success(SearchResult(options, 18))
         } catch (ignore: ParseException) {
            SearchResponse.Error("Failed parsing MGRS text.")
         }
      }
      else if (GARS.isGARS(text)) {
         try {
            val point = GARS.parse(text).toPoint()
            val options = MarkerOptions()
               .position(LatLng(point.latitude, point.longitude))
               .title(CoordinateSystem.GARS.name)
               .snippet(text)

            SearchResponse.Success(SearchResult(options, 18))
         } catch (ignore: ParseException) {
            SearchResponse.Error("Failed parsing GARS text.")
         }
      } else {
         val geocoder = Geocoder(application)

         try {
            val addresses = geocoder.getFromLocationName(text, 1)
            addresses?.firstOrNull()?.let { address ->
               val addressLines = address.maxAddressLineIndex + 1

               val markerOptions = MarkerOptions()
                  .position(LatLng(address.latitude, address.longitude))
                  .title(text)
                  .snippet(address.getAddressLine(0))

               val zoom = MAX_ADDRESS_ZOOM - (MAX_ADDRESS_LINES - addressLines) * 2

               SearchResponse.Success(SearchResult(markerOptions, zoom))
            } ?: SearchResponse.Error("Address or location not found.")
         } catch (e: IOException) {
            Log.e(LOG_NAME, "Problem executing search.", e)
            SearchResponse.Error("Address not found, please check network connectivity.")
         }
      }
   }

   companion object {
      private val LOG_NAME = Geocoder::class.java.name

      private const val MAX_ADDRESS_LINES = 3
      private const val MAX_ADDRESS_ZOOM = 18
   }
}