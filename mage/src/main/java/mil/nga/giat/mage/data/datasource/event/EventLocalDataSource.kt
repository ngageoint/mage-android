package mil.nga.giat.mage.data.datasource.event

import android.util.Log
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.misc.TransactionManager
import mil.nga.giat.mage.database.model.event.Form.Companion.getColumnNameEventId
import mil.nga.giat.mage.data.datasource.location.LocationLocalDataSource
import mil.nga.giat.mage.data.datasource.observation.ObservationLocalDataSource
import mil.nga.giat.mage.database.model.team.TeamEvent
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.database.dao.MageSqliteOpenHelper
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.database.model.event.Form
import mil.nga.giat.mage.sdk.exceptions.EventException
import mil.nga.giat.mage.sdk.exceptions.UserException
import org.apache.commons.lang3.StringUtils
import java.sql.SQLException
import javax.inject.Inject

class EventLocalDataSource @Inject constructor(
   private val daoStore: MageSqliteOpenHelper,
   private val formDao: Dao<Form, Long>,
   private val eventDao: Dao<Event, Long>,
   private val teamEventDao: Dao<TeamEvent, Long>,
   private val userLocalDataSource: UserLocalDataSource,
   private val locationLocalDataSource: LocationLocalDataSource,
   private val observationLocalDataSource: ObservationLocalDataSource
) {

   @Throws(EventException::class)
   fun create(pEvent: Event): Event {
      return try {
         eventDao.createIfNotExists(pEvent)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "There was a problem creating event: $pEvent", e)
         throw EventException("There was a problem creating event: $pEvent", e)
      }
   }

   @Throws(EventException::class)
   fun read(id: Long): Event {
      return try {
         eventDao.queryForId(id)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to query for existence for id = '$id'", e)
         throw EventException("Unable to query for existence for id = '$id'", e)
      }
   }

   @Throws(EventException::class)
   fun readAll(): MutableList<Event> {
      val events: MutableList<Event> = ArrayList()
      try {
         events.addAll(eventDao.queryForAll())
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to read Events", e)
         throw EventException("Unable to read Events.", e)
      }
      return events
   }

   @Throws(EventException::class)
   fun read(pRemoteId: String): Event? {
      var event: Event? = null
      try {
         val results = eventDao.queryBuilder().where().eq("remote_id", pRemoteId).query()
         if (results != null && results.size > 0) {
            event = results[0]
         }
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to query for existence for remote_id = '$pRemoteId'", e)
         throw EventException("Unable to query for existence for remote_id = '$pRemoteId'", e)
      }
      return event
   }

   @Throws(EventException::class)
   fun update(event: Event): Event {
      try {
         TransactionManager.callInTransaction(daoStore.connectionSource) {
            val deleteBuilder = formDao.deleteBuilder()
            deleteBuilder.where().eq(getColumnNameEventId(), event.id)
            deleteBuilder.delete()
            eventDao.update(event)
            for (form in event.forms) {
               form.event = event
               formDao.create(form)
            }
            null
         }
      } catch (sqle: SQLException) {
         Log.e(LOG_NAME, "There was a problem creating event: $event")
         throw EventException("There was a problem creating event: $event", sqle)
      }
      return event
   }

   fun createOrUpdate(event: Event): Event {
      return try {
         val oldEvent = read(event.remoteId)
         if (oldEvent == null) {
            val newEvent = create(event)
            for (form in newEvent.forms) {
               form.event = newEvent
               formDao.create(form)
            }
            Log.d(LOG_NAME, "Created event with remote_id " + newEvent.remoteId)
            newEvent
         } else {
            event.id = oldEvent.id
            update(event)
            Log.d(LOG_NAME, "Updated event with remote_id " + event.remoteId)
            event
         }
      } catch (e: Exception) {
         Log.e(LOG_NAME, "There was a problem creating event: $event", e)
         throw EventException("There was a problem creating event: $event", e)
      }

   }

   fun getForm(formId: Long): Form? {
      var form: Form? = null
      try {
         val forms = formDao.queryBuilder()
            .where()
            .eq("formId", formId)
            .query()
         if (forms != null && forms.size > 0) {
            form = forms[0]
         }
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Error pulling form with id: $formId", e)
      }
      return form
   }

   val currentEvent: Event?
      get() {
         var event: Event? = null
         try {
            val user: User? = userLocalDataSource.readCurrentUser()
            if (user != null) {
               event = user.userLocal.currentEvent
            } else {
               Log.d(LOG_NAME, "Current user is null.  Why?")
            }
         } catch (ue: UserException) {
            Log.e(LOG_NAME, "There is no current user. ", ue)
         }
         return event
      }

   fun getRecentEvents(): List<Event> {
      val user = userLocalDataSource.readCurrentUser() ?: return emptyList()

      return  try {
         val recentEventIds = user.recentEventIds
         val cases = mutableListOf<String>()
         for (i in recentEventIds.indices) {
            cases.add("WHEN " + recentEventIds[i] + " THEN " + i)
         }
         eventDao
            .queryBuilder()
            .orderByRaw(
               String.format(
                  "CASE %s %s END",
                  Event.COLUMN_NAME_REMOTE_ID,
                  StringUtils.join(cases, " ")
               )
            )
            .where()
            .`in`(Event.COLUMN_NAME_REMOTE_ID, user.recentEventIds)
            .query()
      } catch (e: Exception) {
         emptyList()
      }
   }

   /**
    * Remove any events from the database that are not in this event list.
    *
    * @param remoteEvents list of events that should remain in the database, all others will be removed
    */
   fun syncEvents(remoteEvents: List<Event>) {
      try {
         val eventsToRemove = readAll()
         eventsToRemove.removeAll(remoteEvents)
         for (eventToRemove in eventsToRemove) {
            Log.e(LOG_NAME, "Removing event " + eventToRemove.name)
            locationLocalDataSource.deleteLocations(eventToRemove)
            observationLocalDataSource.deleteObservations(eventToRemove)
            val teamDeleteBuilder = teamEventDao.deleteBuilder()
            teamDeleteBuilder.where().eq("event_id", eventToRemove.id)
            teamDeleteBuilder.delete()
            val eventDeleteBuilder = eventDao.deleteBuilder()
            eventDeleteBuilder.where().idEq(eventToRemove.id)
            eventDeleteBuilder.delete()
         }
      } catch (e: Exception) {
         Log.e(LOG_NAME, "Error deleting event ", e)
      }
   }

   companion object {
      private val LOG_NAME = EventLocalDataSource::class.java.name
   }
}