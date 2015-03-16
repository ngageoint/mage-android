package mil.nga.giat.mage.sdk.datastore.layer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import mil.nga.giat.mage.sdk.datastore.DaoHelper;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeatureHelper;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeatureProperty;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.event.IEventDispatcher;
import mil.nga.giat.mage.sdk.event.ILayerEventListener;
import mil.nga.giat.mage.sdk.exceptions.EventException;
import mil.nga.giat.mage.sdk.exceptions.LayerException;
import mil.nga.giat.mage.sdk.exceptions.StaticFeatureException;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;

/**
 * A utility class for accessing {@link Layer} data from the physical data
 * model. The details of ORM DAOs and Lazy Loading should not be exposed past
 * this class.
 * 
 * @author wiedemanns
 * 
 */
public class LayerHelper extends DaoHelper<Layer> implements IEventDispatcher<ILayerEventListener> {

    private static final String LOG_NAME = LayerHelper.class.getName();

    private final Dao<Layer, Long> layerDao;

    private Collection<ILayerEventListener> listeners = new CopyOnWriteArrayList<ILayerEventListener>();
    
    private Context context;

    /**
     * Singleton.
     */
    private static LayerHelper mLayerHelper;

    /**
     * Use of a Singleton here ensures that an excessive amount of DAOs are not
     * created.
     * 
     * @param context
     *            Application Context
     * @return A fully constructed and operational LocationHelper.
     */
    public static LayerHelper getInstance(Context context) {
        if (mLayerHelper == null) {
            mLayerHelper = new LayerHelper(context);
        }
        return mLayerHelper;
    }

    /**
     * Only one-per JVM. Singleton.
     * 
     * @param context
     */
    private LayerHelper(Context context) {
        super(context);
        this.context = context;

        try {
            layerDao = daoStore.getLayerDao();
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "Unable to communicate with Layers database.", sqle);

            throw new IllegalStateException("Unable to communicate with Layers database.", sqle);
        }

    }

    public Collection<Layer> readAll() throws LayerException {
        List<Layer> layers = new ArrayList<Layer>();
        try {
            layers = layerDao.queryForAll();
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "Unable to read Layers", sqle);
            throw new LayerException("Unable to read Layers.", sqle);
        }
        return layers;
    }

	public Collection<Layer> readByEvent(Event pEvent) throws LayerException {
		List<Layer> layers = new ArrayList<Layer>();
		try {
			layers = layerDao.queryBuilder().where().eq("event_id", pEvent.getId()).query();
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to read Layers", sqle);
			throw new LayerException("Unable to read Layers.", sqle);
		}
		return layers;
	}

	@Override
	public Layer read(Long id) throws LayerException {
		try {
			return layerDao.queryForId(id);
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to query for existence for id = '" + id + "'", sqle);
			throw new LayerException("Unable to query for existence for id = '" + id + "'", sqle);
		}
	}

	@Override
	public Layer read(String pRemoteId) throws LayerException {
		Layer layer = null;
		try {
			List<Layer> results = layerDao.queryBuilder().where().eq("remote_id", pRemoteId).query();
			if (results != null && results.size() > 0) {
				layer = results.get(0);
			}
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to query for existence for remote_id = '" + pRemoteId + "'", sqle);
			throw new LayerException("Unable to query for existence for remote_id = '" + pRemoteId + "'", sqle);
		}

		return layer;
	}

    @Override
    public Layer create(Layer pLayer) throws LayerException {

        Layer createdLayer;
        try {
            createdLayer = layerDao.createIfNotExists(pLayer);
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "There was a problem creating the layer: " + pLayer + ".", sqle);
            throw new LayerException("There was a problem creating the layer: " + pLayer + ".", sqle);
        }
		// fire the event
		for (ILayerEventListener listener : listeners) {
			listener.onLayerCreated(pLayer);
		}

        return createdLayer;
    }

	@Override
	public Layer update(Layer pLayer) throws LayerException {
		try {
			layerDao.update(pLayer);
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "There was a problem updating layer: " + pLayer);
			throw new LayerException("There was a problem updating layer: " + pLayer, sqle);
		}
		return pLayer;
	}

	public void delete(Long pPrimaryKey) throws LayerException {
		try {
			Layer layer = layerDao.queryForId(pPrimaryKey);

			if(layer != null) {
				StaticFeatureHelper.getInstance(mApplicationContext).deleteAll(layer.getId());

				// finally, delete the Layer.
				layerDao.deleteById(pPrimaryKey);
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, "Unable to delete layer: " + pPrimaryKey, e);
			throw new LayerException("Unable to delete layer: " + pPrimaryKey, e);
		}
	}

    public void deleteAll() throws LayerException {
		for(Layer layer : readAll()) {
			delete(layer.getId());
		}
    }

    @Override
    public boolean addListener(ILayerEventListener listener) {
        return listeners.add(listener);
    }

    @Override
    public boolean removeListener(ILayerEventListener listener) {
        return listeners.remove(listener);
    }
}
