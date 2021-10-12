package mil.nga.giat.mage.sdk.datastore.user;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mil.nga.giat.mage.sdk.datastore.DaoHelper;
import mil.nga.giat.mage.sdk.exceptions.RoleException;

/**
 * 
 * A utility class for accessing {@link Role} data from the physical data model.
 * The details of ORM DAOs and Lazy Loading should not be exposed past this
 * class.
 * 
 * @author wiedemanns
 * 
 */
public class RoleHelper extends DaoHelper<Role> {

	private static final String LOG_NAME = RoleHelper.class.getName();

	private final Dao<Role, Long> roleDao;

	/**
	 * Singleton.
	 */
	private static RoleHelper mRoleHelper;

	/**
	 * Use of a Singleton here ensures that an excessive amount of DAOs are not
	 * created.
	 * 
	 * @param context
	 *            Application Context
	 * @return A fully constructed and operational {@link RoleHelper}.
	 */
	public static RoleHelper getInstance(Context context) {
		if (mRoleHelper == null) {
			mRoleHelper = new RoleHelper(context);
		}
		return mRoleHelper;
	}

	/**
	 * Only one-per JVM. Singleton.
	 * 
	 * @param pContext
	 */
	private RoleHelper(Context pContext) {
		super(pContext);

		try {
			roleDao = daoStore.getRoleDao();
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to communicate with Role database.", sqle);

			throw new IllegalStateException("Unable to communicate with Role database.", sqle);
		}

	}

	@Override
	public Role create(Role pRole) throws RoleException {
		Role createdRole = null;
		try {
			createdRole = roleDao.createIfNotExists(pRole);
            Log.d(LOG_NAME, "created role with remote_id " + createdRole.getRemoteId());
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "There was a problem creating the role: " + pRole);
			throw new RoleException("There was a problem creating the role: " + pRole, sqle);
		}
		return createdRole;
	}

    public Role update(Role pRole) throws RoleException {
        try {
            roleDao.update(pRole);
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "There was a problem creating role: " + pRole);
            throw new RoleException("There was a problem creating role: " + pRole, sqle);
        }
		return pRole;
    }

    public Role createOrUpdate(Role role) {
        try {
            Role oldRole = read(role.getRemoteId());
            if (oldRole == null) {
                role = create(role);
                Log.d(LOG_NAME, "Created role with remote_id " + role.getRemoteId());
            } else {
                // perform update?
                role.setId(oldRole.getId());
                update(role);
                Log.d(LOG_NAME, "Updated role with remote_id " + role.getRemoteId());
            }
        } catch (RoleException re) {
            Log.e(LOG_NAME, "There was a problem reading role: " + role, re);
        }
        return role;
    }
	
	public void deleteAll() throws RoleException {
		try {
			DeleteBuilder<Role, Long> db = roleDao.deleteBuilder();
			db.delete();
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "There was a problem deleting all roles.", sqle);
			throw new RoleException("There was a problem deleting all roles.", sqle);
		}
	}

	@Override
	public Role read(Long id) throws RoleException {
		try {
			return roleDao.queryForId(id);
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to query for existence for id = '" + id + "'", sqle);
			throw new RoleException("Unable to query for existence for id = '" + id + "'", sqle);
		}

	}

	public Role readAdmin() throws RoleException {
		Role adminRole = null;
		try {
			QueryBuilder<Role, Long> queryBuilder = roleDao.queryBuilder();
			queryBuilder.where().eq("name", Role.ADMIN_ROLE_NAME);
			adminRole = roleDao.queryForFirst(queryBuilder.prepare());
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to query for " + Role.ADMIN_ROLE_NAME + ".", sqle);
			throw new RoleException("Unable to query for " + Role.ADMIN_ROLE_NAME + ".", sqle);
		}
		return adminRole;
	}
	
    @Override
    public Role read(String pRemoteId) throws RoleException {
        Role role = null;
        try {
            List<Role> results = roleDao.queryBuilder().where().eq("remote_id", pRemoteId).query();
            if (results != null && results.size() > 0) {
                role = results.get(0);
            }
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "Unable to query for existence for remote_id = '" + pRemoteId + "'", sqle);
            throw new RoleException("Unable to query for existence for remote_id = '" + pRemoteId + "'", sqle);
        }
        return role;
    }

	public Collection<Role> readAll() throws RoleException {
		Collection<Role> roles = new ArrayList<>();
		try {
			roles.addAll(roleDao.queryForAll());
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to read Observations", sqle);
			throw new RoleException("Unable to read Roles.", sqle);
		}

		return roles;
	}
}
