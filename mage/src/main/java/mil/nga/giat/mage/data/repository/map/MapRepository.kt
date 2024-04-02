package mil.nga.giat.mage.data.repository.map

import android.app.Application
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MapType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import mil.nga.giat.mage.R
import javax.inject.Inject

data class MapLocation(
   val latitude: Double,
   val longitude: Double,
   val zoom: Float
) {
   companion object {
      fun parseXYZ(xyz: String): MapLocation {
         val values = xyz.split(",")
         return MapLocation(
            latitude = values.getOrNull(1)?.toDoubleOrNull() ?: 0.0,
            longitude = values.getOrNull(0)?.toDoubleOrNull() ?: 0.0,
            zoom = values.getOrNull(2)?.toFloatOrNull() ?: 1f
         )
      }
   }

   fun formatXYZ(): String {
      return "${longitude},${latitude},${zoom}"
   }
}

class MapRepository @Inject constructor(
   val application: Application,
   val sharedPreferences: SharedPreferences
) {
   val baseMapType: Flow<MapType> = callbackFlow {
      val listener = OnSharedPreferenceChangeListener { preferences, key ->
         try {
            if (key === application.getString(R.string.baseLayerKey)) {
               trySend(getMapType(preferences))
            }
         } catch (_: Throwable) { }
      }

      sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

      trySend(getMapType(sharedPreferences))

      awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
   }

   private fun getMapType(preferences: SharedPreferences): MapType {
      return when (preferences.getInt(application.getString(R.string.baseLayerKey), GoogleMap.MAP_TYPE_NORMAL)) {
         GoogleMap.MAP_TYPE_NORMAL -> MapType.NORMAL
         GoogleMap.MAP_TYPE_SATELLITE -> MapType.SATELLITE
         GoogleMap.MAP_TYPE_TERRAIN -> MapType.TERRAIN
         GoogleMap.MAP_TYPE_HYBRID -> MapType.HYBRID
         else -> MapType.NONE
      }
   }

   val mapLocation: Flow<MapLocation> = callbackFlow {
      val recentMapXYZKey = application.getString(R.string.recentMapXYZKey)
      val recentMapXYZDefault = application.getString(R.string.recentMapXYZDefaultValue)
      val listener = OnSharedPreferenceChangeListener { preferences, key ->
         try {
            if (key === recentMapXYZKey) {
               val xyz = preferences.getString(recentMapXYZKey, recentMapXYZDefault)!!
               trySend(MapLocation.parseXYZ(xyz))
            }
         } catch (_: Throwable) { }
      }

      sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

      val xyz = sharedPreferences.getString(recentMapXYZKey, recentMapXYZDefault)!!
      trySend(MapLocation.parseXYZ(xyz))

      awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
   }

   suspend fun setMapLocation(location: MapLocation) = withContext(Dispatchers.IO) {
      val xyz = location.formatXYZ()
      sharedPreferences
         .edit()
         .putString(application.resources.getString(R.string.recentMapXYZKey), xyz)
         .apply()
   }
}