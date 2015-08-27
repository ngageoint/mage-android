package mil.nga.giat.mage.sdk.datastore.user;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import mil.nga.giat.mage.sdk.datastore.DaoHelper;
import mil.nga.giat.mage.sdk.exceptions.EventException;
import mil.nga.giat.mage.sdk.exceptions.TeamException;

/**
 * A utility class for accessing {@link Team} data from the physical data model.
 * The details of ORM DAOs and Lazy Loading should not be exposed past this
 * class.
 *
 * @author wiedemanns
 *
 */
public class TeamHelper extends DaoHelper<Team> {

    private static final String LOG_NAME = TeamHelper.class.getName();

    private final Dao<Team, Long> teamDao;
    private final Dao<UserTeam, Long> userTeamDao;
    private final Dao<TeamEvent, Long> teamEventDao;

    /**
     * Singleton.
     */
    private static TeamHelper mTeamHelper;

    /**
     * Use of a Singleton here ensures that an excessive amount of DAOs are not
     * created.
     *
     * @param context
     *            Application Context
     * @return A fully constructed and operational UserHelper.
     */
    public static TeamHelper getInstance(Context context) {
        if (mTeamHelper == null) {
            mTeamHelper = new TeamHelper(context);
        }
        return mTeamHelper;
    }

    /**
     * Only one-per JVM. Singleton.
     *
     * @param pContext
     */
    private TeamHelper(Context pContext) {
        super(pContext);

        try {
            teamDao = daoStore.getTeamDao();
            userTeamDao = daoStore.getUserTeamDao();
            teamEventDao = daoStore.getTeamEventDao();
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "Unable to communicate with Team database.", sqle);

            throw new IllegalStateException("Unable to communicate with Team database.", sqle);
        }

    }

    @Override
    public Team create(Team pTeam) throws TeamException {
        Team createdTeam = null;
        try {
            createdTeam = teamDao.createIfNotExists(pTeam);
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "There was a problem creating team: " + pTeam, sqle);
            throw new TeamException("There was a problem creating team: " + pTeam, sqle);
        }
        return createdTeam;
    }

    @Override
    public Team read(Long id) throws TeamException {
        try {
            return teamDao.queryForId(id);
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "Unable to query for existence for id = '" + id + "'", sqle);
            throw new TeamException("Unable to query for existence for id = '" + id + "'", sqle);
        }
    }

    public List<Team> readAll() throws EventException {
        List<Team> teams = new ArrayList<Team>();
        try {
            teams.addAll(teamDao.queryForAll());
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "Unable to read Teams", sqle);
            throw new EventException("Unable to read Teams.", sqle);
        }
        return teams;
    }

    @Override
    public Team read(String pRemoteId) throws TeamException {
        Team team = null;
        try {
            List<Team> results = teamDao.queryBuilder().where().eq("remote_id", pRemoteId).query();
            if (results != null && results.size() > 0) {
                team = results.get(0);
            }
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "Unable to query for existence for remote_id = '" + pRemoteId + "'", sqle);
            throw new TeamException("Unable to query for existence for remote_id = '" + pRemoteId + "'", sqle);
        }
        return team;
    }

	@Override
    public Team update(Team pTeam) throws TeamException {
        try {
            teamDao.update(pTeam);
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "There was a problem creating team: " + pTeam);
            throw new TeamException("There was a problem creating team: " + pTeam, sqle);
        }
		return pTeam;
    }

    public Team createOrUpdate(Team team) {
        try {
            Team oldTeam = read(team.getRemoteId());
            if (oldTeam == null) {
                team = create(team);
                Log.d(LOG_NAME, "Created team with remote_id " + team.getRemoteId());
            } else {
                // perform update?
                team.setId(oldTeam.getId());
                update(team);
                Log.d(LOG_NAME, "Updated team with remote_id " + team.getRemoteId());
            }
        } catch (TeamException te) {
            Log.e(LOG_NAME, "There was a problem reading team: " + team, te);
        }
        return team;
    }

    public void deleteTeamEvents() {
        try {
            DeleteBuilder<TeamEvent, Long> db = teamEventDao.deleteBuilder();
            db.delete();
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "There was a problem deleting teamevents.", sqle);
        }
    }

    public TeamEvent create(TeamEvent pTeamEvent) {
        TeamEvent createdTeamEvent = null;
        try {
            createdTeamEvent = teamEventDao.createIfNotExists(pTeamEvent);
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "There was a problem creating teamevent: " + pTeamEvent, sqle);
        }
        return createdTeamEvent;
    }

    public List<Team> getTeamsByUser(User pUser) {
        List<Team> teams = new ArrayList<Team>();
        try {
            QueryBuilder<UserTeam, Long> userTeamQuery = userTeamDao.queryBuilder();
            userTeamQuery.selectColumns("team_id");
            Where<UserTeam, Long> where = userTeamQuery.where();
            where.eq("user_id", pUser.getId());

            QueryBuilder<Team, Long> teamQuery = teamDao.queryBuilder();
            teamQuery.where().in("_id", userTeamQuery);

            teams = teamQuery.query();
            if(teams == null) {
                teams = new ArrayList<Team>();
            }

        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "There was a problem getting teams for the user: " + pUser, sqle);
        }
        return teams;
    }

    public List<Team> getTeamsByEvent(User pEvent) {
        List<Team> teams = new ArrayList<Team>();
        try {
            QueryBuilder<TeamEvent, Long> teamEventQuery = teamEventDao.queryBuilder();
            teamEventQuery.selectColumns("team_id");
            Where<TeamEvent, Long> where = teamEventQuery.where();
            where.eq("event_id", pEvent.getId());

            QueryBuilder<Team, Long> teamQuery = teamDao.queryBuilder();
            teamQuery.where().in("_id", teamEventQuery);

            teams = teamQuery.query();
            if(teams == null) {
                teams = new ArrayList<Team>();
            }

        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "There was a problem getting teams for the event: " + pEvent, sqle);
        }
        return teams;
    }

    /**
     * Remove any teams from the database that are not in this team list.
     *
     * @param remoteTeams list of team that should remain in the database, all others will be removed
     */
    public void syncTeams(Set<Team> remoteTeams) {
        try {
            List<Team> teamsToRemove = readAll();
            teamsToRemove.removeAll(remoteTeams);

            for (Team teamToRemove : teamsToRemove) {
                Log.e(LOG_NAME, "Removing team " + teamToRemove.getName());

                DeleteBuilder<TeamEvent, Long> teamDeleteBuilder = teamEventDao.deleteBuilder();
                teamDeleteBuilder.where().eq("team_id", teamToRemove.getId());
                teamDeleteBuilder.delete();

                DeleteBuilder<Team, Long> eventDeleteBuilder = teamDao.deleteBuilder();
                eventDeleteBuilder.where().idEq(teamToRemove.getId());
                eventDeleteBuilder.delete();
            }
        } catch (Exception e) {
            Log.e(LOG_NAME, "Error deleting event ", e);
        }
    }
}
