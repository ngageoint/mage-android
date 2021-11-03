package mil.nga.giat.mage.data.observation

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mil.nga.giat.mage.LandingActivity
import mil.nga.giat.mage.MageApplication
import mil.nga.giat.mage.R
import mil.nga.giat.mage.form.FieldType
import mil.nga.giat.mage.form.Form
import mil.nga.giat.mage.form.field.Media
import mil.nga.giat.mage.network.api.ObservationService
import mil.nga.giat.mage.sdk.datastore.observation.*
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import mil.nga.giat.mage.sdk.datastore.user.UserHelper
import mil.nga.giat.mage.sdk.fetch.UserServerFetch
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory
import retrofit2.Response
import java.net.HttpURLConnection
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObservationRepository @Inject constructor(
   @ApplicationContext private val context: Context,
   private val observationService: ObservationService
) {

   private val iso8601Format = ISO8601DateFormatFactory.ISO8601()
   private val userHelper: UserHelper = UserHelper.getInstance(context)
   private val observationHelper: ObservationHelper = ObservationHelper.getInstance(context)

   suspend fun create(observation: Observation) = withContext(Dispatchers.IO) {
      var response = observationService.createObservationId(observation.event.remoteId)

      if (response.isSuccessful) {
         val returnedObservation = response.body()
         observation.remoteId = returnedObservation?.remoteId
         observation.url = returnedObservation?.url

         // Got the observation id from the server, lets send the observation
         response = update(observation)
      } else {
         observation.error = parseError(response)
         ObservationHelper.getInstance(context).update(observation)
      }

      response
   }

   suspend fun update(observation: Observation) = withContext(Dispatchers.IO) {
      val response = observationService.updateObservation(observation.event.remoteId, observation.remoteId, observation)

      if (response.isSuccessful) {
         val returnedObservation = response.body()
         returnedObservation?.isDirty = false
         returnedObservation?.id = observation.id
         returnedObservation?.event = observation.event

         // Mark new attachments as dirty and set local path for upload
         for (observationForm in observation.forms) {
            val formDefinition = Form.fromJson(observation.event.formMap[observationForm.formId])
            for (observationProperty in observationForm.properties) {
               val fieldDefinition = formDefinition?.fields?.find { it.name == observationProperty.key }
               if (fieldDefinition?.type == FieldType.ATTACHMENT) {
                  for (attachment in observationProperty.value as List<Attachment>) {
                     if (attachment.action == Media.ATTACHMENT_ADD_ACTION) {
                        val returnedAttachment = returnedObservation?.attachments?.find { returnedAttachment ->
                           attachment.url == null &&
                             attachment.name == returnedAttachment.name &&
                             attachment.fieldName == returnedAttachment.fieldName &&
                             attachment.contentType == returnedAttachment.contentType
                        }

                        if (returnedAttachment != null) {
                           returnedAttachment.localPath = attachment.localPath
                           returnedAttachment.isDirty = true
                        }
                     }
                  }
               }
            }
         }

         ObservationHelper.getInstance(context).update(returnedObservation)
      } else {
         observation.error = parseError(response)
         ObservationHelper.getInstance(context).update(observation)
      }

      response
   }

   suspend fun archive(observation: Observation) = withContext(Dispatchers.IO) {
      val state = JsonObject()
      state.addProperty("name", "archive")

      val response = observationService.archiveObservation(observation.event.remoteId, observation.remoteId, state)
      when {
         response.isSuccessful || response.code() == HttpURLConnection.HTTP_NOT_FOUND -> {
            observationHelper.delete(observation)
         }
         response.code() != HttpURLConnection.HTTP_UNAUTHORIZED -> {
            observation.error = parseError(response)
            ObservationHelper.getInstance(context).update(observation)
         }
      }

      response
   }

   suspend fun updateImportant(observation: Observation) = withContext(Dispatchers.IO) {
      val response = if (observation.important?.isImportant == true) {
         val jsonImportant = JsonObject()
         jsonImportant.addProperty("description", observation.important?.description)
         observationService.addImportant(observation.event.remoteId, observation.remoteId, jsonImportant)
      } else {
         observationService.removeImportant(observation.event.remoteId, observation.remoteId)
      }

      if (response.isSuccessful) {
         val returnedObservation = response.body()
         observation.lastModified = returnedObservation?.lastModified
         observationHelper.updateImportant(observation)
      }

      response
   }

   suspend fun updateFavorite(favorite: ObservationFavorite) = withContext(Dispatchers.IO) {
      val observation = favorite.observation

      val response = if (favorite.isFavorite) {
          observationService.favoriteObservation(observation.event.remoteId, observation.remoteId)
      } else {
          observationService.unfavoriteObservation(observation.event.remoteId, observation.remoteId)
      }

      if (response.isSuccessful) {
         val updatedObservation = response.body()
         observation.lastModified = updatedObservation?.lastModified
         observationHelper.updateFavorite(favorite)
      }

      response
   }

   suspend fun fetch(notify: Boolean) = withContext(Dispatchers.IO) {
      val fetched = mutableListOf<Observation>()

      val event = EventHelper.getInstance(context).currentEvent
      Log.d(LOG_NAME, "Fetch observations for event " + event.name)

      try {
         val lastModifiedDate = observationHelper.getLatestCleanLastModified(context, event)

         val response = observationService.getObservations(event.remoteId, iso8601Format.format(lastModifiedDate))
         if (response.isSuccessful) {
            val observations = response.body()!!.map {
               it.event = event
               it
            }.toMutableList()

            Log.d(LOG_NAME, "Fetched " + observations.size + " new observations")

            val iterator = observations.iterator()
            while(iterator.hasNext()) {
               val observation = iterator.next()

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

               iterator.remove()
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
      val notificationsEnabled = preferences.getBoolean(context.getString(R.string.notificationsEnabledKey), context.resources.getBoolean(
         R.bool.notificationsEnabledDefaultValue))
      if (observations.isEmpty() || !notificationsEnabled) {
         return
      }

      val notificationManager = NotificationManagerCompat.from(context)
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {

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
            observation.primaryFeedField?.value?.let { information.add(it.toString()) }
            observation.secondaryFeedField?.value?.let { information.add(it.toString()) }

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

   private fun parseError(response: Response<out Any>): ObservationError {
      val observationError = ObservationError()
      observationError.statusCode = response.code()
      observationError.description = response.message()
      observationError.message = response.errorBody()?.string()

      try {
         val error = JsonParser.parseString(observationError.message).asJsonObject
         observationError.message = error.get("message")?.asString
      } catch(ignore: Exception) {}

      return observationError
   }

   companion object {
      private val LOG_NAME = ObservationRepository::class.java.simpleName
   }
}