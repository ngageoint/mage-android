package mil.nga.giat.mage.map.marker;

import java.util.HashMap;
import java.util.Map;

import mil.nga.giat.mage.map.marker.ObservationClusterCollection.ObservationClusterItem;
import mil.nga.giat.mage.observation.ObservationViewActivity;
import mil.nga.giat.mage.sdk.datastore.common.PointGeometry;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.ClusterManager.OnClusterItemClickListener;

public class ObservationClusterCollection implements ObservationCollection, OnClusterItemClickListener<ObservationClusterItem> {

    private Context context;
    
    private Map<Long, Observation> observations = new HashMap<Long, Observation>();
    private Map<Long, ObservationClusterItem> items = new HashMap<Long, ObservationClusterItem>();

    private ClusterManager<ObservationClusterItem> observationClusterManager;

    public ObservationClusterCollection(Context context, GoogleMap map) {
        this.context = context;
        
        observationClusterManager = new ClusterManager<ObservationClusterItem>(context, map);
        map.setOnCameraChangeListener(observationClusterManager);
        map.setOnMarkerClickListener(observationClusterManager);
        
        observationClusterManager.setOnClusterItemClickListener(this);
    }

    public void add(Observation o) {
        observations.put(o.getId(), o);
        ObservationClusterItem item = new ObservationClusterItem(o);
        items.put(o.getId(), item);
        observationClusterManager.addItem(item);
    }

    public void remove(Observation o) {
        ObservationClusterItem item = items.remove(o.getId());
        observationClusterManager.removeItem(item);
        observations.remove(o.getId());
    }

    public class ObservationClusterItem implements ClusterItem {

        private Observation observation;

        public ObservationClusterItem(Observation observation) {
            this.observation = observation;
        }

        @Override
        public LatLng getPosition() {
            PointGeometry point = (PointGeometry) observation.getObservationGeometry().getGeometry();
            return new LatLng(point.getLatitude(), point.getLongitude());
        }
        
        public Long getId() {
            return observation.getId();
        }
    }

    @Override
    public boolean onClusterItemClick(ObservationClusterItem item) {
        Intent o = new Intent(context, ObservationViewActivity.class);
        o.putExtra(ObservationViewActivity.OBSERVATION_ID, item.getId());
        context.startActivity(o);
        
        return false;
    }
}
