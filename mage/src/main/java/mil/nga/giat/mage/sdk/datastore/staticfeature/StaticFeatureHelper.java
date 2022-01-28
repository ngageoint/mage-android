package mil.nga.giat.mage.sdk.datastore.staticfeature;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.DeleteBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import mil.nga.giat.mage.sdk.datastore.DaoHelper;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.event.IEventDispatcher;
import mil.nga.giat.mage.sdk.event.IStaticFeatureEventListener;
import mil.nga.giat.mage.sdk.exceptions.StaticFeatureException;

public class StaticFeatureHelper extends DaoHelper<StaticFeature> implements IEventDispatcher<IStaticFeatureEventListener> {

	private static final String LOG_NAME = StaticFeatureHelper.class.getName();
	
	private final Context context;

	private final Dao<StaticFeature, Long> staticFeatureDao;
	private final Dao<StaticFeatureProperty, Long> staticFeaturePropertyDao;

	private final Collection<IStaticFeatureEventListener> listeners = new CopyOnWriteArrayList<IStaticFeatureEventListener>();

	/**
	 * Singleton.
	 */
	private static StaticFeatureHelper mStaticFeatureHelper;

	/**
	 * Use of a Singleton here ensures that an excessive amount of DAOs are not
	 * created.
	 * 
	 * @param context
	 *            Application Context
	 * @return A fully constructed and operational StaticFeatureHelper.
	 */
	public static StaticFeatureHelper getInstance(Context context) {
		if (mStaticFeatureHelper == null) {
			mStaticFeatureHelper = new StaticFeatureHelper(context);
		}
		return mStaticFeatureHelper;
	}

	/**
	 * Only one-per JVM. Singleton.
	 * 
	 * @param context
	 */
	private StaticFeatureHelper(Context context) {
		super(context);
		this.context = context;
		
		try {
			// Set up DAOs
			staticFeatureDao = daoStore.getStaticFeatureDao();
			staticFeaturePropertyDao = daoStore.getStaticFeaturePropertyDao();

		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to communicate with StaticFeature database.", sqle);

			throw new IllegalStateException("Unable to communicate with StaticFeature database.", sqle);
		}
	}

	@Override
	public StaticFeature create(StaticFeature pStaticFeature) throws StaticFeatureException {

		StaticFeature createdStaticFeature;
		try {
			createdStaticFeature = staticFeatureDao.createIfNotExists(pStaticFeature);
			// create Static Feature properties.
			Collection<StaticFeatureProperty> properties = pStaticFeature.getProperties();
			if (properties != null) {
				for (StaticFeatureProperty property : properties) {
					property.setStaticFeature(createdStaticFeature);
					staticFeaturePropertyDao.create(property);
				}
			}
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "There was a problem creating the static feature: " + pStaticFeature + ".", sqle);
			throw new StaticFeatureException("There was a problem creating the static feature: " + pStaticFeature + ".", sqle);
		}

