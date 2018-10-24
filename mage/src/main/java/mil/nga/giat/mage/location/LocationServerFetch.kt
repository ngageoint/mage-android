package mil.nga.giat.mage.location

import android.content.Context
import android.util.Log
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import mil.nga.giat.mage.sdk.datastore.user.User
import mil.nga.giat.mage.sdk.datastore.user.UserHelper
import mil.nga.giat.mage.sdk.exceptions.UserException
import mil.nga.giat.mage.sdk.fetch.UserServerFetch
import mil.nga.giat.mage.sdk.http.resource.LocationResource
import java.util.*

class LocationServerFetch(val context: Context) {

    companion object {
        private val LOG_NAME = mil.nga.giat.mage.location.LocationServerFetch::class.java.name
    }

    private val userHelper: UserHelper = UserHelper.getInstance(context)
    private val userFetch: UserServerFetch = UserServerFetch(context)
    private val locationHelper: LocationHelper = LocationHelper.getInstance(context)
    private val locationResource: LocationResource = LocationResource(context)

    fun fetch() {
        var currentUser: User? = null
        try {
            currentUser = userHelper.readCurrentUser()
        } catch (e: UserException) {
            Log.e(LOG_NAME, "Error reading current user.", e)
        }

        val event = EventHelper.getInstance(context).currentEvent
        try {
            val locations = locationResource.getLocations(event)
            for (location in locations) {
                // make sure that the user exists and is persisted in the local data-store
                var userId: String? = null
                val userIdProperty = location.propertiesMap["userId"]
                if (userIdProperty != null) {
                    userId = userIdProperty.value.toString()
                }
                if (userId != null) {
                    var user: User? = userHelper.read(userId)
                    // TODO : test the timer to make sure users are updated as needed!
                    val sixHoursInMilliseconds = (6 * 60 * 60 * 1000).toLong()
                    if (user == null || Date().after(Date(user.fetchedDate.time + sixHoursInMilliseconds))) {
                        // get any users that were not recognized or expired
                        Log.d(LOG_NAME, "User for location is null or stale, re-pulling")
                        userFetch.fetch(userId)
                        user = userHelper.read(userId)
                    }
                    location.user = user

                    // if there is no existing location, create one
                    val l = locationHelper.read(location.remoteId)
                    if (l == null) {
                        // delete old location and create new one
                        if (user != null) {
                            // don't pull your own locations
                            if (user != currentUser) {
                                userId = user.id.toString()
                                val newLocation = locationHelper.create(location)
                                locationHelper.deleteUserLocations(userId, true, newLocation.event)
                            }
                        } else {
                            Log.w(LOG_NAME, "A location with no user was found and discarded.  User id: $userId")
                        }
                    }
                }
            }
        } catch(e: Exception) {
            Log.e(LOG_NAME, "Failed to fetch user locations from server", e)
        }

    }
}
