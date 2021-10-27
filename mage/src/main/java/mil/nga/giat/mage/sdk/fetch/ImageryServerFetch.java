package mil.nga.giat.mage.sdk.fetch;

import android.content.Context;
import android.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.http.resource.LayerResource;

public class ImageryServerFetch extends AbstractServerFetch {

    private static final String LOG_NAME = ImageryServerFetch.class.getName();
    private static final String TYPE = "Imagery";

    private final AtomicBoolean isCanceled = new AtomicBoolean(false);

    private final LayerResource layerResource;

    public ImageryServerFetch(Context context) {
        super(context);
        layerResource = new LayerResource(context);
    }

    public void fetch() {
        Event event = EventHelper.getInstance(mContext).getCurrentEvent();
        LayerHelper layerHelper = LayerHelper.getInstance(mContext);

        // if you are disconnect, skip this
        if (!ConnectivityUtility.isOnline(mContext)) {
            Log.d(LOG_NAME, "Disconnected, not pulling imagery.");
            return;
        }

        try {
            Collection<Layer> remoteLayers = layerResource.getImageryLayers(event);

            // get local layers
            Collection<Layer> localLayers = layerHelper.readAll(TYPE);

            Map<String, Layer> remoteIdToLayer = new HashMap<>(localLayers.size());
            Iterator<Layer> it = localLayers.iterator();
            while (it.hasNext()) {
                Layer localLayer = it.next();

                //See if the layer has been deleted on the server
                if (!remoteLayers.contains(localLayer)){
                    it.remove();
                    layerHelper.delete(localLayer.getId());
                } else {
                    remoteIdToLayer.put(localLayer.getRemoteId(), localLayer);
                }
            }


            for (Layer remoteLayer : remoteLayers) {
                if (isCanceled.get()) {
                    break;
                }
                remoteLayer.setEvent(event);
                remoteLayer.setLoaded(true);

                if (!localLayers.contains(remoteLayer)) {
                    //New layer from remote server
                    layerHelper.create(remoteLayer);
                } else {
                    Layer localLayer = remoteIdToLayer.get(remoteLayer.getRemoteId());
                    layerHelper.delete(localLayer.getId());
                    layerHelper.create(remoteLayer);
                }
            }
        } catch(Exception e) {
            Log.w(LOG_NAME, "Error performing imagery layer operations",e);
        }
    }


    public void destroy() {
        isCanceled.getAndSet(true);
    }

}
