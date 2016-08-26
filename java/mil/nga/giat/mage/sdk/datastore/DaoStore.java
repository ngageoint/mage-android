package mil.nga.giat.mage.sdk.datastore;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.location.LocationProperty;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationFavorite;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationImportant;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeatureProperty;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.Role;
import mil.nga.giat.mage.sdk.datastore.user.Team;
import mil.nga.giat.mage.sdk.datastore.user.TeamEvent;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserLocal;
import mil.nga.giat.mage.sdk.datastore.user.UserTeam;

/**
 * This is an implementation of OrmLite android database Helper. Go here to get
 * daos that you may need. Manage your table creation and update strategies here
 * as well.
 * 
 * @author travis, wiedemanns
 * 
 */
public class DaoStore extends OrmLiteSqliteOpenHelper {

	private static DaoStore helperInstance;

	private static final String DATABASE_NAME = "mage.db";
	private static final String LOG_NAME = DaoStore.class.getName();
	// Making this public so we can check if it has been upgraded and log the user out
	public static final int DATABASE_VERSION = 11;

	// Observation DAOS
	private Dao<Observation, Long> observationDao;
	private Dao<ObservationProperty, Long> observationPropertyDao;
	private Dao<ObservationImportant, Long> observationImportantDao;
	private Dao<ObservationFavorite, Long> observationFavoriteDao;
	private Dao<Attachment, Long> attachmentDao;

	// User and Location DAOS
	private Dao<User, Long> userDao;
	private Dao<Role, Long> roleDao;
    private Dao<Event, Long> eventDao;
    private Dao<Team, Long> teamDao;
	private Dao<UserLocal, Long> userLocalDao;
    private Dao<UserTeam, Long> userTeamDao;
    private Dao<TeamEvent, Long> teamEventDao;
	private Dao<Location, Long> locationDao;
	private Dao<LocationProperty, Long> locationPropertyDao;
	
	// Layer and StaticFeature DAOS
	private Dao<Layer, Long> layerDao;
	private Dao<StaticFeature, Long> staticFeatureDao;
	private Dao<StaticFeatureProperty, Long> staticFeaturePropertyDao;
	
	/**
	 * Singleton implementation.
	 * 
	 * @param context context
	 * @return the dao store
	 */
	public static DaoStore getInstance(Context context) {
		if (helperInstance == null) {
			helperInstance = new DaoStore(context);
		}
		return helperInstance;
	}

	/**
	 * Constructor that takes an android Context.
	 * 
	 * @param context context
	 *
	 */
	private DaoStore(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);

