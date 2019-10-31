package mil.nga.giat.mage.sdk.fetch;

import android.content.Context;
import android.util.Log;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.http.resource.LayerResource;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.login.LoginTaskFactory;

public class ImageryServerFetch extends AbstractServerFetch {

    private static final String LOG_NAME = ImageryServerFetch.class.getName();
    private static final String TYPE = "Imagery";

    private AtomicBoolean isCanceled = new AtomicBoolean(false);

    private final LayerResource layerResource;

    public ImageryServerFetch(Context context) {
        super(context);
        layerResource = new LayerResource(context);
    }

    public void fetch(){
        Event event = EventHelper.getInstance(mContext).getCurrentEvent();
        LayerHelper layerHelper = LayerHelper.getInstance(mContext);

        // if you are disconnect, skip this
        if(!ConnectivityUtility.isOnline(mContext) || LoginTaskFactory.getInstance(mContext).isLocalLogin()) {
            Log.d(LOG_NAME, "Disconnected, not pulling imagery.");
            return;
        }

        try {
            Collection<Layer> remoteLayers = layerResource.getImageryLayers(event);

            // get local layers
            Collection<Layer> localLayers = layerHelper.readAll(TYPE);
            remoteLayers.removeAll(localLayers);

            for (Layer layer : remoteLayers) {
                if(isCanceled.get()){
                    break;
                }
                layer.setLoaded(true);
                layerHelper.create(layer);
            }
        }catch(Exception e){
            Log.w(LOG_NAME, "Error performing imagery layer operations",e);
        }
    }


    public void destroy() {
        isCanceled.getAndSet(true);
    }

}
