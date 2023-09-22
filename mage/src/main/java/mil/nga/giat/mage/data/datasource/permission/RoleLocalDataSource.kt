package mil.nga.giat.mage.data.datasource.permission

import android.util.Log
import com.j256.ormlite.dao.Dao
import mil.nga.giat.mage.database.model.permission.Role
import mil.nga.giat.mage.sdk.exceptions.RoleException
import java.sql.SQLException
import javax.inject.Inject

class RoleLocalDataSource @Inject constructor(
   private val roleDao: Dao<Role, Long>
) {

   @Throws(RoleException::class)
   fun create(pRole: Role): Role {
      return try {
         val createdRole = roleDao.createIfNotExists(pRole)
         Log.d(LOG_NAME, "created role with remote_id " + createdRole.remoteId)
         createdRole
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "There was a problem creating the role: $pRole")
         throw RoleException("There was a problem creating the role: $pRole", e)
      }
   }

   @Throws(RoleException::class)
   fun update(pRole: Role): Role {
      try {
         roleDao.update(pRole)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "There was a problem creating role: $pRole")
         throw RoleException("There was a problem creating role: $pRole", e)
      }
      return pRole
   }

   fun createOrUpdate(role: Role): Role {
      return try {
         val oldRole = read(role.remoteId)
         if (oldRole == null) {
            val newRole = create(role)
            Log.d(LOG_NAME, "Created role with remote_id " + role.remoteId)
            newRole
         } else {
            role.id = oldRole.id
            update(role)
            Log.d(LOG_NAME, "Updated role with remote_id " + role.remoteId)
            role
         }
      } catch (e: RoleException) {
         Log.e(LOG_NAME, "There was a problem reading role: $role", e)
         role
      }
   }

   @Throws(RoleException::class)
   fun read(id: Long): Role {
      return try {
         roleDao.queryForId(id)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to query for existence for id = '$id'", e)
         throw RoleException("Unable to query for existence for id = '$id'", e)
      }
   }

   fun read(pRemoteId: String): Role? {
      return try {
         val results = roleDao.queryBuilder().where().eq("remote_id", pRemoteId).query()
         results.firstOrNull()
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to query for existence for remote_id = '$pRemoteId'", e)
         null
      }
   }

   fun readAll(): Collection<Role> {
      return try {
         roleDao.queryForAll()
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to read Observations", e)
         emptyList()
      }
   }

   companion object {
      private val LOG_NAME = RoleLocalDataSource::class.java.name
   }
}