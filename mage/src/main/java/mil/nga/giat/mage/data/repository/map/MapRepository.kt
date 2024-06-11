package mil.nga.giat.mage.data.repository.map

import android.app.Application
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.util.Log
import androidx.compose.runtime.remember
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.VisibleRegion
import com.google.maps.android.compose.MapType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import mil.nga.giat.mage.R
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.feed.item.SnackbarState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

data class MapLocation(
   val latitude: Double,
   val longitude: Double,
   val zoom: Float,
   val visibleRegion: VisibleRegion? = null
) {
   companion object {
      fun parseXYZ(xyz: String, visibleRegion: VisibleRegion? = null): MapLocation {
         val values = xyz.split(",")
         return MapLocation(
            latitude = values.getOrNull(1)?.toDoubleOrNull() ?: 0.0,
            longitude = values.getOrNull(0)?.toDoubleOrNull() ?: 0.0,
            zoom = values.getOrNull(2)?.toFloatOrNull() ?: 1f,
            visibleRegion = visibleRegion
         )
      }
   }

   fun formatXYZ(): String {
      return "${longitude},${latitude},${zoom}"
   }

   override fun equals(other: Any?): Boolean {
      return false
   }

   override fun hashCode(): Int {
      return Random.nextInt()
   }
}

@Singleton
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

   private val _mapLocationWithRegion = MutableStateFlow(MapLocation(0.0, 0.0, 0.0f))
   val mapLocationWithRegion: StateFlow<MapLocation>
      get() = _mapLocationWithRegion.asStateFlow()

   suspend fun setMapLocation(location: MapLocation) = withContext(Dispatchers.IO) {
      Log.d("whatever", "Set the region to $location")
      _mapLocationWithRegion.value = location
      val xyz = location.formatXYZ()
      sharedPreferences
         .edit()
         .putString(application.resources.getString(R.string.recentMapXYZKey), xyz)
         .apply()
   }
}