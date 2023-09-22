package mil.nga.giat.mage.data.datasource.location

import android.util.Log
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.misc.TransactionManager
import com.j256.ormlite.stmt.Where
import mil.nga.giat.mage.database.dao.MageSqliteOpenHelper
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.database.model.location.Location
import mil.nga.giat.mage.database.model.location.LocationProperty
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.filter.Filter
import mil.nga.giat.mage.sdk.Temporal
import mil.nga.giat.mage.sdk.event.IEventDispatcher
import mil.nga.giat.mage.sdk.event.ILocationEventListener
import mil.nga.giat.mage.sdk.exceptions.LocationException
import java.sql.SQLException
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A utility class for accessing [Location] data from the physical data
 * model. The details of ORM DAOs and Lazy Loading should not be exposed past
 * this class.
 */
@Singleton
class LocationLocalDataSource @Inject constructor(
   private val daoStore: MageSqliteOpenHelper,
   private val locationDao: Dao<Location, Long>,
   private val locationPropertyDao: Dao<LocationProperty, Long>
) : IEventDispatcher<ILocationEventListener> {
   private val listeners: MutableCollection<ILocationEventListener> = CopyOnWriteArrayList()


   @Throws(LocationException::class)
   fun create(pLocation: Location): Location {
      val createdLocation: Location = try {
         TransactionManager.callInTransaction(daoStore.connectionSource) { // create Location geometry.
            val createdLocation = locationDao.createIfNotExists(pLocation)
            // create Location properties.
            val locationProperties: Collection<LocationProperty>? = pLocation.properties
            if (locationProperties != null) {
               for (locationProperty in locationProperties) {
                  locationProperty.setLocation(createdLocation)
                  locationPropertyDao.create(locationProperty)
               }
            }
            for (listener in listeners) {
               listener.onLocationCreated(listOf(createdLocation))
            }
            createdLocation
         }
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "There was a problem creating the location: $pLocation.", e)
         throw LocationException("There was a problem creating the location: $pLocation.", e)
      }
      return createdLocation
   }

   @Throws(LocationException::class)
   fun read(id: Long): Location {
      return try {
         locationDao.queryForId(id)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to query for existence for id = '$id'", e)
         throw LocationException("Unable to query for existence for id = '$id'", e)
      }
   }

   @Throws(LocationException::class)
   fun read(pRemoteId: String): Location? {
      return try {
         val results = locationDao.queryBuilder().where().eq("remote_id", pRemoteId).query()
         results.firstOrNull()
      } catch (sqle: SQLException) {
         Log.e(LOG_NAME, "Unable to query for existence for remote_id = '$pRemoteId'", sqle)
         throw LocationException("Unable to query for existence for remote_id = '$pRemoteId'", sqle)
      }
   }

   /**
    * We have to realign all the foreign ids so the update works correctly
    *
    * @param location
    * @throws LocationException
    */
   @Throws(LocationException::class)
   fun update(location: Location): Location {
      // set all the ids as needed
      val pOldLocation = read(location.id)

      // do the update
      try {
         TransactionManager.callInTransaction(daoStore.connectionSource) {
            location.id = pOldLocation.id

            // FIXME : make this run faster?
            for (lp in location.properties) {
               for (olp in pOldLocation.properties) {
                  if (lp.key.equals(olp.key, ignoreCase = true)) {
                     lp.id = olp.id
                     break
                  }
               }
            }
            locationDao.update(location)
            val properties: Collection<LocationProperty>? = location.properties
            if (properties != null) {
               for (property in properties) {
                  property.location = location
                  locationPropertyDao.createOrUpdate(property)
               }
            }
            null
         }
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "There was a problem updating the location: $location.", e)
         throw LocationException("There was a problem updating the location: $location.", e)
      }

      // fire the event
      for (listener in listeners) {
         listener.onLocationUpdated(location)
      }
      return location
   }

   /**
    * Light-weight query for testing the existence of a location in the local data-store.
    * @param location The primary key of the passed in Location object is used for the query.
    * @return
    */
   fun exists(location: Location): Boolean {
      return try {
         val locations = locationDao.queryBuilder()
            .selectColumns("_id")
            .limit(1L)
            .where()
            .eq("_id", location.id)
            .query()
         locations.isNotEmpty()
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to query for existence for location = '" + location.id + "'", e)
         false
      }
   }

   fun getCurrentUserLocations(user: User?, limit: Long, includeRemote: Boolean): List<Location> {
      var locations: List<Location> = ArrayList()
      if (user != null) {
         locations = getUserLocations(user.id, null, limit, includeRemote)
      }
      return locations
   }

   fun getSyncedLocations(user: User, event: Event): List<Location> {
      val queryBuilder = locationDao.queryBuilder()
      queryBuilder
         .where()
         .eq("user_id", user.id)
         .and()
         .isNotNull("remote_id").and()
         .eq("event_id", event.id)
      queryBuilder.orderBy("timestamp", false)
      return queryBuilder.query()
   }

   fun getAllUsersLocations(
      user: User?,
      filter: Filter<Temporal>? = null
   ): List<Location> {
      val query = locationDao.queryBuilder()
      val where = query.where()


      if (user != null) {
         where
            .ne("user_id", user.id)
            .and()
            .eq("event_id", user.userLocal.currentEvent.id)
      }

      filter?.let {
         it.query()?.let { filterQuery -> query.join(filterQuery) }
         it.and(where)
      }

      query.orderBy("timestamp", false)

      return locationDao.query(query.prepare())
   }

   fun getUserLocations(
      userId: Long?,
      eventId: Long?,
      limit: Long,
      includeRemote: Boolean
   ): List<Location> {
      var locations: List<Location> = ArrayList()
      val queryBuilder = locationDao.queryBuilder()
      try {
         if (limit > 0) {
            queryBuilder.limit(limit)
            // most recent first!
            queryBuilder.orderBy("timestamp", false)
         }
         val where: Where<Location, Long> = queryBuilder.where().eq("user_id", userId)
         if (eventId != null) {
            where.and().eq("event_id", eventId)
         }
         if (!includeRemote) {
            where.and().isNull("remote_id")
         }
         locations = locationDao.query(queryBuilder.prepare())
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Could not get current users Locations.")
      }
      return locations
   }

   /**
    * This will delete the user's location(s) that have remote_ids. Locations
    * that do NOT have remote_ids have not been sync'ed w/ the server.
    *
    * @param userLocalId
    * The user's local id
    * @throws LocationException
    */
   @Throws(LocationException::class)
   fun deleteUserLocations(userLocalId: String?, keepMostRecent: Boolean, event: Event): Int {
      val numberLocationsDeleted: Int = try {
         // newest first
         val qb = locationDao.queryBuilder().orderBy(Location.COLUMN_NAME_TIMESTAMP, false)
         qb.where()
            .eq(Location.COLUMN_NAME_USER_ID, userLocalId)
            .and()
            .eq(Location.COLUMN_NAME_EVENT_ID, event.id)
         val locations = qb.query().toMutableList()

         // if we should keep the most recent record, then skip one record.
         if (keepMostRecent) {
            locations.removeAt(0)
         }
         delete(locations)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to delete user's locations", e)
         throw LocationException("Unable to delete user's locations", e)
      }
      return numberLocationsDeleted
   }

   /**
    * This will delete all locations for an event.
    *
    * @param event
    * The event to remove locations for
    * @throws LocationException
    */
   @Throws(LocationException::class)
   fun deleteLocations(event: Event) {
      Log.e(LOG_NAME, "Deleting locations for event " + event.name)
      try {
         val qb = locationDao.queryBuilder()
         qb.where().eq("event_id", event.id)
         val locations: List<Location> = qb.query()
         delete(locations)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to delete locations for an event", e)
         throw LocationException("Unable to delete locations for an event", e)
      }
   }

   /**
    * Deletes locations. This will also delete a Location's child
    * Properties and Geometry data.
    *
    * @param locations
    * @throws LocationException
    */
   @Throws(LocationException::class)
   fun delete(locations: Collection<Location>): Int {
      val deletedLocations = try {
         TransactionManager.callInTransaction(daoStore.connectionSource) { // read the full Location in
            val deletedLocations = mutableListOf<Location>()
            for (location in locations) {
               // delete Location properties.
               location.properties?.forEach { property ->
                  locationPropertyDao.deleteById(property.id)
               }

               // finally, delete the Location.
               locationDao.deleteById(location.id)
               deletedLocations.add(location)
            }

            for (listener in listeners) {
               listener.onLocationDeleted(deletedLocations)
            }

            deletedLocations
         }
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to delete Location: " + locations.toTypedArray().contentToString(), e)
         throw LocationException("Unable to delete Location: " + locations.toTypedArray().contentToString(), e)
      }

      return deletedLocations.size
   }

   override fun addListener(listener: ILocationEventListener): Boolean {
      return listeners.add(listener)
   }

   override fun removeListener(listener: ILocationEventListener): Boolean {
      return listeners.remove(listener)
   }

   companion object {
      private val LOG_NAME = LocationLocalDataSource::class.java.name
   }
}