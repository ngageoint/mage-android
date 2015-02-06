package mil.nga.giat.mage.sdk.datastore.user;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mil.nga.giat.mage.sdk.datastore.DaoHelper;
import mil.nga.giat.mage.sdk.exceptions.TeamException;
import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

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

    public void update(Team pTeam) throws TeamException {
        try {
            teamDao.update(pTeam);
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "There was a problem creating team: " + pTeam);
            throw new TeamException("There was a problem creating team: " + pTeam, sqle);
        }
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
                Log.d(LOG_NAME, "Updated user with remote_id " + team.getRemoteId());
            }
        } catch (TeamException te) {
            Log.e(LOG_NAME, "There was a problem reading user: " + team, te);
        }
        return team;
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
}
