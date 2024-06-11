package mil.nga.giat.mage.ui.map

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.VisibleRegion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.datasource.layer.LayerLocalDataSource
import mil.nga.giat.mage.data.datasource.location.LocationLocalDataSource
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.data.repository.event.EventRepository
import mil.nga.giat.mage.data.repository.location.LocationRepository
import mil.nga.giat.mage.data.repository.map.BottomSheetRepository
import mil.nga.giat.mage.data.repository.map.MapLocation
import mil.nga.giat.mage.data.repository.map.MapRepository
import mil.nga.giat.mage.data.repository.map.ObservationMapImage
import mil.nga.giat.mage.data.repository.map.ObservationsTileRepository
import mil.nga.giat.mage.data.repository.observation.ObservationLocationRepository
import mil.nga.giat.mage.data.repository.observation.ObservationRepository
import mil.nga.giat.mage.database.model.location.Location
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.glide.model.Avatar
import mil.nga.giat.mage.glide.target.MarkerTarget
import mil.nga.giat.mage.glide.transform.LocationAgeTransformation
import mil.nga.giat.mage.location.LocationAccess
import mil.nga.giat.mage.location.LocationPolicy
import mil.nga.giat.mage.map.UserMapState
import mil.nga.giat.mage.map.annotation.MapAnnotation
import mil.nga.giat.mage.search.Geocoder
import mil.nga.giat.mage.ui.map.overlay.DataSourceTileProvider
import mil.nga.giat.mage.utils.DateFormatFactory
import java.text.DateFormat
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

enum class LocationState {
   NotReporting,
   NotEventMember,
   ReportingCoarse,
   ReportingPrecise,
   PermissionDenied
}

enum class LocationPrecision { Precise, Coarse }

sealed class LocationToggleResult {
   class Start(val precision: LocationPrecision): LocationToggleResult()
   data object Stop : LocationToggleResult()
   data object NotEventMember : LocationToggleResult()
   data object PermissionDenied : LocationToggleResult()
}

@HiltViewModel
class MapViewModel @Inject constructor(
   val locationPolicy: LocationPolicy,
   private val preferences: SharedPreferences,
   private val locationAccess: LocationAccess,
   private val application: Application,
   private val geocoder: Geocoder,
   private val eventRepository: EventRepository,
   private val userLocalDataSource: UserLocalDataSource,
   private val layerLocalDataSource: LayerLocalDataSource,
   private val mapRepository: MapRepository,
   private val bottomSheetRepository: BottomSheetRepository
): ViewModel() {
   // TODO use the right formatter here based on preferences
   var dateFormat: DateFormat =
      DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), application)

   val baseMapType = mapRepository.baseMapType.asLiveData()
   val mapLocation = mapRepository.mapLocation

   private val isReportingLocation: Flow<Boolean> = callbackFlow {
      val isReportingLocationKey = application.resources.getString(R.string.reportLocationKey)
      val listener = SharedPreferences.OnSharedPreferenceChangeListener { preferences, key ->
         try {
            if (key === isReportingLocationKey) {
               trySend(preferences.getBoolean(key, false))
            }
         } catch (_: Throwable) { }
      }

      preferences.registerOnSharedPreferenceChangeListener(listener)

      trySend(preferences.getBoolean(isReportingLocationKey, false))

      awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
   }

   private val event = flow {
      val event = eventRepository.getCurrentEvent()
      emit(event)
   }

   private val isEventMember: Flow<Boolean> = flow {
      val isEventMember = userLocalDataSource.isCurrentUserPartOfCurrentEvent()
      emit(isEventMember)
   }

   @OptIn(ExperimentalCoroutinesApi::class)
   val availableLayerDownloads = event.mapLatest { event ->
      layerLocalDataSource.readByEvent(event).any { layer ->
         !layer.isLoaded
      }
   }.asLiveData()

   val locationStatus = combine(
      isEventMember,
      isReportingLocation
   ) { isEventMember: Boolean, isReportingLocation: Boolean ->
      if (!isEventMember) {
         LocationState.NotEventMember
      } else {
         if (locationAccess.isLocationGranted()) {
            if (!isReportingLocation) {
               LocationState.NotReporting
            } else {
               if (locationAccess.isPreciseLocationGranted()) {
                  LocationState.ReportingPrecise
               } else {
                  LocationState.ReportingCoarse
               }
            }
         } else {
            LocationState.PermissionDenied
         }
      }
   }.asLiveData()

   suspend fun toggleReportLocation(): LocationToggleResult = withContext(Dispatchers.IO) {
      if (!isEventMember.last()) {
         LocationToggleResult.NotEventMember
      } else if (!locationAccess.isLocationGranted()) {
         LocationToggleResult.PermissionDenied
      } else {
         val key = application.resources.getString(R.string.reportLocationKey)
         val reportLocation = !preferences.getBoolean(key, false)

         if (locationAccess.isLocationGranted()) {
            preferences.edit().putBoolean(key, reportLocation).apply()

            val precision = if (locationAccess.isPreciseLocationGranted()) LocationPrecision.Precise else LocationPrecision.Coarse
            if (reportLocation) {
               LocationToggleResult.Start(precision)
            } else {
               LocationToggleResult.Stop
            }
         } else {
            LocationToggleResult.PermissionDenied
         }
      }
   }

   private val searchText = MutableStateFlow("")
   fun search(text: String) {
      searchText.value = text
   }

   val searchResponse = searchText
      .map {
         if (it.isNotEmpty()) {
            geocoder.search(it)
         } else null
      }.flowOn(Dispatchers.IO)
      .asLiveData()

   suspend fun setMapLocation(cameraPosition: CameraPosition, visibleRegion: VisibleRegion) {
      mapRepository.setMapLocation(
         MapLocation(
            latitude = cameraPosition.target.latitude,
            longitude = cameraPosition.target.longitude,
            zoom = cameraPosition.zoom,
            visibleRegion = visibleRegion
         )
      )
   }

   suspend fun setTapLocation(point: LatLng, bounds: LatLngBounds, longitudePerPixel: Float, latitudePerPixel: Float, zoom: Float): Int {
      return bottomSheetRepository.setLocation(point, bounds, longitudePerPixel, latitudePerPixel, zoom)
   }

   fun clearBottomSheetItems() {
      bottomSheetRepository.clearAnnotations()
   }
}