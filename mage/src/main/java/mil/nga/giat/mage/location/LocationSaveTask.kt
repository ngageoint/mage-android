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
import mil.nga.wkb.geom.Point
import java.util.*

class LocationSaveTask(val context: Context, private val listener: LocationDatabaseListener) : AsyncTask<Location, Void, Location>() {

    companion object {
        private val LOG_NAME = LocationSaveTask::class.java.name
    }

    interface LocationDatabaseListener {
        fun onSaveComplete(location: Location?)
    }

    private var batteryStatus: Intent

    init {
        batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun doInBackground(vararg locations: Location): Location? {
        val location = locations.getOrNull(0)
        location?.let {
            saveLocation(it);
        }

        return location
    }

    private fun saveLocation(location: Location) {
        Log.v(LOG_NAME, "Saving MAGE location to database.")

        if (location.time > 0) {
            val locationProperties = ArrayList<LocationProperty>()

            val locationHelper = LocationHelper.getInstance(context)

            // build properties
            locationProperties.add(LocationProperty("accuracy", location.accuracy))
            locationProperties.add(LocationProperty("bearing", location.bearing))
            locationProperties.add(LocationProperty("speed", location.speed))
            locationProperties.add(LocationProperty("provider", location.provider))
            locationProperties.add(LocationProperty("altitude", location.altitude))

            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            if (level != -1) {
                locationProperties.add(LocationProperty("battery_level", level))
            }

            var currentUser: User? = null
            try {
                currentUser = UserHelper.getInstance(context).readCurrentUser()
            } catch (e: UserException) {
                Log.e(LOG_NAME, "Could not get current User!")
            }

            // build location
            val loc = mil.nga.giat.mage.sdk.datastore.location.Location(
                    "Feature",
                    currentUser,
                    locationProperties,
                    Point(location.longitude, location.latitude),
                    Date(location.time),
                    currentUser!!.currentEvent)

            // save the location
            try {
                locationHelper.create(loc)
            } catch (le: LocationException) {
                Log.e(LOG_NAME, "Unable to save current location locally!", le)
            }

        }
    }

    override fun onPostExecute(location: Location?) {
        listener.onSaveComplete(location)
    }
}
