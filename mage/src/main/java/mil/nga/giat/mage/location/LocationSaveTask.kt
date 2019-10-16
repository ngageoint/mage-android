package mil.nga.giat.mage.location

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.AsyncTask
import android.os.BatteryManager
import android.util.Log
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper
import mil.nga.giat.mage.sdk.datastore.location.LocationProperty
import mil.nga.giat.mage.sdk.datastore.user.User
import mil.nga.giat.mage.sdk.datastore.user.UserHelper
import mil.nga.giat.mage.sdk.exceptions.LocationException
import mil.nga.giat.mage.sdk.exceptions.UserException
import mil.nga.sf.Point
import java.util.*

class LocationSaveTask(val context: Context, private val listener: LocationDatabaseListener) : AsyncTask<Location, Void, Location>() {

    companion object {
        private val LOG_NAME = LocationSaveTask::class.java.name
    }

    interface LocationDatabaseListener {
        fun onSaveComplete(location: Location?)
    }

    private var batteryStatus: Intent?

    init {
        batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun doInBackground(vararg locations: Location): Location? {
        val location = locations.getOrNull(0)
        location?.let {
            saveLocation(it)
        }

        return location
    }

    private fun saveLocation(gpsLocation: Location) {
        Log.v(LOG_NAME, "Saving GPS location to database.")

        if (gpsLocation.time > 0) {
            val locationProperties = ArrayList<LocationProperty>()

            val locationHelper = LocationHelper.getInstance(context)

            // build properties
            locationProperties.add(LocationProperty("accuracy", gpsLocation.accuracy))
            locationProperties.add(LocationProperty("bearing", gpsLocation.bearing))
            locationProperties.add(LocationProperty("speed", gpsLocation.speed))
            locationProperties.add(LocationProperty("provider", gpsLocation.provider))
            locationProperties.add(LocationProperty("altitude", gpsLocation.altitude))

            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            level?.let {
                locationProperties.add(LocationProperty("battery_level", it))
            }

            var user: User? = null
            try {
                user = UserHelper.getInstance(context).readCurrentUser()
            } catch (e: UserException) {
                Log.e(LOG_NAME, "Error reading current user from database", e)
            }

            if (user == null || user.currentEvent == null) {
                Log.e(LOG_NAME, "Not saving location for user: $user in event: ${user?.currentEvent}")
                return
            }

            try {
                val location = mil.nga.giat.mage.sdk.datastore.location.Location(
                        "Feature",
                        user,
                        locationProperties,
                        Point(gpsLocation.longitude, gpsLocation.latitude),
                        Date(gpsLocation.time),
                        user.currentEvent)

                locationHelper.create(location)
            } catch (e: LocationException) {
                Log.e(LOG_NAME, "Error saving GPS location", e)
            }
        }
    }

    override fun onPostExecute(location: Location?) {
        listener.onSaveComplete(location)
    }
}
