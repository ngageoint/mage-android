package mil.nga.giat.mage.observation.sync

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.preference.PreferenceManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log
import mil.nga.giat.mage.LandingActivity
import mil.nga.giat.mage.MageApplication
import mil.nga.giat.mage.R
import mil.nga.giat.mage.sdk.datastore.observation.Observation
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper
import mil.nga.giat.mage.sdk.datastore.observation.State
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import mil.nga.giat.mage.sdk.datastore.user.UserHelper
import mil.nga.giat.mage.sdk.fetch.UserServerFetch
import mil.nga.giat.mage.sdk.http.resource.ObservationResource
import java.util.*

class ObservationServerFetch(var context: Context) {

    companion object {
        private val LOG_NAME = ObservationServerFetch::class.java.simpleName
    }

    private val userHelper: UserHelper = UserHelper.getInstance(context)
    private val observationHelper: ObservationHelper = ObservationHelper.getInstance(context)
    private val observationResource: ObservationResource = ObservationResource(context)

    fun fetch(notify: Boolean) {
        val fetched = mutableListOf<Observation>()

        val event = EventHelper.getInstance(context).currentEvent
        Log.d(LOG_NAME, "The device is currently connected. Attempting to fetch Observations for event " + event.name)

        try {
            val observations = observationResource.getObservations(event)
            Log.d(LOG_NAME, "Fetched " + observations.size + " new observations")
            for (observation in observations) {
                val userId = observation.userId
                if (userId != null) {
                    val user = userHelper.read(userId)
                    // TODO : test the timer to make sure users are updated as needed!
                    val sixHoursInMilliseconds = (6 * 60 * 60 * 1000).toLong()
                    if (user == null || Date().after(Date(user.fetchedDate.time + sixHoursInMilliseconds))) {
                        // get any users that were not recognized or expired
                        Log.d(LOG_NAME, "User for observation is null or stale, re-pulling")
                        UserServerFetch(context).fetch(userId)
                    }
                }

                val oldObservation = observationHelper.read(observation.remoteId)
                if (observation.state == State.ARCHIVE && oldObservation != null) {
                    observationHelper.delete(oldObservation)
                    Log.d(LOG_NAME, "Deleted observation with remote_id " + observation.remoteId)
                } else if (observation.state != State.ARCHIVE && oldObservation == null) {
                    val newObservation = observationHelper.create(observation, false)
                    fetched.add(newObservation)
                    Log.d(LOG_NAME, "Created observation with remote_id " + newObservation.remoteId)
                } else if (observation.state != State.ARCHIVE && oldObservation != null && !oldObservation.isDirty) { // TODO : conflict resolution
                    observation.id = oldObservation.id
                    observationHelper.update(observation)
                    Log.d(LOG_NAME, "Updated observation with remote_id " + observation.remoteId)
                }
            }
        } catch(e: Exception) {
            Log.e(LOG_NAME, "Failed to fetch observations from the server", e)
        }


        if (notify) {
            createNotifications(fetched)
        }
    }

    private fun createNotifications(observations: Collection<Observation>) {
        // are we configured to fire notifications?
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val notificationsEnabled = preferences.getBoolean(context.getString(R.string.notificationsEnabledKey), context.resources.getBoolean(R.bool.notificationsEnabledDefaultValue))
        if (observations.isEmpty() || !notificationsEnabled) {
            return
        }

        val notificationManager = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) {

            val viewIntent = Intent(context, LandingActivity::class.java)
            val viewPendingIntent = PendingIntent.getActivity(context, 0, viewIntent, 0)

            val content = if (observations.size == 1) "New observation was created in ${observations.first().event.name}" else "${observations.size} new observations were created in ${observations.first().event.name}"

            val notificationBuilder = NotificationCompat.Builder(context, MageApplication.MAGE_NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_new_obs)
                    .setContentTitle("New MAGE Observation(s)")
                    .setContentText(content)
                    .setVibrate(longArrayOf(0, 400, 75, 250, 75, 250))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setAutoCancel(true)
                    .setContentIntent(viewPendingIntent)

            notificationManager.notify(MageApplication.MAGE_OBSERVATION_NOTIFICATION_PREFIX, notificationBuilder.build())
        } else {
            val groupNotification = NotificationCompat.Builder(context, MageApplication.MAGE_OBSERVATION_NOTIFICATION_CHANNEL_ID)
                    .setGroupSummary(true)
                    .setContentTitle("New MAGE Observations")
                    .setContentText("Some other text")
                    .setSmallIcon(R.drawable.ic_place_black_24dp)
                    .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                    .setGroup(MageApplication.MAGE_OBSERVATION_NOTIFICATION_GROUP)

            notificationManager.notify(MageApplication.MAGE_OBSERVATION_NOTIFICATION_PREFIX, groupNotification.build())

            observations.forEach { observation ->
                val intent = Intent(context, LandingActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

                val information = mutableListOf<String>()
                observation.primaryField?.value?.let { information.add(it.toString()) }
                observation.secondaryField?.value?.let { information.add(it.toString()) }

                val content = if (information.isNotEmpty()) "${information.joinToString(", ")} was created in ${observation.event.name}" else "Observation was created in ${observation.event.name}"

                val notificationBuilder = NotificationCompat.Builder(context, MageApplication.MAGE_OBSERVATION_NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_place_black_24dp)
                        .setContentTitle("New Observation")
                        .setContentText(content)
                        .setAutoCancel(true)
                        .setGroup(MageApplication.MAGE_OBSERVATION_NOTIFICATION_GROUP)
                        .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                        .setContentIntent(pendingIntent)

                notificationManager.notify(MageApplication.MAGE_OBSERVATION_NOTIFICATION_PREFIX + observation.id.toInt(), notificationBuilder.build())
            }
        }
    }
}