		// initialize DAOs
		try {
			getObservationDao();
			getObservationPropertyDao();
			getObservationImportantDao();
			getObservationFavoriteDao();
			getAttachmentDao();
			getUserDao();
			getUserLocalDao();
			getRoleDao();
            getEventDao();
            getTeamDao();
            getUserTeamDao();
            getTeamEventDao();
			getLocationDao();
			getLocationPropertyDao();
			getLayerDao();
			getStaticFeatureDao();
			getStaticFeaturePropertyDao();
		} catch (SQLException sqle) {
			// TODO: handle this...
			sqle.printStackTrace();
		}

	}

	public boolean isDatabaseEmpty() {
		long countOfAllRecords = 0l;
		try {
			countOfAllRecords += getObservationDao().countOf();
			countOfAllRecords += getObservationPropertyDao().countOf();
			countOfAllRecords += getObservationImportantDao().countOf();
			countOfAllRecords += getObservationFavoriteDao().countOf();
			countOfAllRecords += getAttachmentDao().countOf();
			countOfAllRecords += getUserDao().countOf();
			countOfAllRecords += getUserLocalDao().countOf();
			countOfAllRecords += getRoleDao().countOf();
            countOfAllRecords += getEventDao().countOf();
            countOfAllRecords += getTeamDao().countOf();
            countOfAllRecords += getUserTeamDao().countOf();
            countOfAllRecords += getTeamEventDao().countOf();
			countOfAllRecords += getLocationDao().countOf();
			countOfAllRecords += getLocationPropertyDao().countOf();
			countOfAllRecords += getLayerDao().countOf();
			countOfAllRecords += getStaticFeatureDao().countOf();
			countOfAllRecords += getStaticFeaturePropertyDao().countOf();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
			return false;
		}
		return countOfAllRecords == 0;
	}

	private void createTables() throws SQLException {
		TableUtils.createTable(connectionSource, Observation.class);
		TableUtils.createTable(connectionSource, ObservationProperty.class);
		TableUtils.createTable(connectionSource, ObservationImportant.class);
		TableUtils.createTable(connectionSource, ObservationFavorite.class);
		TableUtils.createTable(connectionSource, Attachment.class);

		TableUtils.createTable(connectionSource, User.class);
		TableUtils.createTable(connectionSource, UserLocal.class);
		TableUtils.createTable(connectionSource, Role.class);
        TableUtils.createTable(connectionSource, Event.class);
        TableUtils.createTable(connectionSource, Team.class);
        TableUtils.createTable(connectionSource, UserTeam.class);
        TableUtils.createTable(connectionSource, TeamEvent.class);
		TableUtils.createTable(connectionSource, Location.class);
		TableUtils.createTable(connectionSource, LocationProperty.class);
		
		TableUtils.createTable(connectionSource, Layer.class);
		TableUtils.createTable(connectionSource, StaticFeature.class);
		TableUtils.createTable(connectionSource, StaticFeatureProperty.class);
	}

	@Override
	public void onCreate(SQLiteDatabase sqliteDatabase, ConnectionSource connectionSource) {
		try {
			createTables();
		} catch (SQLException se) {
			Log.e(LOG_NAME, "Could not create tables.", se);
		}
	}

	private void dropTables() throws SQLException {
		TableUtils.dropTable(connectionSource, Observation.class, Boolean.TRUE);

		TableUtils.dropTable(connectionSource, ObservationProperty.class, Boolean.TRUE);
		TableUtils.dropTable(connectionSource, ObservationImportant.class, Boolean.TRUE);
		TableUtils.dropTable(connectionSource, ObservationFavorite.class, Boolean.TRUE);
		TableUtils.dropTable(connectionSource, Attachment.class, Boolean.TRUE);

		TableUtils.dropTable(connectionSource, User.class, Boolean.TRUE);
		TableUtils.dropTable(connectionSource, UserLocal.class, Boolean.TRUE);

		TableUtils.dropTable(connectionSource, Role.class, Boolean.TRUE);
        TableUtils.dropTable(connectionSource, Event.class, Boolean.TRUE);
        TableUtils.dropTable(connectionSource, Team.class, Boolean.TRUE);
        TableUtils.dropTable(connectionSource, UserTeam.class, Boolean.TRUE);
        TableUtils.dropTable(connectionSource, TeamEvent.class, Boolean.TRUE);
		TableUtils.dropTable(connectionSource, Location.class, Boolean.TRUE);
		TableUtils.dropTable(connectionSource, LocationProperty.class, Boolean.TRUE);
		
		TableUtils.dropTable(connectionSource, Layer.class, Boolean.TRUE);
		TableUtils.dropTable(connectionSource, StaticFeature.class, Boolean.TRUE);
		TableUtils.dropTable(connectionSource, StaticFeatureProperty.class, Boolean.TRUE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
		resetDatabase();
	}

	/**
	 * Drop and create all tables.
	 */
	public void resetDatabase() {
		try {
			Log.d(LOG_NAME, "Reseting Database.");
			dropTables();
			createTables();
			Log.d(LOG_NAME, "Reset Database.");
		} catch (SQLException se) {
			Log.e(LOG_NAME, "Could not reset Database.", se);
		}
	}

	@Override
	public void close() {
		helperInstance = null;
		super.close();
	}

	/**
	 * Getter for the ObservationDao.
	 * 
	 * @return This instance's ObservationDao
	 * @throws SQLException
	 */
	public Dao<Observation, Long> getObservationDao() throws SQLException {
		if (observationDao == null) {
			observationDao = getDao(Observation.class);
		}
		return observationDao;
	}

	/**
	 * Getter for the PropertyDao
	 * 
	 * @return This instance's PropertyDao
	 * @throws SQLException
	 */
	public Dao<ObservationProperty, Long> getObservationPropertyDao() throws SQLException {
		if (observationPropertyDao == null) {
			observationPropertyDao = getDao(ObservationProperty.class);
		}
		return observationPropertyDao;
	}

	/**
	 * Getter for the ObservationImportantDao
	 *
	 * @return This instance's ObservationImportantDao
	 * @throws SQLException
	 */
	public Dao<ObservationImportant, Long> getObservationImportantDao() throws SQLException {
		if (observationImportantDao == null) {
			observationImportantDao = getDao(ObservationImportant.class);
		}
		return observationImportantDao;
	}

	/**
	 * Getter for the ObservationFavoriteDao
	 *
	 * @return This instance's ObservationFavoriteDao
	 * @throws SQLException
	 */
	public Dao<ObservationFavorite, Long> getObservationFavoriteDao() throws SQLException {
		if (observationFavoriteDao == null) {
			observationFavoriteDao = getDao(ObservationFavorite.class);
		}
		return observationFavoriteDao;
	}

	/**
	 * Getter for the AttachmentDao
	 * 
	 * @return This instance's AttachmentDao
	 * @throws SQLException
	 */
	public Dao<Attachment, Long> getAttachmentDao() throws SQLException {
		if (attachmentDao == null) {
			attachmentDao = getDao(Attachment.class);
		}
		return attachmentDao;
	}

	/**
	 * Getter for the UserDao
	 * 
	 * @return This instance's UserDao
	 * @throws SQLException
	 */
	public Dao<User, Long> getUserDao() throws SQLException {
		if (userDao == null) {
			userDao = getDao(User.class);
		}
		return userDao;
	}

	/**
	 * Getter for the UserLocalDao
	 *
	 * @return This instance's UserLocalDao
	 * @throws SQLException
	 */
	public Dao<UserLocal, Long> getUserLocalDao() throws SQLException {
		if (userLocalDao == null) {
			userLocalDao = getDao(UserLocal.class);
		}
		return userLocalDao;
	}


	/**
	 * Getter for the RoleDao
	 * 
	 * @return This instance's RoleDao
	 * @throws SQLException
	 */
	public Dao<Role, Long> getRoleDao() throws SQLException {
		if (roleDao == null) {
			roleDao = getDao(Role.class);
		}
		return roleDao;
	}

    /**
     * Getter for the EventDao
     *
     * @return This instance's EventDao
     * @throws SQLException
     */
    public Dao<Event, Long> getEventDao() throws SQLException {
        if (eventDao == null) {
            eventDao = getDao(Event.class);
        }
        return eventDao;
    }

    /**
     * Getter for the TeamDao
     *
     * @return This instance's TeamDao
     * @throws SQLException
     */
    public Dao<Team, Long> getTeamDao() throws SQLException {
        if (teamDao == null) {
            teamDao = getDao(Team.class);
        }
        return teamDao;
    }

    /**
     * Getter for the UserTeamDao
     *
     * @return This instance's UserTeamDao
     * @throws SQLException
     */
    public Dao<UserTeam, Long> getUserTeamDao() throws SQLException {
        if (userTeamDao == null) {
            userTeamDao = getDao(UserTeam.class);
        }
        return userTeamDao;
    }

    /**
     * Getter for the TeamEventDao
     *
     * @return This instance's TeamEventDao
     * @throws SQLException
     */
    public Dao<TeamEvent, Long> getTeamEventDao() throws SQLException {
        if (teamEventDao == null) {
            teamEventDao = getDao(TeamEvent.class);
        }
        return teamEventDao;
    }

	/**
	 * Getter for the LocationDao
	 * 
	 * @return This instance's LocationDao
	 * @throws SQLException
	 */
	public Dao<Location, Long> getLocationDao() throws SQLException {
		if (locationDao == null) {
			locationDao = getDao(Location.class);
		}
		return locationDao;
	}

	/**
	 * Getter for the LocationPropertyDao
	 * 
	 * @return This instance's LocationPropertyDao
	 * @throws SQLException
	 */
	public Dao<LocationProperty, Long> getLocationPropertyDao() throws SQLException {
		if (locationPropertyDao == null) {
			locationPropertyDao = getDao(LocationProperty.class);
		}
		return locationPropertyDao;
	}
	
	/**
	 * Getter for the LayerDao
	 * 
	 * @return This instance's LayerDao
	 * @throws SQLException
	 */
	public Dao<Layer, Long> getLayerDao() throws SQLException {
		if (layerDao == null) {
			layerDao = getDao(Layer.class);
		}
		return layerDao;
	}
	
	/**
	 * Getter for the StaticFeatureDao
	 * 
	 * @return This instance's StaticFeatureDao
	 * @throws SQLException
	 */
	public Dao<StaticFeature, Long> getStaticFeatureDao() throws SQLException {
		if (staticFeatureDao == null) {
			staticFeatureDao = getDao(StaticFeature.class);
		}
		return staticFeatureDao;
	}
	
	/**
	 * Getter for the StaticFeaturePropertyDao
	 * 
	 * @return This instance's StaticFeaturePropertyDao
	 * @throws SQLException
	 */
	public Dao<StaticFeatureProperty, Long> getStaticFeaturePropertyDao() throws SQLException {
		if (staticFeaturePropertyDao == null) {
			staticFeaturePropertyDao = getDao(StaticFeatureProperty.class);
		}
		return staticFeaturePropertyDao;
	}
}