		return createdStaticFeature;
	}

	@Override
	public StaticFeature update(StaticFeature pStaticFeature) throws Exception {
		throw new UnsupportedOperationException();
	}

	/**
	 * Set of layers that features were added to, or already belonged to.
	 * 
	 * @param staticFeatures
	 * @return
	 * @throws StaticFeatureException
	 */
	public Layer createAll(final Collection<StaticFeature> staticFeatures, final Layer pLayer) throws StaticFeatureException {

		try {
			TransactionManager.callInTransaction(DaoStore.getInstance(context).getConnectionSource(), new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					for (StaticFeature staticFeature : staticFeatures) {
						try {
							Collection<StaticFeatureProperty> properties = staticFeature.getProperties();
							staticFeature = staticFeatureDao.createIfNotExists(staticFeature);

							// create Static Feature properties.
							if (properties != null) {
								for (StaticFeatureProperty property : properties) {
									property.setStaticFeature(staticFeature);
									staticFeaturePropertyDao.create(property);
								}
							}
						} catch (SQLException sqle) {
							Log.e(LOG_NAME, "There was a problem creating the static feature: " + staticFeature + ".", sqle);
							continue;
							// TODO Throw exception?
						}
					}

					return null;
				}
			});
			pLayer.setLoaded(true);
			// fire the event
			for (IStaticFeatureEventListener listener : listeners) {
				listener.onStaticFeaturesCreated(pLayer);
			}
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "There was a problem creating static features.", sqle);
		}

		return pLayer;
	}

	@Override
	public StaticFeature read(Long id) throws StaticFeatureException {
		try {
			return staticFeatureDao.queryForId(id);
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to query for existence for id = '" + id + "'", sqle);
			throw new StaticFeatureException("Unable to query for existence for id = '" + id + "'", sqle);
		}
	}

    @Override
    public StaticFeature read(String pRemoteId) throws StaticFeatureException {
        StaticFeature staticFeature = null;
        try {
            List<StaticFeature> results = staticFeatureDao.queryBuilder().where().eq("remote_id", pRemoteId).query();
            if (results != null && results.size() > 0) {
                staticFeature = results.get(0);
            }
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "Unable to query for existence for remote_id = '" + pRemoteId + "'", sqle);
            throw new StaticFeatureException("Unable to query for existence for remote_id = '" + pRemoteId + "'", sqle);
        }

        return staticFeature;
    }

	public List<StaticFeature> readAll(Long pLayerId) throws StaticFeatureException {
		List<StaticFeature> staticFeatures = new ArrayList<StaticFeature>();
		try {
			List<StaticFeature> results = staticFeatureDao.queryBuilder().where().eq("layer_id", pLayerId).query();
			if (results != null) {
				staticFeatures.addAll(results);
			}
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to query for features with layer id = '" + pLayerId + "'", sqle);
			throw new StaticFeatureException("Unable to query for features with layer id = '" + pLayerId + "'", sqle);
		}

		return staticFeatures;
	}

	public StaticFeature readFeature(Long layerId, Long id) throws StaticFeatureException {
		StaticFeature staticFeature = null;
		try {
			List<StaticFeature> results = staticFeatureDao.queryBuilder()
					.where()
					.eq(StaticFeature.STATIC_FEATURE_LAYER_ID, layerId)
					.and()
					.eq(StaticFeature.STATIC_FEATURE_ID, id)
					.query();

			if (results != null && results.size() > 0) {
				staticFeature = results.get(0);
			}
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to query for feature with layer id: " + layerId + " and id: " + id, sqle);
			throw new StaticFeatureException("Unable to query for feature with layer id: " + layerId + " and id: " + id, sqle);
		}

		return staticFeature;
	}

	public void deleteAll(Long layerId) throws StaticFeatureException {
		List<StaticFeature> features = readAll(layerId);
		Collection<Long> ids = new ArrayList<>(features.size());
		for (StaticFeature feature : features) {
			ids.add(feature.getId());
		}

		try {
			// Delete the properties (children)
			DeleteBuilder propertyDeleteBuilder = staticFeaturePropertyDao.deleteBuilder();
			propertyDeleteBuilder.where().in(StaticFeatureProperty.STATIC_FEATURE_ID, ids);
			int propertiesDeleted = staticFeaturePropertyDao.delete(propertyDeleteBuilder.prepare());
			Log.i(LOG_NAME, propertiesDeleted + " static feature properties deleted");

			// All children deleted, delete the static feature.
			DeleteBuilder featureDeleteBuilder = staticFeatureDao.deleteBuilder();
			featureDeleteBuilder.where().eq(StaticFeature.STATIC_FEATURE_LAYER_ID, layerId);
			int featuresDeleted = staticFeatureDao.delete(featureDeleteBuilder.prepare());
			Log.i(LOG_NAME, featureDeleteBuilder + " features deleted");
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to delete Static Feature: " + ids, sqle);
			throw new StaticFeatureException("Unable to delete Static Feature: " + ids, sqle);
		}
	}

	@Override
	public boolean addListener(IStaticFeatureEventListener listener) {
		return listeners.add(listener);
	}

	@Override
	public boolean removeListener(IStaticFeatureEventListener listener) {
		return listeners.remove(listener);
	}
}
