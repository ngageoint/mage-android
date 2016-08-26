package mil.nga.giat.mage.sdk.datastore.user;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
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
	private final Dao<UserLocal, Long> userLocalDao;
	private final Dao<UserTeam, Long> userTeamDao;

	private static Collection<IEventEventListener> listeners = new CopyOnWriteArrayList<>();
	
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
	 * @param context context
	 */
	private UserHelper(Context context) {
		super(context);

		try {
			userDao = daoStore.getUserDao();
			userLocalDao = daoStore.getUserLocalDao();
            userTeamDao = daoStore.getUserTeamDao();
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to communicate with User database.", sqle);

			throw new IllegalStateException("Unable to communicate with User database.", sqle);
		}
	}

    // FIXME : should add user to team if needed
	@Override
	public User create(User user) throws UserException {
		User createdUser;
		try {
			UserLocal userLocal = userLocalDao.createIfNotExists(new UserLocal());
			user.setUserLocal(userLocal);
			createdUser = userDao.createIfNotExists(user);
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "There was a problem creating user: " + user, sqle);
			throw new UserException("There was a problem creating user: " + user, sqle);
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
    public User read(String remoteId) throws UserException {
        User user = null;
        try {
            List<User> results = userDao.queryBuilder().where().eq("remote_id", remoteId).query();
            if (results != null && results.size() > 0) {
                user = results.get(0);
            }
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "Unable to query for existence for remote_id = '" + remoteId + "'", sqle);
            throw new UserException("Unable to query for existence for remote_id = '" + remoteId + "'", sqle);
        }
        return user;
    }

	public List<User> read(Collection<String> remoteIds) throws UserException {
		try {
			return userDao.queryBuilder().where().in("remote_id", remoteIds).query();
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to query for existence for remote_ids = '" + remoteIds + "'", sqle);
			throw new UserException("Unable to query for existence for remote_ids = '" + remoteIds.toString() + "'", sqle);
		}
	}

	public User readCurrentUser() throws UserException {
		User user;

		try {
			QueryBuilder<UserLocal, Long> userLocalQuery = userLocalDao.queryBuilder();
			userLocalQuery.selectColumns(UserLocal.COLUMN_NAME_ID);
			Where<UserLocal, Long> where = userLocalQuery.where();
			where.eq(UserLocal.COLUMN_NAME_CURRENT_USER, Boolean.TRUE);

			QueryBuilder<User, Long> userQuery = userDao.queryBuilder();
			userQuery.where().in(User.COLUMN_NAME_USER_LOCAL_ID, userLocalQuery);

			PreparedQuery<User> preparedQuery = userQuery.prepare();
			user = userDao.queryForFirst(preparedQuery);
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "There was a problem reading active users.");
			throw new UserException("There was a problem reading active users.", sqle);
		}

		return user;
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
			User user = readCurrentUser();
			status = isCurrentUserPartOfEvent(user.getCurrentEvent());
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem getting user or event.");
		}
		return status;
	}

	@Override
	public User update(User user) throws UserException {
		try {
			User oldUser = read(user.getId());
			user.setUserLocal(oldUser.getUserLocal());
			userDao.update(user);
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "There was a problem creating user: " + user);
			throw new UserException("There was a problem creating user: " + user, sqle);
		}

		return user;
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
				user.setUserLocal(oldUser.getUserLocal());
				userDao.update(user);
				Log.d(LOG_NAME, "Updated user with remote_id " + user.getRemoteId());
			}
		} catch (Exception ue) {
			Log.e(LOG_NAME, "There was a problem reading user: " + user, ue);
		}
		return user;
	}

	public User setCurrentUser(User user) throws UserException {
		try {
			clearCurrentUser();

			UpdateBuilder<UserLocal, Long> builder = userLocalDao.updateBuilder();
			builder.where().idEq(user.getUserLocal().getId());
			builder.updateColumnValue(UserLocal.COLUMN_NAME_CURRENT_USER, true);
			builder.update();

			userDao.refresh(user);
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to update user '" + user.getDisplayName() + "' to current user" , sqle);
			throw new UserException("Unable to update UserLocal table", sqle);
		}

		return user;
	}

	public User setCurrentEvent(User user, Event event) throws UserException {
		try {
			UpdateBuilder<UserLocal, Long> builder = userLocalDao.updateBuilder();
			builder.where().idEq(user.getUserLocal().getId());
			builder.updateColumnValue(UserLocal.COLUMN_NAME_CURRENT_EVENT, event);

			// check if we need to send event onChange
			UserLocal userLocal = user.getUserLocal();
			if (userLocal.isCurrentUser()) {
				String oldEventRemoteId = null;
				if (userLocal.getCurrentEvent() != null) {
					oldEventRemoteId = userLocal.getCurrentEvent().getRemoteId();
				}

				String newEventRemoteId = event != null ? event.getRemoteId() : null;

				// run update before firing event to make sure update works.
				builder.update();

				if (oldEventRemoteId == null ^ newEventRemoteId == null) {
					for (IEventEventListener listener : listeners) {
						listener.onEventChanged();
					}
				} else if (oldEventRemoteId != null && newEventRemoteId != null) {
					if (!oldEventRemoteId.equals(newEventRemoteId)) {
						for (IEventEventListener listener : listeners) {
							listener.onEventChanged();
						}
					}
				}

				userDao.refresh(user);
			}
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to update users '" + user.getDisplayName() + "' current event" , sqle);
			throw new UserException("Unable to update UserLocal table", sqle);
		}

		return user;
	}

	public User setAvatarPath(User user, String path) throws UserException {
		try {
			UpdateBuilder<UserLocal, Long> builder = userLocalDao.updateBuilder();
			builder.where().idEq(user.getUserLocal().getId());
			builder.updateColumnValue(UserLocal.COLUMN_NAME_AVATAR_PATH, path);
			builder.update();

			userDao.refresh(user);
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to update users '" + user.getDisplayName() + "' avatar path" , sqle);
			throw new UserException("Unable to update UserLocal table", sqle);
		}

		return user;
	}

	public User setIconPath(User user, String path) throws UserException {
		try {
			UpdateBuilder<UserLocal, Long> builder = userLocalDao.updateBuilder();
			builder.where().idEq(user.getUserLocal().getId());
			builder.updateColumnValue(UserLocal.COLUMN_NAME_ICON_PATH, path);
			builder.update();

			userDao.refresh(user);
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to update users '" + user.getDisplayName() + "' icon path" , sqle);
			throw new UserException("Unable to update UserLocal table", sqle);
		}

		return user;
	}

	private void clearCurrentUser() throws UserException {
		try {
			UpdateBuilder<UserLocal, Long> builder = userLocalDao.updateBuilder();
			builder.updateColumnValue(UserLocal.COLUMN_NAME_CURRENT_USER, Boolean.FALSE);
			builder.update();
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "There was a problem deleting active userlocal.", sqle);
			throw new UserException("There was a problem deleting active userlocal.", sqle);
		}
	}

	/**
	* Delete all users that are flagged as isCurrentUser.
	*
	* @throws UserException
	*             If current users can't be deleted.
	*/
	public void deleteCurrentUser() throws UserException {
		try {
			DeleteBuilder<UserLocal, Long> db = userLocalDao.deleteBuilder();
			db.where().eq(UserLocal.COLUMN_NAME_CURRENT_USER, Boolean.TRUE);
			db.delete();
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "There was a problem deleting active userlocal.", sqle);
			throw new UserException("There was a problem deleting active userlocal.", sqle);
		}
	}

    public void deleteUserTeams() {
        try {
            DeleteBuilder<UserTeam, Long> db = userTeamDao.deleteBuilder();
            db.delete();
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "There was a problem deleting userteams.", sqle);
        }
    }

    public UserTeam create(UserTeam userTeam) {
        UserTeam createdUserTeam = null;
        try {
            createdUserTeam = userTeamDao.createIfNotExists(userTeam);
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "There was a problem creating userteam: " + userTeam, sqle);
        }
        return createdUserTeam;
    }

    public Collection<User> getUsersByTeam(Team team) {
        Collection<User> users = new ArrayList<>();
        try {
            QueryBuilder<UserTeam, Long> userTeamQuery = userTeamDao.queryBuilder();
            userTeamQuery.selectColumns("user_id");
            Where<UserTeam, Long> where = userTeamQuery.where();
            where.eq("team_id", team.getId());

            QueryBuilder<User, Long> teamQuery = userDao.queryBuilder();
            teamQuery.where().in("_id", userTeamQuery);

            users = teamQuery.query();
            if(users == null) {
                users = new ArrayList<>();
            }

        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "There was a problem getting users for the team: " + team, sqle);
        }
        return users;
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
