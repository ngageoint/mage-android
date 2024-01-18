package mil.nga.giat.mage.data.repository.map

import android.app.Application
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.compose.MapType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import mil.nga.giat.mage.R
import javax.inject.Inject

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
         GoogleMap.MAP_TYPE_SATELLITE -> MapType.NORMAL
         GoogleMap.MAP_TYPE_TERRAIN -> MapType.NORMAL
         GoogleMap.MAP_TYPE_HYBRID -> MapType.NORMAL
         else -> MapType.NONE
      }
   }
}