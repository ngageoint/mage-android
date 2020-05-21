package mil.nga.giat.mage.location

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LiveData
import mil.nga.giat.mage.R
import mil.nga.giat.mage.dagger.module.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationProvider @Inject
constructor(@ApplicationContext val context: Context, val preferences: SharedPreferences) : LiveData<Location>(), SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        private val LOG_NAME = LocationProvider::class.java.simpleName
    }

    private var locationManager: LocationManager? = null
    private var locationListener: LiveDataLocationListener? = null

    private var minimumDistanceChangeForUpdates: Long = 0

    override fun onActive() {
        super.onActive()

        preferences.registerOnSharedPreferenceChangeListener(this)
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        minimumDistanceChangeForUpdates = getMinimumDistanceChangeForUpdates()

        requestLocationUpdates()

        try {
            var location: Location? = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            if (location == null) {
                location = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }

            location?.let {
                setValue(it)
            }
        } catch (e: SecurityException) {
            Log.e(LOG_NAME, "Could not get users location", e)
        }
    }

    override fun onInactive() {
        super.onInactive()

        removeLocationUpdates()
        preferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key.equals(context.getString(R.string.gpsSensitivityKey), ignoreCase = true)) {
            Log.d(LOG_NAME, "GPS sensitivity changed, distance in meters for change: $minimumDistanceChangeForUpdates")
            minimumDistanceChangeForUpdates = getMinimumDistanceChangeForUpdates()

            // bounce location updates so new distance sensitivity takes effect
            removeLocationUpdates()
            requestLocationUpdates()
        }
    }

    private fun requestLocationUpdates() {
        Log.v(LOG_NAME, "request location updates")

        locationListener = LiveDataLocationListener()

        try {
            locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, minimumDistanceChangeForUpdates.toFloat(), locationListener)
        } catch (ex: java.lang.SecurityException) {
            Log.i(LOG_NAME, "Error requesting location updates", ex)
        } catch (ex: IllegalArgumentException) {
            Log.d(LOG_NAME, "LocationManager.NETWORK_PROVIDER does not exist, " + ex.message)
        }

        try {
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, minimumDistanceChangeForUpdates.toFloat(), locationListener)
        } catch (ex: java.lang.SecurityException) {
            Log.i(LOG_NAME, "Error requesting location updates", ex)
        } catch (ex: IllegalArgumentException) {
            Log.d(LOG_NAME, "LocationManager.GPS_PROVIDER does not exist, " + ex.message)
        }
    }

    private fun removeLocationUpdates() {
        Log.v(LOG_NAME, "Removing location updates.")

        if (locationListener != null) {
            locationManager?.removeUpdates(locationListener)
            locationListener = null
        }
    }

    private fun getMinimumDistanceChangeForUpdates(): Long {
        return preferences.getInt(context.getString(R.string.gpsSensitivityKey), context.resources.getInteger(R.integer.gpsSensitivityDefaultValue)).toLong()
    }

    private inner class LiveDataLocationListener : LocationListener {

        override fun onLocationChanged(location: Location) {
            setValue(location)
        }

        override fun onProviderDisabled(provider: String) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    }
}
