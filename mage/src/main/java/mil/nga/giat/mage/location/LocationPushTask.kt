package mil.nga.giat.mage.location

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import mil.nga.giat.mage.sdk.datastore.DaoStore
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper
import mil.nga.giat.mage.sdk.datastore.user.User
import mil.nga.giat.mage.sdk.datastore.user.UserHelper
import mil.nga.giat.mage.sdk.exceptions.LocationException
import mil.nga.giat.mage.sdk.exceptions.UserException
import mil.nga.giat.mage.sdk.http.resource.LocationResource
import java.sql.SQLException
import java.util.*
class LocationPushTask(val context: Context, private val listener: LocationSyncListener) : AsyncTask<Void, Void, Boolean>() {

    interface LocationSyncListener {
        fun onSyncComplete(status: Boolean)
    }
    companion object {
        private val LOG_NAME = LocationPushTask::class.java.name

        private val LOCATION_PUSH_BATCH_SIZE: Long = 100

        val minNumberOfLocationsToKeep = 40
    }

    override fun doInBackground(vararg voids: Void): Boolean {

        val locationResource = LocationResource(context)
        val locationHelper = LocationHelper.getInstance(context)

        var currentUser: User? = null
        try {
            currentUser = UserHelper.getInstance(context).readCurrentUser()
        } catch (e: UserException) {
            e.printStackTrace()
        }

        var locations = locationHelper.getCurrentUserLocations(LOCATION_PUSH_BATCH_SIZE, false)
        while (locations.isNotEmpty()) {

            // Send locations for the current event
            val event = locations[0].event

            val eventLocations = ArrayList<mil.nga.giat.mage.sdk.datastore.location.Location>()
            for (l in locations) {
                if (event == l.event) {
                    eventLocations.add(l)
                }
            }

            // We've sync-ed locations to the server, lets remove the locations we sync'eds from the database
            if (locationResource.createLocations(event, eventLocations)) {
                Log.d(LOG_NAME, "Pushed " + eventLocations.size + " locations.")

                // Delete location where:
                // the user is current user
                // the remote id is set. (have been sent to server)
                // past the lower n amount!
                try {
                    if (currentUser != null) {
                        val locationDao = DaoStore.getInstance(context).locationDao
                        val queryBuilder = locationDao.queryBuilder()
                        val where = queryBuilder.where().eq("user_id", currentUser.id)
                        where.and().isNotNull("remote_id").and().eq("event_id", event.id)
                        queryBuilder.orderBy("timestamp", false)
                        val pushedLocations = queryBuilder.query()

                        if (pushedLocations.size > minNumberOfLocationsToKeep) {
                            val locationsToDelete = pushedLocations.subList(minNumberOfLocationsToKeep, pushedLocations.size)

                            try {
                                LocationHelper.getInstance(context).delete(locationsToDelete)
                            } catch (e: LocationException) {
                                Log.e(LOG_NAME, "Could not delete locations.", e)
                            }

                        }
                    }
                } catch (e: SQLException) {
                    Log.e(LOG_NAME, "Problem deleting locations.", e)
                }

            } else {
                Log.e(LOG_NAME, "Failed to push locations.")
                return false;
            }

            locations = locationHelper.getCurrentUserLocations(LOCATION_PUSH_BATCH_SIZE, false)
        }

        return true;
    }

    override fun onPostExecute(status: Boolean) {
        listener.onSyncComplete(status)
    }
}
