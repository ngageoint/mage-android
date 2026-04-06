package mil.nga.giat.mage.location

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import mil.nga.giat.mage.R
import mil.nga.giat.mage.di.ApplicationModule
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserLocationProvider @Inject
constructor(@ApplicationContext val context: Context, @ApplicationModule.ApplicationScope val appScope: CoroutineScope, val preferences: SharedPreferences) {
    companion object {
        private val LOG_NAME = UserLocationProvider::class.java.simpleName
        private const val LOCATION_STALE_INTERVAL_MS = 120000L
        private const val LOCATION_UPDATES_INTERVAL_MS = 2000L
        private const val LOCATION_ACCURACY_THRESHOLD_METERS = 200f
    }

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val gpsSensitivitySetting: Flow<Float> = callbackFlow {
        val sharedPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key.equals(context.getString(R.string.gpsSensitivityKey), ignoreCase = true)) {
                //GPS sensitivity setting changed, emit new value
                trySend(getMinimumDistanceChangeForUpdates())
            }
        }

        preferences.registerOnSharedPreferenceChangeListener(sharedPrefsListener)

        awaitClose {
            preferences.unregisterOnSharedPreferenceChangeListener(sharedPrefsListener)
        }
    }.onStart {
        emit(getMinimumDistanceChangeForUpdates())
    }.conflate()


    //monitor updates to the GPS sensitivity setting in shared preferences and create the LocationRequest using the latest value
    @OptIn(ExperimentalCoroutinesApi::class)
    val locationUpdates: StateFlow<Location?> =
        gpsSensitivitySetting.flatMapLatest { gpsSensitivity ->
            callbackFlow {
                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        for (location in locationResult.locations) {
                            trySend(location)
                        }
                    }
                }

                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            //immediately send the location currently in the Fused Location Provider's cache, if available
                            //subsequent location updates will invoke locationCallback
                            trySend(it)
                        }
                    }

                    //set a preference for "highly accurate" location updates with an interval of 2 seconds
                    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATES_INTERVAL_MS)
                        .apply {
                            setMinUpdateDistanceMeters(gpsSensitivity)

                            //allow updates as fast as 1s if another app is already requesting them
                            setMinUpdateIntervalMillis(1000L)

                            //allows the Fused Location Provider to wait for a more accurate location, even if it means a slight delay
                            setWaitForAccurateLocation(true)
                    }.build()

                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )
                } catch (se: SecurityException) {
                    Log.e(LOG_NAME, "Location permission not granted, cannot request updates", se)
                    //close the flow with an error if location permission is denied
                    close(se)

                } catch (e: Exception) {
                    Log.e(LOG_NAME, "Error obtaining location updates", e)
                }

                awaitClose {
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                }
            }
        }.stateIn( scope = appScope,
            started = SharingStarted.WhileSubscribed( 5000L),
            initialValue = null
        )

    val bestLocation: StateFlow<Location?> = locationUpdates
        .scan(null as Location?) { currentBest, newLocation ->
            if (newLocation == null) {
                return@scan currentBest
            }
            if (isBetterLocation(newLocation, currentBest)) {
                newLocation
            } else {
                currentBest
            }
        }
        .distinctUntilChanged()
        .stateIn(
            scope = appScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    private fun isBetterLocation(newLocation: Location, currentBestLocation: Location?): Boolean {
        if (currentBestLocation == null) {
            //a new location is always better than no location
            return true
        }

        // Check whether the new location fix is newer or older
        val timeDelta = newLocation.time - currentBestLocation.time

        if (timeDelta > LOCATION_STALE_INTERVAL_MS) {
            //if the last best location is two minutes older than the new location, automatically use the new location regardless of accuracy
            return true
        } else if (timeDelta < -LOCATION_STALE_INTERVAL_MS) {
            //if the "new" location is more than two minutes older then the last best location, assume it's worse and discard it
            //this is a possibility because the Android system might lose GPS signal and fall back to a cached location from a Wi-Fi or cellular scan that occurred a few seconds or even a minute ago
            return false
        }

        val isNewer = timeDelta > 0
        val isMoreAccurate = newLocation.accuracy < currentBestLocation.accuracy
        val isNotSignificantlyLessAccurate = newLocation.accuracy <= (currentBestLocation.accuracy + LOCATION_ACCURACY_THRESHOLD_METERS)

        //the location is better if it's more accurate,
        //or if it's newer and doesn't have a large accuracy drop compared to the prior best location
        return isMoreAccurate || (isNewer && isNotSignificantlyLessAccurate)
    }

    private fun getMinimumDistanceChangeForUpdates(): Float {
        return preferences.getInt(context.getString(R.string.gpsSensitivityKey), context.resources.getInteger(R.integer.gpsSensitivityDefaultValue)).toFloat()
    }

}
