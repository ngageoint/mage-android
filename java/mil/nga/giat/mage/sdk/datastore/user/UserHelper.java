package mil.nga.giat.mage.sdk.datastore.user;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import mil.nga.giat.mage.sdk.datastore.DaoHelper;
import mil.nga.giat.mage.sdk.event.IEventDispatcher;
import mil.nga.giat.mage.sdk.event.IEventEventListener;
import mil.nga.giat.mage.sdk.exceptions.UserException;

/**
 * A utility class for accessing {@link User} data from the physical data model.
 * The details of ORM DAOs and Lazy Loading should not be exposed past this
 * class.
 * 
 * @author wiedemanns
 * 
 */
public class UserHelper extends DaoHelper<User> implements IEventDispatcher<IEventEventListener> {

	private static final String LOG_NAME = UserHelper.class.getName();

	private final Dao<User, Long> userDao;
    private final Dao<UserTeam, Long> userTeamDao;

	private static Collection<IEventEventListener> listeners = new CopyOnWriteArrayList<IEventEventListener>();
	
	/**
	 * Singleton.
	 */
	private static UserHelper mUserHelper;

	/**
	 * Use of a Singleton here ensures that an excessive amount of DAOs are not
	 * created.
	 * 
	 * @param context
	 *            Application Context
	 * @return A fully constructed and operational UserHelper.
	 */
	public static UserHelper getInstance(Context context) {
		if (mUserHelper == null) {
			mUserHelper = new UserHelper(context);
		}
		return mUserHelper;
	}

	/**
	 * Only one-per JVM. Singleton.
	 * 
	 * @param pContext
	 */
	private UserHelper(Context pContext) {
		super(pContext);

		try {
			userDao = daoStore.getUserDao();
            userTeamDao = daoStore.getUserTeamDao();
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to communicate with User database.", sqle);

			throw new IllegalStateException("Unable to communicate with User database.", sqle);
		}
	}

