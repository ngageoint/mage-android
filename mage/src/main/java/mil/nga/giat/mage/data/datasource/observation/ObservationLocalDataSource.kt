package mil.nga.giat.mage.data.datasource.observation

import android.app.Application
import android.content.Context
import android.util.Log
import com.j256.ormlite.dao.Dao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import mil.nga.giat.mage.database.dao.MageSqliteOpenHelper
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.database.model.observation.Attachment
import mil.nga.giat.mage.database.model.observation.Observation
import mil.nga.giat.mage.database.model.observation.ObservationFavorite
import mil.nga.giat.mage.database.model.observation.ObservationForm
import mil.nga.giat.mage.database.model.observation.ObservationImportant
import mil.nga.giat.mage.database.model.observation.ObservationProperty
import mil.nga.giat.mage.database.model.observation.State
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.filter.Filter
import mil.nga.giat.mage.sdk.Compatibility.Companion.isServerVersion5
import mil.nga.giat.mage.sdk.Temporal
import mil.nga.giat.mage.sdk.event.IEventDispatcher
import mil.nga.giat.mage.sdk.event.IObservationEventListener
import mil.nga.giat.mage.sdk.exceptions.ObservationException
import java.sql.SQLException
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObservationLocalDataSource @Inject constructor(
   private val application: Application,
   private val observationDao: Dao<Observation, Long>,
   private val observationFormDao: Dao<ObservationForm, Long>,
   private val observationPropertyDao: Dao<ObservationProperty, Long>,
   private val observationImportantDao: Dao<ObservationImportant, Long>,
   private val observationFavoriteDao: Dao<ObservationFavorite, Long>,
   private val attachmentLocalDataSource: AttachmentLocalDataSource,
   private val observationLocationLocalDataSource: ObservationLocationLocalDataSource
) : IEventDispatcher<IObservationEventListener> {

   // When we change to Room the dao will take care of making the flow, but in the mean time...
   fun observeObservation(observationId: Long): Flow<Observation?> {
      return flowOf(observationDao.queryForId(observationId))
   }

   private val listeners: MutableCollection<IObservationEventListener> = CopyOnWriteArrayList()

   @JvmOverloads
   @Throws(ObservationException::class)
   fun create(observation: Observation, sendNotifications: Boolean? = true): Observation? {
      var savedObservation: Observation? = null
      try {
         savedObservation = observationDao.callBatchTasks {

            // Now we try and create the Observation structure.
            try {
               // set last Modified
               if (observation.lastModified == null) {
                  observation.lastModified = Date()
               }

               // create the Observation.
               observationDao.create(observation)
               observation.forms.forEach { form ->
                  form.setObservation(observation)
                  observationFormDao.create(form)

                  // create Observation properties.
                  form.properties.forEach { property ->
                     property.setObservationForm(form)
                     observationPropertyDao.create(property)
                  }
               }

               // create Observation favorites.
               observation.favorites.forEach { favorite ->
                  favorite.observation = observation
                  observationFavoriteDao.create(favorite)
               }

               // create Observation attachments.
               observation.attachments.forEach { attachment ->
                  try {
                     attachment.observation = observation
                     attachmentLocalDataSource.create(attachment)
                  } catch (e: Exception) {
                     throw ObservationException("There was a problem creating the observations attachment: $attachment.", e)
                  }
               }

               // create Observation Locations
               observationLocationLocalDataSource.create(observation)

            } catch (e: SQLException) {
               Log.e(LOG_NAME, "There was a problem creating the observation: $observation.", e)
               throw ObservationException("There was a problem creating the observation: $observation.", e)
            }

            // fire the event
            for (listener in listeners) {
               listener.onObservationCreated(listOf(observation), sendNotifications)
            }
            observation
         }
      } catch (e: Exception) {
         Log.e(LOG_NAME, "Error creating observation", e)
      }
      return savedObservation
   }

   @Throws(ObservationException::class)
   fun read(id: Long): Observation {
      return try {
         observationDao.queryForId(id)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to query for existence for id = '$id'", e)
         throw ObservationException("Unable to query for existence for id = '$id'", e)
      }
   }

   @Throws(ObservationException::class)
   fun read(pRemoteId: String): Observation? {
      return try {
         val results = observationDao.queryBuilder().where().eq("remote_id", pRemoteId).query()
        results.firstOrNull()
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to query for existence for remote_id = '$pRemoteId'", e)
         throw ObservationException("Unable to query for existence for remote_id = '$pRemoteId'", e)
      }
   }

   /**
    * We have to realign all the foreign ids so the update works correctly
    *
    * @param observation
    * @throws ObservationException
    */
   @Throws(ObservationException::class)
   fun update(observation: Observation): Observation {
      Log.i(LOG_NAME, "Updating observation w/ id: " + observation.id)
      val updatedObservation: Observation
      try {
         updatedObservation = observationDao.callBatchTasks {

            // set all the ids as needed
            val oldObservation = read(observation.id)

            // if the observation is dirty, set the last_modified date!
            // FIXME this is a server property and should not be set by the client,
            // investigate why we are setting this
            if (observation.isDirty) {
               observation.lastModified = Date()
            }
            val important = observation.important
            val oldImportant = oldObservation.important
            if (oldImportant != null && oldImportant.isDirty) {
               observation.setImportant(oldImportant)
            } else {
               if (important != null) {
                  if (oldImportant != null) {
                     important.id = oldImportant.id
                  }
                  observationImportantDao.createOrUpdate(important)
               } else {
                  if (oldImportant != null) {
                     observationImportantDao.deleteById(oldImportant.id)
                  }
               }
            }
            observationDao.update(observation)

            // TODO might not need to delete all forms/properties when server sets a unique form id
            // Delete all forms for this observation and all properties
            oldObservation.forms.forEach { form ->
               form.properties.forEach { property ->
                  observationPropertyDao.deleteById(property.id)
               }
               observationFormDao.deleteById(form.id)
            }

            observation.forms.forEach { form ->
               form.setObservation(observation)
               observationFormDao.createOrUpdate(form)
               form.properties.forEach { property ->
                  property.setObservationForm(form)
                  observationPropertyDao.createOrUpdate(property)
               }
            }

            val favorites = observation.favoritesMap
            val oldFavorites = oldObservation.favoritesMap
            favorites.keys.intersect(oldFavorites.keys).forEach { key ->
               favorites[key]!!.id = oldFavorites[key]!!.id
            }

            // Map database ids from old properties to new properties
            favorites.values.forEach { favorite ->
               val oldFavorite = oldFavorites[favorite.userId]
               // only update favorite if local is not dirty
               if (oldFavorite == null || !oldFavorite.isDirty) {
                  favorite.observation = observation
                  observationFavoriteDao.createOrUpdate(favorite)
               }
            }

            // Remove any favorites that existed in the old observation but do not exist
            // in the new observation.
            oldFavorites.keys.subtract(favorites.keys).forEach { key ->
               // Only delete favorites that are not dirty
               if (!oldFavorites[key]!!.isDirty) {
                  observationFavoriteDao.deleteById(oldFavorites[key]!!.id)
               }
            }

            Log.i(LOG_NAME, "Observation attachments " + observation.attachments.size)
            oldObservation.attachments.forEach { oldAttachment ->
               if (oldAttachment.remoteId != null) {
                  var found: Attachment? = null
                  observation.attachments.forEach { attachment ->
                     if (oldAttachment.remoteId == attachment.remoteId) {
                        found = attachment
                        attachment.id = oldAttachment.id
                     }
                  }

                  // if no longer in attachments array response from server, remove it
                  if (!isServerVersion5(application)) {
                     if (found == null) {
                        attachmentLocalDataSource.delete(oldAttachment)
                     }
                  }
               }
            }

            for (attachment in observation.attachments) {
               try {
                  attachment.observation = observation
                  attachmentLocalDataSource.create(attachment)
               } catch (e: Exception) {
                  throw ObservationException("There was a problem creating/updating the observations attachment: $attachment.", e)
               }
            }
            observationDao.refresh(observation)
            if (observation.remoteId != null) {
               observation.attachments.filter { it.isDirty }.forEach { attachment ->
                  attachmentLocalDataSource.uploadableAttachment(attachment)
               }
            }
            observation
         }
      } catch (e: Exception) {
         Log.e(LOG_NAME, "There was a problem updating the observation: $observation.", e)
         throw ObservationException("There was a problem updating the observation: $observation.", e)
      }

      // fire the event
      for (listener in listeners) {
         listener.onObservationUpdated(updatedObservation)
      }
      return updatedObservation
   }

   @Throws(ObservationException::class)
   fun readAll(): List<Observation> {
      return try {
         observationDao.queryForAll()
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to read Observations", e)
         throw ObservationException("Unable to read Observations.", e)
      }
   }

   fun getEventObservations(
      event: Event,
      filters: List<Filter<Observation>>,
      observationIds: List<Long>? = null
   ): List<Observation> {
      val query = observationDao.queryBuilder()
      val where = query
         .orderBy("timestamp", false)
         .where()
         .eq("event_id", event.id)
//         .and()
//         .`in`("_id", observationIds)


      filters.forEach { filter ->
         filter.query()?.let { query.join(it) }
         filter.and(where)
      }

      return observationDao.query(query.prepare())
   }

   /**
    * Gets the latest last modified date.  Used when fetching.
    *
    * @return
    */
   fun getLatestCleanLastModified(user: User?, event: Event): Date {
      var lastModifiedDate = Date(0)
      val queryBuilder = observationDao.queryBuilder()
      try {
         if (user != null) {
            queryBuilder.where()
               .eq("dirty", java.lang.Boolean.FALSE)
               .and()
               .ne("user_id", user.remoteId.toString())
               .and()
               .eq("event_id", event.id)
            queryBuilder.orderBy("last_modified", false)
            val o = observationDao.queryForFirst(queryBuilder.prepare())
            if (o != null) {
               lastModifiedDate = o.lastModified
            }
         }
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Could not get last_modified date.", e)
      }
      return lastModifiedDate
   }

   /**
    * Gets a List of Observations from the datastore that are dirty (i.e.
    * should be synced with the server).
    *
    * @return
    */
   val dirty: List<Observation>
      get() {
         val queryBuilder = observationDao.queryBuilder()
         return try {
            queryBuilder.where().eq("dirty", true)
             observationDao.query(queryBuilder.prepare())
         } catch (e: SQLException) {
            Log.e(LOG_NAME, "Could not get dirty Observations.", e)
            emptyList()
         }
      }

   /**
    * Archive an Observation. This will remove the observation from the server
    *
    * @param observation
    * @throws ObservationException
    */
   @Throws(ObservationException::class)
   fun archive(observation: Observation) {
      if (observation.remoteId == null) {
         // observation does not exist on the server yet, just remove it from the database
         try {
            observationDao.delete(observation)
         } catch (e: SQLException) {
            throw ObservationException("Unable to archive Observation: " + observation.id, e)
         }
      } else {
         observation.state = State.ARCHIVE
         observation.isDirty = true
         try {
            observationDao.update(observation)
         } catch (e: SQLException) {
            throw ObservationException("Unable to archive Observation: " + observation.id, e)
         }

         for (listener in listeners) {
            listener.onObservationUpdated(observation)
         }
      }
   }

   /**
    * Deletes an Observation. This will also delete an Observation's child
    * Attachments, child Properties and Geometry data.
    *
    * @param observation
    * @throws ObservationException
    */
   @Throws(ObservationException::class)
   fun delete(observation: Observation) {
      try {
         observationDao.callBatchTasks<Void> { // delete Observation forms.
            observation.forms.forEach { form ->
               form.properties.forEach { property ->
                  observationPropertyDao.deleteById(property.id)

               }

               observationFormDao.deleteById(form.id)
            }

            // delete Observation favorites.
            val favorites = observation.favorites
            if (favorites != null) {
               for (favorite in favorites) {
                  observationFavoriteDao.deleteById(favorite.id)
               }
            }

            // delete Observation attachments.
            val attachments = observation.attachments
            if (attachments != null) {
               for (attachment in attachments) {
                  attachmentLocalDataSource.delete(attachment)
               }
            }

            // delete important
            val important = observation.important
            if (important != null) {
               observationImportantDao.deleteById(important.id)
            }

            // finally, delete the Observation.
            observationDao.deleteById(observation.id)
            for (listener in listeners) {
               listener.onObservationDeleted(observation)
            }
            null
         }
      } catch (e: Exception) {
         Log.e(LOG_NAME, "Unable to delete Observation: " + observation.id, e)
         throw ObservationException("Unable to delete Observation: " + observation.id, e)
      }
   }

   /**
    * This will delete all observations for an event.
    *
    * @param event
    * The event to remove locations for
    * @throws ObservationException
    */
   @Throws(ObservationException::class)
   fun deleteObservations(event: Event) {
      Log.e(LOG_NAME, "Deleting observations for event " + event.name)
      try {
         val qb = observationDao.queryBuilder()
         qb.where().eq("event_id", event.id)
         for (observation in qb.query()) {
            delete(observation)
         }
      } catch (sqle: SQLException) {
         Log.e(LOG_NAME, "Unable to delete observations for an event", sqle)
         throw ObservationException("Unable to delete observations for an event", sqle)
      }
   }

   /**
    * This will mark the  observation as important
    *
    * @param observation The observation to mark as important
    *
    * @throws ObservationException
    */
   @Throws(ObservationException::class)
   fun addImportant(observation: Observation) {
      val important = observation.important
      important!!.isImportant = true
      important.isDirty = true
      try {
         observationImportantDao.createOrUpdate(important)
         observationDao.update(observation)

         // fire the event
         for (listener in listeners) {
            listener.onObservationUpdated(observation)
         }
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to favorite observation", e)
         throw ObservationException("Unable to favorite observation", e)
      }
   }

   /**
    * This will remove the important mark from an observation.
    *
    * @param observation The observation to unfavorite
    *
    * @throws ObservationException
    */
   @Throws(ObservationException::class)
   fun removeImportant(observation: Observation) {
      try {
         observationImportantDao.queryForAll()
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Error querying for observations", e)
      }
      val important = observation.important
      if (important != null) {
         important.isImportant = false
         important.isDirty = true
         try {
            observationImportantDao.update(important)
            observationDao.refresh(observation)

            // fire the event
            for (listener in listeners) {
               listener.onObservationUpdated(observation)
            }
         } catch (e: SQLException) {
            Log.e(LOG_NAME, "Unable to unfavorite observation", e)
            throw ObservationException("Unable to unfavorite observation", e)
         }
      }
   }

   @Throws(ObservationException::class)
   fun updateImportant(observation: Observation) {
      val important = observation.important
      try {
         if (important!!.isImportant) {
            important.isDirty = java.lang.Boolean.FALSE
            observation.important = important
            observationImportantDao.update(important)
         } else {
            observationImportantDao.delete(important)
         }

         // Update the observation so that the lastModified time is updated
         observationDao.update(observation)
         observationDao.refresh(observation)
         for (listener in listeners) {
            listener.onObservationUpdated(observation)
         }
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to update observation favorite", e)
         throw ObservationException("Unable to update observation favorite", e)
      }
   }

   /**
    * This will favorite and observation for the user.
    *
    * @param observation The observation to favorite
    * @param user The user that is favoriting the observation
    *
    * @throws ObservationException
    */
   @Throws(ObservationException::class)
   fun favoriteObservation(observation: Observation, user: User) {
      val favoritesMap = observation.favoritesMap
      var favorite = favoritesMap[user.remoteId]
      if (favorite == null) {
         favorite = ObservationFavorite(user.remoteId, true)
      }
      favorite.observation = observation
      favorite.isFavorite = true
      favorite.isDirty = true
      try {
         observationFavoriteDao.createOrUpdate(favorite)
         observationDao.refresh(observation)

         // fire the event
         for (listener in listeners) {
            listener.onObservationUpdated(favorite.observation)
         }
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to favorite observation", e)
         throw ObservationException("Unable to favorite observation", e)
      }
   }

   /**
    * This will unfavorite and observation for the user.
    *
    * @param observation The observation to unfavorite
    * @param user The user that is unfavoriting the observation
    *
    * @throws ObservationException
    */
   @Throws(ObservationException::class)
   fun unfavoriteObservation(observation: Observation, user: User) {
      val favoritesMap = observation.favoritesMap
      val favorite = favoritesMap[user.remoteId]
      if (favorite != null) {
         favorite.isFavorite = false
         favorite.isDirty = true
         try {
            observationFavoriteDao.update(favorite)
            observationDao.refresh(observation)

            // fire the event
            for (listener in listeners) {
               listener.onObservationUpdated(favorite.observation)
            }
         } catch (e: SQLException) {
            Log.e(LOG_NAME, "Unable to remove favorite from observation", e)
            throw ObservationException("Unable to remove favorite from observation", e)
         }
      }
   }

   @Throws(ObservationException::class)
   fun updateFavorite(favorite: ObservationFavorite) {
      try {
         val observation = favorite.observation
         if (favorite.isFavorite) {
            favorite.isDirty = java.lang.Boolean.FALSE
            observationFavoriteDao.update(favorite)
         } else {
            observationFavoriteDao.delete(favorite)
         }

         // Update the observation so that the lastModified time is updated
         observationDao.update(observation)
         observationDao.refresh(observation)
         for (listener in listeners) {
            listener.onObservationUpdated(observation)
         }
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to update observation favorite", e)
         throw ObservationException("Unable to update observation favorite", e)
      }
   }// TODO Auto-generated catch block

   /**
    * A List of [ObservationImportant] from the datastore that are dirty (i.e.
    * should be synced with the server).
    *
    * @return
    */
   @get:Throws(ObservationException::class)
   val dirtyImportant: List<Observation>
      get() = try {
         val importantQb = observationImportantDao.queryBuilder()
         importantQb.where().eq("dirty", true)
         val observationQb = observationDao.queryBuilder()
         observationQb.join(importantQb).query()
      } catch (e: SQLException) {
         // TODO Auto-generated catch block
         Log.e(LOG_NAME, "Unable to get dirty observation favorites", e)
         throw ObservationException("Unable to get dirty observation favorites", e)
      }// TODO Auto-generated catch block

   /**
    * A List of [ObservationProperty] from the datastore that are dirty (i.e.
    * should be synced with the server).
    *
    * @return
    */
   @get:Throws(ObservationException::class)
   val dirtyFavorites: List<ObservationFavorite>
      get() = try {
         val queryBuilder = observationFavoriteDao.queryBuilder()
         queryBuilder.where().eq("dirty", true)
         observationFavoriteDao.query(queryBuilder.prepare())
      } catch (e: SQLException) {
         // TODO Auto-generated catch block
         Log.e(LOG_NAME, "Unable to get dirty observation favorites", e)
         throw ObservationException("Unable to get dirty observation favorites", e)
      }

   override fun addListener(listener: IObservationEventListener): Boolean {
      return listeners.add(listener)
   }

   override fun removeListener(listener: IObservationEventListener): Boolean {
      return listeners.remove(listener)
   }

   companion object {
      private val LOG_NAME = ObservationLocalDataSource::class.java.name
   }
}