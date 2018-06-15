package mil.nga.giat.mage.map.cache;

import android.content.Context;
import android.os.Environment;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;

import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.exceptions.LayerException;

public class CacheOverlayFilter {
    private Context context;
    private Collection<Layer> layers;

    private Predicate<CacheOverlay> eventPredicate = new Predicate<CacheOverlay>() {
        @Override
        public boolean apply(CacheOverlay overlay) {
            if (overlay instanceof GeoPackageCacheOverlay) {
                String filePath = ((GeoPackageCacheOverlay) overlay).getFilePath();
                if (filePath.startsWith(String.format("%s/MAGE/geopackages", context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)))) {
                    for (Layer layer : layers) {
                        String layerPath = String.format("geopackages/%s/%s", layer.getRemoteId(), layer.getFileName());
                        if (filePath.endsWith(layerPath)) {
                            return true;
                        }
                    }

                    return false;
                } else {
                    return true;
                }
            } else {
                return true;
            }
        }
    };

    public CacheOverlayFilter(Context context, Event event) {
        this.context = context;

        try {
            this.layers = LayerHelper.getInstance(context).readByEvent(event, "GeoPackage");
        } catch (LayerException e) {
            e.printStackTrace();
        }
    }

    public List<CacheOverlay> filter(List<CacheOverlay> overlays) {
        return Lists.newArrayList(Iterables.filter(overlays, eventPredicate));
    }
}