    // FIXME : should add user to team if needed
	@Override
	public User create(User pUser) throws UserException {
		User createdUser = null;
		try {
			createdUser = userDao.createIfNotExists(pUser);

			if(createdUser.isCurrentUser()) {
				for (IEventEventListener listener : listeners) {
					listener.onEventChanged();
				}
			}
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "There was a problem creating user: " + pUser, sqle);
			throw new UserException("There was a problem creating user: " + pUser, sqle);
		}
		return createdUser;
	}

	@Override
	public User read(Long id) throws UserException {
		try {
			return userDao.queryForId(id);
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to query for existence for id = '" + id + "'", sqle);
			throw new UserException("Unable to query for existence for id = '" + id + "'", sqle);
		}
	}
	
    @Override
    public User read(String pRemoteId) throws UserException {
        User user = null;
        try {
            List<User> results = userDao.queryBuilder().where().eq("remote_id", pRemoteId).query();
            if (results != null && results.size() > 0) {
                user = results.get(0);
            }
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "Unable to query for existence for remote_id = '" + pRemoteId + "'", sqle);
            throw new UserException("Unable to query for existence for remote_id = '" + pRemoteId + "'", sqle);
        }
        return user;
    }

	@Override
	public User update(User pUser) throws UserException {

		try {
			// check if we need to send event onChange
			if(pUser.isCurrentUser()) {
				User oldUser = read(pUser.getRemoteId());
				String oldEventRemoteId = null;
				if(oldUser != null && oldUser.getCurrentEvent() != null) {
					oldEventRemoteId = oldUser.getCurrentEvent().getRemoteId();
				}
				userDao.update(pUser);

				String newEventRemoteId = null;
				if(pUser != null && pUser.getCurrentEvent() != null) {
					newEventRemoteId = pUser.getCurrentEvent().getRemoteId();
				}

				if(oldEventRemoteId == null ^ newEventRemoteId == null) {
					for (IEventEventListener listener : listeners) {
						listener.onEventChanged();
					}
				} else if(oldEventRemoteId != null && newEventRemoteId != null) {
					if(!oldEventRemoteId.equals(newEventRemoteId)) {
						for (IEventEventListener listener : listeners) {
							listener.onEventChanged();
						}
					}
				}

			} else {
				userDao.update(pUser);
			}
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "There was a problem creating user: " + pUser);
			throw new UserException("There was a problem creating user: " + pUser, sqle);
		}
		return pUser;
	}

	/**
	 * This method is used to read current Active Users from the database. An
	 * active user is one that is currently logged into the client and is
	 * presumably the user consuming Location Services.
	 * 
	 * @return A List of Users that are flagged as active in the datastore.
	 * @throws UserException
	 *             Indicates a problem reading users from the datastore.
	 */
	public User readCurrentUser() throws UserException {

		User currentUser = null;
		try {
			QueryBuilder<User, Long> qb = userDao.queryBuilder();
			Where<User, Long> where = qb.where();
			where.eq("isCurrentUser", Boolean.TRUE);
			PreparedQuery<User> preparedQuery = qb.prepare();
			currentUser = userDao.queryForFirst(preparedQuery);
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "There was a problem reading active users.");
			throw new UserException("There was a problem reading active users.", sqle);
		}

		return currentUser;
	}

	/**
	 * Delete all users that are flagged as isCurrentUser.
	 * 
	 * @throws UserException
	 *             If current users can't be deleted.
	 */
	public void deleteCurrentUsers() throws UserException {

		try {
			DeleteBuilder<User, Long> db = userDao.deleteBuilder();
			db.where().eq("isCurrentUser", Boolean.TRUE);
			db.delete();
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "There was a problem deleting active users.", sqle);
			throw new UserException("There was a problem deleting active users.", sqle);
		}
	}
	
	public User createOrUpdate(User user) {
		try {
			User oldUser = read(user.getRemoteId());
			if (oldUser == null) {
				user = create(user);
				Log.d(LOG_NAME, "Created user with remote_id " + user.getRemoteId());
			} else {
				// perform update?
				user.setId(oldUser.getId());
				update(user);
				Log.d(LOG_NAME, "Updated user with remote_id " + user.getRemoteId());
			}
		} catch (UserException ue) {
			Log.e(LOG_NAME, "There was a problem reading user: " + user, ue);
		}
		return user;
	}

    public void deleteUserTeams() {
        try {
            DeleteBuilder<UserTeam, Long> db = userTeamDao.deleteBuilder();
            db.delete();
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "There was a problem deleting userteams.", sqle);
        }
    }

    public UserTeam create(UserTeam pUserTeam) {
        UserTeam createdUserTeam = null;
        try {
            createdUserTeam = userTeamDao.createIfNotExists(pUserTeam);
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "There was a problem creating userteam: " + pUserTeam, sqle);
        }
        return createdUserTeam;
    }

    public Collection<User> getUsersByTeam(Team pTeam) {
        Collection<User> users = new ArrayList<User>();
        try {
            QueryBuilder<UserTeam, Long> userTeamQuery = userTeamDao.queryBuilder();
            userTeamQuery.selectColumns("user_id");
            Where<UserTeam, Long> where = userTeamQuery.where();
            where.eq("team_id", pTeam.getId());

            QueryBuilder<User, Long> teamQuery = userDao.queryBuilder();
            teamQuery.where().in("_id", userTeamQuery);

            users = teamQuery.query();
            if(users == null) {
                users = new ArrayList<User>();
            }

        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "There was a problem getting users for the team: " + pTeam, sqle);
        }
        return users;
    }

	public boolean isCurrentUserPartOfEvent(Event event) {
		boolean status = false;

		try {
			status = EventHelper.getInstance(mApplicationContext).getEventsForCurrentUser().contains(event);
		} catch(Exception e) {
			Log.e(LOG_NAME, "Problem getting user or event.");
		}
		return status;
	}

	public boolean isCurrentUserPartOfCurrentEvent() {
		boolean status = false;
		try {
			status = isCurrentUserPartOfEvent(readCurrentUser().getCurrentEvent());
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem getting user or event.");
		}
		return status;
	}

	@Override
	public boolean addListener(IEventEventListener listener) {
		return listeners.add(listener);
	}

	@Override
	public boolean removeListener(IEventEventListener listener) {
		return listeners.remove(listener);
	}
}
