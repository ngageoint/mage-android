package mil.nga.giat.mage.data.datasource.team

import android.util.Log
import com.j256.ormlite.dao.Dao
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.database.model.team.Team
import mil.nga.giat.mage.database.model.team.TeamEvent
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.database.model.user.UserTeam
import mil.nga.giat.mage.sdk.exceptions.EventException
import mil.nga.giat.mage.sdk.exceptions.TeamException
import java.sql.SQLException
import javax.inject.Inject

class TeamLocalDataSource @Inject constructor(
   private val teamDao: Dao<Team, Long>,
   private val userTeamDao: Dao<UserTeam, Long>,
   private val teamEventDao: Dao<TeamEvent, Long>
) {
   @Throws(TeamException::class)
   fun create(pTeam: Team): Team {
      val createdTeam: Team = try {
         teamDao.createIfNotExists(pTeam)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "There was a problem creating team: $pTeam", e)
         throw TeamException("There was a problem creating team: $pTeam", e)
      }
      return createdTeam
   }

   @Throws(TeamException::class)
   fun read(id: Long): Team {
      return try {
         teamDao.queryForId(id)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to query for existence for id = '$id'", e)
         throw TeamException("Unable to query for existence for id = '$id'", e)
      }
   }

   @Throws(EventException::class)
   fun readAll(): MutableList<Team> {
      val teams: MutableList<Team> = ArrayList()
      try {
         teams.addAll(teamDao.queryForAll())
      } catch (sqle: SQLException) {
         Log.e(LOG_NAME, "Unable to read Teams", sqle)
         throw EventException("Unable to read Teams.", sqle)
      }
      return teams
   }

   @Throws(TeamException::class)
   fun read(pRemoteId: String): Team? {
      var team: Team? = null
      try {
         val results = teamDao.queryBuilder().where().eq("remote_id", pRemoteId).query()
         if (results != null && results.size > 0) {
            team = results[0]
         }
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to query for existence for remote_id = '$pRemoteId'", e)
         throw TeamException("Unable to query for existence for remote_id = '$pRemoteId'", e)
      }
      return team
   }

   @Throws(TeamException::class)
   fun update(pTeam: Team): Team {
      try {
         teamDao.update(pTeam)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "There was a problem creating team: $pTeam")
         throw TeamException("There was a problem creating team: $pTeam", e)
      }
      return pTeam
   }

   fun createOrUpdate(update: Team): Team? {
      return try {
         val oldTeam = read(update.remoteId)
         if (oldTeam == null) {
            val team = create(update)
            Log.d(LOG_NAME, "Created team with remote_id " + team.remoteId)
            team
         } else {
            // perform update?
            update.id = oldTeam.id
            val  team = update(update)
            Log.d(LOG_NAME, "Updated team with remote_id " + team.remoteId)
            team
         }
      } catch (e: TeamException) {
         Log.e(LOG_NAME, "There was a problem reading team: $update", e)
         null
      }
   }

   fun deleteTeamEvents() {
      try {
         val db = teamEventDao.deleteBuilder()
         db.delete()
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "There was a problem deleting teamevents.", e)
      }
   }

   fun create(pTeamEvent: TeamEvent): TeamEvent? {
      var createdTeamEvent: TeamEvent? = null
      try {
         createdTeamEvent = teamEventDao.createIfNotExists(pTeamEvent)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "There was a problem creating teamevent: $pTeamEvent", e)
      }
      return createdTeamEvent
   }

   fun getTeamsByUser(pUser: User): List<Team> {
      return try {
         val userTeamQuery = userTeamDao.queryBuilder()
         userTeamQuery.selectColumns("team_id")
         val where = userTeamQuery.where()
         where.eq("user_id", pUser.id)
         val teamQuery = teamDao.queryBuilder()
         teamQuery.where().`in`("_id", userTeamQuery)
         teamQuery.query()
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "There was a problem getting teams for the user: $pUser", e)
         emptyList()
      }
   }

   fun getTeamsByEvent(pEvent: Event): List<Team> {
      return try {
         val teamEventQuery = teamEventDao.queryBuilder()
         teamEventQuery.selectColumns("team_id")
         val where = teamEventQuery.where()
         where.eq("event_id", pEvent.id)
         val teamQuery = teamDao.queryBuilder()
         teamQuery.where().`in`("_id", teamEventQuery)
         teamQuery.query()
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "There was a problem getting teams for the event: $pEvent", e)
         emptyList()
      }
   }

   /**
    * Remove any teams from the database that are not in this team list.
    *
    * @param remoteTeams list of team that should remain in the database, all others will be removed
    */
   fun syncTeams(remoteTeams: Set<Team>) {
      try {
         val teamsToRemove = readAll()
         teamsToRemove.removeAll(remoteTeams)
         for (teamToRemove in teamsToRemove) {
            Log.e(LOG_NAME, "Removing team " + teamToRemove.name)
            val teamDeleteBuilder = teamEventDao.deleteBuilder()
            teamDeleteBuilder.where().eq("team_id", teamToRemove.id)
            teamDeleteBuilder.delete()
            val eventDeleteBuilder = teamDao.deleteBuilder()
            eventDeleteBuilder.where().idEq(teamToRemove.id)
            eventDeleteBuilder.delete()
         }
      } catch (e: Exception) {
         Log.e(LOG_NAME, "Error deleting event ", e)
      }
   }

   companion object {
      private val LOG_NAME = TeamLocalDataSource::class.java.name
   }
}