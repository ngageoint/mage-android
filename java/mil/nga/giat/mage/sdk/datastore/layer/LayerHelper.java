package mil.nga.giat.mage.sdk.datastore.layer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import mil.nga.giat.mage.sdk.datastore.DaoHelper;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.event.IEventDispatcher;
import mil.nga.giat.mage.sdk.event.ILayerEventListener;
import mil.nga.giat.mage.sdk.exceptions.LayerException;
import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;

/**
 * A utility class for accessing {@link Layer} data from the physical data
 * model. The details of ORM DAOs and Lazy Loading should not be exposed past
 * this class.
 * 
 * @author wiedemannse
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

    public Collection<Layer> readAllStaticLayers() throws LayerException {
        List<Layer> layers = new ArrayList<Layer>();
        try {
            layers = layerDao.queryBuilder().where().eq("type", "External").query(); 
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "Unable to read Layers", sqle);
            throw new LayerException("Unable to read Layers.", sqle);
        }
        return layers;
    }

    @Override
    public Layer create(Layer pLayer) throws LayerException {

        Layer createdLayer = null;
        try {
            createdLayer = layerDao.createIfNotExists(pLayer);
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "There was a problem creating the layer: " + pLayer + ".", sqle);
            throw new LayerException("There was a problem creating the layer: " + pLayer + ".", sqle);
        }

        return createdLayer;
    }

    /**
     * Returns only the list of layers that were created from this call. If a
     * layer already existed locally, it will not be returned in the list.
     * 
     * @param pLayers
     * @return
     * @throws LayerException
     */
    public List<Layer> createAll(Collection<Layer> pLayers) throws LayerException {
        
        List<Layer> createdLayers = new ArrayList<Layer>();
        for (Layer layer : pLayers) {
            try {
                if (read(layer.getRemoteId()) == null) {
                    createdLayers.add(layerDao.createIfNotExists(layer));
                }
            } catch (SQLException sqle) {
                Log.e(LOG_NAME, "There was a problem creating the layer: " + layer + ".", sqle);
                continue;
                // TODO Throw exception?
            }
        }

        Log.d(LOG_NAME, "Layers created: " + createdLayers);
        // fire the event
        for (ILayerEventListener listener : listeners) {
            listener.onLayersCreated(createdLayers);
        }

        return createdLayers;
    }
    
    public int deleteAllStaticLayers() throws LayerException {
        try {
            DaoStore.getInstance(context).getStaticFeaturePropertyDao().deleteBuilder().delete();
            DaoStore.getInstance(context).getStaticFeatureGeometryDao().deleteBuilder().delete();
            DaoStore.getInstance(context).getStaticFeatureDao().deleteBuilder().delete();
            
            DeleteBuilder<Layer, Long> builder = layerDao.deleteBuilder();
            builder.where().eq("type", "External");
            return builder.delete();
        } catch (SQLException e) {
            throw new LayerException("Unable to delete all layers", e);
        }
    }

    @Override
    public Layer read(Long id) throws LayerException {
        try {
            return layerDao.queryForId(id);
        } catch (SQLException sqle) {
            Log.e(LOG_NAME, "Unable to query for existance for id = '" + id + "'", sqle);
            throw new LayerException("Unable to query for existance for id = '" + id + "'", sqle);
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
            Log.e(LOG_NAME, "Unable to query for existance for remote_id = '" + pRemoteId + "'", sqle);
            throw new LayerException("Unable to query for existance for remote_id = '" + pRemoteId + "'", sqle);
        }

        return layer;
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
