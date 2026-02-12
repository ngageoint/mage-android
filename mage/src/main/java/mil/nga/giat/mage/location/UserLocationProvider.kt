package mil.nga.giat.mage.location

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.util.Log
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
        private const val LOCATION_STALE_INTERVAL = 1000 * 60 * 2
        private const val LOCATION_ACCURACY_THRESHOLD = 200
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


    //monitor updates to the GPS sensitivity setting in shared preferences and invoke requestLocationUpdates with the latest value
    @OptIn(ExperimentalCoroutinesApi::class)
    val locationUpdates: StateFlow<Location?> =
        gpsSensitivitySetting.flatMapLatest { gpsSensitivity ->
            callbackFlow {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

                try {
                    val lastGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    val lastNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                    //prefer GPS, but fallback to network
                    val bestLastLocation = lastGpsLocation ?: lastNetworkLocation
                    bestLastLocation?.let { trySend(it) }
                } catch (e: SecurityException) {
                    Log.i(LOG_NAME, "Error requesting location updates")
                } catch (e: Exception) {
                    Log.i(LOG_NAME, "Error requesting location updates")
                }

                val locationUpdateListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        trySend(location)
                    }
                }

                val mainLooper = Looper.getMainLooper()

                try {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L,
                        gpsSensitivity, locationUpdateListener, mainLooper)
                } catch (ex: SecurityException) {
                    Log.i(LOG_NAME, "Error requesting network location updates: $ex")
                } catch (ex: Exception) {
                    Log.i(LOG_NAME, "Error requesting network location updates: $ex")
                }

                try {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L,
                        gpsSensitivity, locationUpdateListener, mainLooper)
                } catch (ex: SecurityException) {
                    Log.i(LOG_NAME, "Error requesting GPS location updates: $ex")
                } catch (ex: Exception) {
                    Log.d(LOG_NAME, "Error requesting GPS location updates: $ex")
                }

                awaitClose {
                    locationManager.removeUpdates(locationUpdateListener)
                }
            }
        }.stateIn( scope = appScope,
            started = SharingStarted.WhileSubscribed( 5000L),
            initialValue = null
        )

    val bestLocation: StateFlow<Location?> = locationUpdates
        .scan(null as Location?) { currentBest, newLocation ->
            if (newLocation == null) {
                return@scan null
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
        if (currentBestLocation == null) { // A new location is always better than no location
            return true
        }

        // Check whether the new location fix is newer or older
        val timeDelta = newLocation.time - currentBestLocation.time
        val isSignificantlyNewer = timeDelta > LOCATION_STALE_INTERVAL
        val isSignificantlyOlder = timeDelta < -LOCATION_STALE_INTERVAL
        val isNewer = timeDelta > 0
        if (isSignificantlyNewer) { // If it's been more than two minutes since the current location, use the new location because the user has likely moved
            return true
        } else if (isSignificantlyOlder) {  // If the new location is more than two minutes older, it must be worse
            return false
        }

        // Check whether the new location fix is more or less accurate
        val accuracyDelta = (newLocation.accuracy - currentBestLocation.accuracy).toInt()
        val isLessAccurate = accuracyDelta > 0
        val isMoreAccurate = accuracyDelta < 0
        val isSignificantlyLessAccurate = accuracyDelta > LOCATION_ACCURACY_THRESHOLD

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true
        } else if (isNewer && !isLessAccurate) {
            return true
        } else if (isNewer && !isSignificantlyLessAccurate && isSameGPSProvider(newLocation.provider, currentBestLocation.provider)) {
            return true
        }

        return false
    }

    private fun isSameGPSProvider(provider1: String?, provider2: String?): Boolean {
        return if (provider1 == null) {
            provider2 == null
        } else provider1 == provider2
    }

    private fun getMinimumDistanceChangeForUpdates(): Float {
        return preferences.getInt(context.getString(R.string.gpsSensitivityKey), context.resources.getInteger(R.integer.gpsSensitivityDefaultValue)).toFloat()
    }


}
