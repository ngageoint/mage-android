package mil.nga.giat.mage.data.repository.observation

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.stmt.QueryBuilder
import com.j256.ormlite.stmt.Where
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import mil.nga.giat.mage.LandingActivity
import mil.nga.giat.mage.MageApplication
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.repository.user.UserRepository
import mil.nga.giat.mage.database.model.observation.Attachment
import mil.nga.giat.mage.database.model.observation.Observation
import mil.nga.giat.mage.database.model.observation.ObservationError
import mil.nga.giat.mage.database.model.observation.ObservationFavorite
import mil.nga.giat.mage.data.datasource.observation.ObservationLocalDataSource
import mil.nga.giat.mage.database.model.observation.State
import mil.nga.giat.mage.filter.DateTimeFilter
import mil.nga.giat.mage.filter.Filter
import mil.nga.giat.mage.form.FieldType
import mil.nga.giat.mage.form.Form
import mil.nga.giat.mage.form.field.Media
import mil.nga.giat.mage.network.observation.ObservationService
import mil.nga.giat.mage.sdk.Temporal
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.database.model.observation.ObservationImportant
import mil.nga.giat.mage.sdk.event.IObservationEventListener
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException
import java.net.HttpURLConnection
import java.util.*
import javax.inject.Inject

class ObservationRepository @Inject constructor(
   @ApplicationContext private val context: Context,
   private val preferences: SharedPreferences,
   private val observationService: ObservationService,
   private val userRepository: UserRepository,
   private val userLocalDataSource: UserLocalDataSource,
   private val eventLocalDataSource: EventLocalDataSource,
   private val observationFavoriteDao: Dao<ObservationFavorite, Long>,
   private val observationImportantDao: Dao<ObservationImportant, Long>,
   private val observationLocalDataSource: ObservationLocalDataSource
) {
   private var refreshTime: Long = 0
   private var refreshJob: Job? = null
   private var oldestObservation: Observation? = null

   fun observeObservation(observationId: Long) = observationLocalDataSource.observeObservation(observationId)

   private val favoriteFilter = object : Filter<Observation> {
      override fun query(): QueryBuilder<ObservationFavorite, Long>? {
         val user = userLocalDataSource.readCurrentUser() ?: return null

         val queryBuilder = observationFavoriteDao.queryBuilder()
         queryBuilder.where()
            .eq("user_id", user.remoteId)
            .and()
            .eq("is_favorite", true)

         return queryBuilder
      }

      override fun passesFilter(observation: Observation): Boolean {
         val user = userLocalDataSource.readCurrentUser()
         return observation.favoritesMap[user?.remoteId]?.isFavorite == true
      }

      override fun and(where: Where<*, Long>) {}
   }

   private val importantFilter = object : Filter<Observation> {
      override fun query(): QueryBuilder<ObservationImportant, Long> {
         val queryBuilder = observationImportantDao.queryBuilder()
         queryBuilder.where().eq("is_important", true)
         return queryBuilder
      }

      override fun passesFilter(obj: Observation) = obj.important?.isImportant == true
      override fun and(where: Where<*, Long>) {}
   }

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
         observationLocalDataSource.update(observation)
      }

      response
   }

   @OptIn(ExperimentalCoroutinesApi::class)
   fun getObservations(): Flow<List<Observation>> = callbackFlow {
      val observationListener = object: IObservationEventListener {
         override fun onObservationCreated(observations: Collection<Observation>, sendUserNotifcations: Boolean) {
            trySend(query(this@callbackFlow))
         }

         override fun onObservationUpdated(observation: Observation) {
            trySend(query(this@callbackFlow))
         }

         override fun onObservationDeleted(observation: Observation) {
            trySend(query(this@callbackFlow))
         }

         override fun onError(error: Throwable) {}
      }
      observationLocalDataSource.addListener(observationListener)

      val observationFilterKey = context.resources.getString(R.string.activeTimeFilterKey)
      val preferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
         if (observationFilterKey == key) {
            trySend(query(this))
         }
      }
      preferences.registerOnSharedPreferenceChangeListener(preferencesListener)

      send(query(this))

      awaitClose {
         observationLocalDataSource.removeListener(observationListener)
         preferences.unregisterOnSharedPreferenceChangeListener(preferencesListener)
      }
   }.flowOn(Dispatchers.IO)

   fun query(observationIds: List<Long>): List<Observation> {
      val event = eventLocalDataSource.currentEvent ?: return emptyList()
      val filters = listOfNotNull(getTemporalFilter(), getImportantFilter(), getFavoriteFilter())
      val observations = observationLocalDataSource.getEventObservations(event, filters)
      return observations.filter { observationIds.contains(it.id) }
   }

   @OptIn(ExperimentalCoroutinesApi::class)
   private fun query(scope: ProducerScope<List<Observation>>): List<Observation> {
      val event = eventLocalDataSource.currentEvent ?: return emptyList()
      val filters = listOfNotNull(getTemporalFilter(), getImportantFilter(), getFavoriteFilter())
      val observations = observationLocalDataSource.getEventObservations(event, filters)

      observations.lastOrNull()?.let { observation ->
         if (oldestObservation == null || oldestObservation?.timestamp?.after(observation.lastModified) == true) {
            oldestObservation = observation

            refreshJob?.cancel()
            refreshJob = scope.launch {
               delay(observation.lastModified.time - refreshTime)
               oldestObservation = null
               scope.trySend(query(scope))
            }
         }
      }

      return observations
   }

   private fun getTemporalFilter(): Filter<Temporal>? {
      var filter: Filter<Temporal>? = null

      val date: Date? = when (getTimeFilterId()) {
         context.resources.getInteger(R.integer.time_filter_last_month) -> {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, -1)
            calendar.time
         }
         context.resources.getInteger(R.integer.time_filter_last_week) -> {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, -7)
            calendar.time
         }
         context.resources.getInteger(R.integer.time_filter_last_24_hours) -> {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.HOUR, -24)
            calendar.time
         }
         context.resources.getInteger(R.integer.time_filter_today) -> {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.time
         }
         context.resources.getInteger(R.integer.time_filter_custom) -> {
            val calendar = Calendar.getInstance()
            val customFilterTimeUnit = getCustomTimeUnit()
            val customTimeNumber = getCustomTimeNumber()
            when (customFilterTimeUnit) {
               "Hours" -> calendar.add(Calendar.HOUR_OF_DAY, -1 * customTimeNumber)
               "Days" -> calendar.add(Calendar.DAY_OF_MONTH, -1 * customTimeNumber)
               "Months" -> calendar.add(Calendar.MONTH, -1 * customTimeNumber)
               else -> calendar.add(Calendar.MINUTE, -1 * customTimeNumber)
            }

            calendar.time
         } else -> null
      }

      if (date != null) {
         filter = DateTimeFilter(date, null, "last_modified")
         refreshTime = date.time
      }

      return filter
   }

   private fun getImportantFilter(): Filter<Observation>? {
      val filter = preferences.getBoolean(context.resources.getString(R.string.activeImportantFilterKey), false)
      return if (filter) {
         importantFilter
      } else null
   }

   private fun getFavoriteFilter(): Filter<Observation>? {
      return if (preferences.getBoolean(context.resources.getString(R.string.activeFavoritesFilterKey), false)) {
         favoriteFilter
      } else null
   }

   private fun getCustomTimeNumber(): Int {
      return preferences.getInt(context.resources.getString(R.string.customObservationTimeNumberFilterKey), 0)
   }

   private fun getCustomTimeUnit(): String? {
      return preferences.getString(context.resources.getString(R.string.customObservationTimeUnitFilterKey), context.resources.getStringArray(R.array.timeUnitEntries)[0])
   }

   private fun getTimeFilterId(): Int {
      return preferences.getInt(context.resources.getString(R.string.activeTimeFilterKey), context.resources.getInteger(R.integer.time_filter_last_month))
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
            eventLocalDataSource.getForm(observationForm.formId)?.json?.let { formJson ->
               val formDefinition = Form.fromJson(formJson)
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
         }

         returnedObservation?.let { observationLocalDataSource.update(it) }
      } else {
         observation.error = parseError(response)
         observationLocalDataSource.update(observation)
      }

      response
   }

   suspend fun archive(observation: Observation) = withContext(Dispatchers.IO) {
      val state = JsonObject()
      state.addProperty("name", "archive")

      val response = observationService.archiveObservation(observation.event.remoteId, observation.remoteId, state)
      when {
         response.isSuccessful || response.code() == HttpURLConnection.HTTP_NOT_FOUND -> {
            observationLocalDataSource.delete(observation)
         }
         response.code() != HttpURLConnection.HTTP_UNAUTHORIZED -> {
            observation.error = parseError(response)
            observationLocalDataSource.update(observation)
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
         observationLocalDataSource.updateImportant(observation)
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
         observationLocalDataSource.updateFavorite(favorite)
      }

      response
   }

   suspend fun fetch(notify: Boolean) = withContext(Dispatchers.IO) {
      val fetched = mutableListOf<Observation>()

      val currentUser = userLocalDataSource.readCurrentUser() ?: return@withContext
      val currentEvent = eventLocalDataSource.currentEvent ?: return@withContext
      Log.d(LOG_NAME, "Fetch observations for event " + currentEvent.name)

      try {
         val lastModifiedDate = observationLocalDataSource.getLatestCleanLastModified(currentUser, currentEvent)
         val iso8601Format = ISO8601DateFormatFactory.ISO8601()
         val response = observationService.getObservations(currentEvent.remoteId, iso8601Format.format(lastModifiedDate))
         if (response.isSuccessful) {
            val observations = response.body()?.map {
               it.event = currentEvent
               it
            }?.toMutableList() ?: mutableListOf()

            Log.d(LOG_NAME, "Fetched " + observations.size + " new observations")

            val iterator = observations.iterator()
            while(iterator.hasNext()) {
               val observation = iterator.next()

               observation.userId?.let { userId ->
                  val user = userLocalDataSource.read(userId)
                  // TODO : test the timer to make sure users are updated as needed!
                  val sixHoursInMilliseconds = (6 * 60 * 60 * 1000).toLong()
                  if (user == null || Date().after(Date(user.fetchedDate.time + sixHoursInMilliseconds))) {
                     // get any users that were not recognized or expired
                     Log.d(LOG_NAME, "User for observation is null or stale, re-pulling")
                     userRepository.fetchUsers(listOf(userId))
                  }
               }

               val oldObservation = observationLocalDataSource.read(observation.remoteId)
               if (observation.state == State.ARCHIVE && oldObservation != null) {
                  observationLocalDataSource.delete(oldObservation)
                  Log.d(LOG_NAME, "Deleted observation with remote_id " + observation.remoteId)
               } else if (observation.state != State.ARCHIVE && oldObservation == null) {
                  observationLocalDataSource.create(observation, false)?.let {
                     fetched.add(it)
                     Log.d(LOG_NAME, "Created observation with remote_id " + it.remoteId)
                  }
               } else if (observation.state != State.ARCHIVE && oldObservation != null && !oldObservation.isDirty) { // TODO : conflict resolution
                  observation.id = oldObservation.id
                  observationLocalDataSource.update(observation)
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
      val preferences = PreferenceManager.getDefaultSharedPreferences(context)
      val notificationsEnabled = preferences.getBoolean(context.getString(R.string.notificationsEnabledKey), context.resources.getBoolean(R.bool.notificationsEnabledDefaultValue))
      if (observations.isEmpty() || !notificationsEnabled) {
         return
      }

      val notificationManager = NotificationManagerCompat.from(context)
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
         val pendingIntent = PendingIntent.getActivity(context, 0, intent, FLAG_IMMUTABLE)

         val information = mutableListOf<String>()
         observation.forms.firstOrNull()?.let { observationForm ->
            eventLocalDataSource.getForm(observationForm.formId)?.let { form ->
               if (form.primaryFeedField != null) {
                  val property = observationForm.properties.find { it.key == form.primaryFeedField }
                  property?.value?.toString()?.let {
                     information.add(it)
                  }
               }

               if (form.secondaryFeedField != null) {
                  val property = observationForm.properties.find { it.key == form.secondaryFeedField }
                  property?.value?.toString()?.let {
                     information.add(it)
                  }
               }
            }
         }

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

   @Throws(IOException::class)
   suspend fun getAttachment(attachment: Attachment): ResponseBody? {
      val eventId = attachment.observation.event.remoteId
      val observationId = attachment.observation.remoteId
      val attachmentId = attachment.remoteId
      val response = observationService.getAttachment(eventId, observationId, attachmentId)
      if (response.isSuccessful) {
         return response.body()
      } else {
         Log.e(LOG_NAME, "Error fetching attachment $attachment")
         response.errorBody()?.let { Log.e(LOG_NAME, it.string()) }
      }
      return null
   }

   private fun parseError(response: Response<out Any>): ObservationError {
      val observationError =
         ObservationError()
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