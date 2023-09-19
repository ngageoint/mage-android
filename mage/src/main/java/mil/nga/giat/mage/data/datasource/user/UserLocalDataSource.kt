package mil.nga.giat.mage.data.datasource.user

import android.util.Log
import com.j256.ormlite.dao.Dao
import mil.nga.giat.mage.data.datasource.team.TeamLocalDataSource
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.database.model.team.TeamEvent
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.database.model.user.UserLocal
import mil.nga.giat.mage.database.model.user.UserTeam
import mil.nga.giat.mage.sdk.event.IEventDispatcher
import mil.nga.giat.mage.sdk.event.IEventEventListener
import mil.nga.giat.mage.sdk.event.IUserDispatcher
import mil.nga.giat.mage.sdk.event.IUserEventListener
import mil.nga.giat.mage.sdk.exceptions.UserException
import java.sql.SQLException
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject

class UserLocalDataSource @Inject constructor(
   private val userDao: Dao<User, Long>,
   private val userLocalDao: Dao<UserLocal, Long>,
   private val userTeamDao: Dao<UserTeam, Long>,
   private val teamEventDao: Dao<TeamEvent, Long>,
   private val teamLocalDataSource: TeamLocalDataSource
): IEventDispatcher<IEventEventListener>, IUserDispatcher {

   // FIXME : should add user to team if needed
   @Throws(UserException::class)
   fun create(user: User): User {
      try {
         val userLocal = userLocalDao.createIfNotExists(UserLocal())
         user.userLocal = userLocal
         val newUser =  userDao.createIfNotExists(user)

         for (listener in userListeners) {
            listener.onUserCreated(newUser)
         }

         return newUser
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "There was a problem creating user: $user", e)
         throw UserException("There was a problem creating user: $user", e)
      }
   }

   @Throws(UserException::class)
   fun read(id: Long): User {
      return try {
         userDao.queryForId(id)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to query for existence for id = '$id'", e)
         throw UserException("Unable to query for existence for id = '$id'", e)
      }
   }

   @Throws(UserException::class)
   fun read(remoteId: String): User? {
      try {
         val results = userDao.queryBuilder().where().eq("remote_id", remoteId).query()
         return results.firstOrNull()
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to query for existence for remote_id = '$remoteId'", e)
         throw UserException("Unable to query for existence for remote_id = '$remoteId'", e)
      }
   }

   @Throws(UserException::class)
   fun read(remoteIds: Collection<String?>): List<User> {
      return try {
         userDao.queryBuilder().where().`in`("remote_id", remoteIds).query()
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to query for existence for remote_ids = '$remoteIds'", e)
         throw UserException("Unable to query for existence for remote_ids = '$remoteIds'", e)
      }
   }

   fun readCurrentUser(): User? {
      return try {
         val userLocalQuery = userLocalDao.queryBuilder()
         userLocalQuery.selectColumns(UserLocal.COLUMN_NAME_ID)
         val where = userLocalQuery.where()
         where.eq(UserLocal.COLUMN_NAME_CURRENT_USER, true)
         val userQuery = userDao.queryBuilder()
         userQuery.where().`in`(User.COLUMN_NAME_USER_LOCAL_ID, userLocalQuery)
         val preparedQuery = userQuery.prepare()
         userDao.queryForFirst(preparedQuery)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "There was a problem reading active users.")
         null
      }
   }

   fun isCurrentUserPartOfCurrentEvent(): Boolean {
      return try {
         readCurrentUser()?.let { user ->
            val currentEvent = user.currentEvent
            val userTeams = teamLocalDataSource.getTeamsByUser(user)
            val eventTeams = teamLocalDataSource.getTeamsByEvent(currentEvent).toMutableSet()
            eventTeams.retainAll(userTeams.toSet())
            eventTeams.size > 0
         } ?: false
      } catch (e: Exception) {
         Log.e(LOG_NAME, "error determining current user event membership", e)
         false
      }
   }

   @Throws(UserException::class)
   fun update(user: User): User {
      try {
         val oldUser = read(user.id)
         user.userLocal = oldUser.userLocal
         userDao.update(user)

         for (listener in userListeners) {
            listener.onUserUpdated(user)
         }

         return user
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "There was a problem creating user: $user")
         throw UserException("There was a problem creating user: $user", e)
      }
   }

   fun createOrUpdate(user: User): User {
      val db = userDao.queryBuilder()
      db.where().eq(User.COLUMN_NAME_USERNAME, user.username)
      val oldUser = db.queryForFirst()

      return if (oldUser == null) {
         val newUser = create(user)
         Log.d(LOG_NAME, "Created user with remote_id " + newUser.remoteId)
         newUser
      } else {
         user.id = oldUser.id
         user.userLocal = oldUser.userLocal
         userDao.update(user)
         Log.d(LOG_NAME, "Updated user with remote_id " + user.remoteId)

         for (listener in userListeners) {
            listener.onUserUpdated(user)
         }

         user
      }
   }

   @Throws(UserException::class)
   fun setCurrentUser(user: User): User {
      try {
         clearCurrentUser()
         val builder = userLocalDao.updateBuilder()
         builder.where().idEq(user.userLocal.id)
         builder.updateColumnValue(UserLocal.COLUMN_NAME_CURRENT_USER, true)
         builder.update()
         userDao.refresh(user)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to update user '" + user.displayName + "' to current user", e)
         throw UserException("Unable to update UserLocal table", e)
      }
      return user
   }

   @Throws(UserException::class)
   fun setCurrentEvent(user: User, event: Event?): User {
      try {
         val builder = userLocalDao.updateBuilder()
         builder.where().idEq(user.userLocal.id)
         builder.updateColumnValue(UserLocal.COLUMN_NAME_CURRENT_EVENT, event)

         // check if we need to send event onChange
         val userLocal = user.userLocal
         if (userLocal.isCurrentUser) {
            var oldEventRemoteId: String? = null
            if (userLocal.currentEvent != null) {
               oldEventRemoteId = userLocal.currentEvent.remoteId
            }
            val newEventRemoteId = event?.remoteId

            // run update before firing event to make sure update works.
            builder.update()
            if ((oldEventRemoteId == null) xor (newEventRemoteId == null)) {
               for (listener in eventListeners) {
                  listener.onEventChanged()
               }
            } else if (oldEventRemoteId != null && newEventRemoteId != null) {
               if (oldEventRemoteId != newEventRemoteId) {
                  for (listener in eventListeners) {
                     listener.onEventChanged()
                  }
               }
            }
            userDao.refresh(user)
         }
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to update users '" + user.displayName + "' current event", e)
         throw UserException("Unable to update UserLocal table", e)
      }
      return user
   }

   fun removeCurrentEvent(): User? {
      val user = readCurrentUser() ?: return null

      try {
         val builder = userLocalDao.updateBuilder()
         builder.where().idEq(user.userLocal.id)
         builder.updateColumnValue(UserLocal.COLUMN_NAME_CURRENT_EVENT, null)
         builder.update()
         userDao.refresh(user)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to clear current event for user '" + user.displayName + "'")
      }

      return user
   }

   @Throws(UserException::class)
   fun setAvatarPath(user: User, path: String?): User {
      try {
         val builder = userLocalDao.updateBuilder()
         builder.where().idEq(user.userLocal.id)
         builder.updateColumnValue(UserLocal.COLUMN_NAME_AVATAR_PATH, path)
         builder.update()
         userLocalDao.refresh(user.userLocal)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to update users '" + user.displayName + "' avatar path", e)
         throw UserException("Unable to update UserLocal table", e)
      }
      for (listener in userListeners) {
         listener.onUserAvatarUpdated(user)
      }
      return user
   }

   @Throws(UserException::class)
   fun setIconPath(user: User, path: String?): User {
      try {
         val builder = userLocalDao.updateBuilder()
         builder.where().idEq(user.userLocal.id)
         builder.updateColumnValue(UserLocal.COLUMN_NAME_ICON_PATH, path)
         builder.update()
         userLocalDao.refresh(user.userLocal)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to update users '" + user.displayName + "' icon path", e)
         throw UserException("Unable to update UserLocal table", e)
      }
      for (listener in userListeners) {
         listener.onUserIconUpdated(user)
      }
      return user
   }

   @Throws(UserException::class)
   private fun clearCurrentUser() {
      try {
         val builder = userLocalDao.updateBuilder()
         builder.updateColumnValue(UserLocal.COLUMN_NAME_CURRENT_USER, java.lang.Boolean.FALSE)
         builder.update()
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "There was a problem deleting active userlocal.", e)
         throw UserException("There was a problem deleting active userlocal.", e)
      }
   }

   fun deleteUserTeams() {
      try {
         val db = userTeamDao.deleteBuilder()
         db.delete()
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "There was a problem deleting userteams.", e)
      }
   }

   fun create(userTeam: UserTeam): UserTeam? {
      var createdUserTeam: UserTeam? = null
      try {
         createdUserTeam = userTeamDao.createIfNotExists(userTeam)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "There was a problem creating userteam: $userTeam", e)
      }
      return createdUserTeam
   }

   fun getUsersInEvent(event: Event): Collection<User> {
      return try {
         val teamEventQuery = teamEventDao.queryBuilder()
         teamEventQuery.selectColumns("team_id")
         val teamEventWhere = teamEventQuery.where()
         teamEventWhere.eq("event_id", event.id)
         val userTeamQuery = userTeamDao.queryBuilder()
         userTeamQuery.selectColumns("user_id")
         val userTeamWhere = userTeamQuery.where()
         userTeamWhere.`in`("team_id", teamEventQuery)
         val teamQuery = userDao.queryBuilder()
         teamQuery.where().`in`("_id", userTeamQuery)
         teamQuery.query()
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Error getting users for event: $event", e)
         emptyList()
      }
   }

   override fun addListener(listener: IEventEventListener): Boolean {
      return eventListeners.add(listener)
   }

   override fun removeListener(listener: IEventEventListener): Boolean {
      return eventListeners.remove(listener)
   }

   override fun addListener(listener: IUserEventListener): Boolean {
      return userListeners.add(listener)
   }

   override fun removeListener(listener: IUserEventListener): Boolean {
      return userListeners.add(listener)
   }

   companion object {
      private val LOG_NAME = UserLocalDataSource::class.java.name
      private val userListeners: MutableCollection<IUserEventListener> = CopyOnWriteArrayList()
      private val eventListeners: MutableCollection<IEventEventListener> = CopyOnWriteArrayList()
   }
}