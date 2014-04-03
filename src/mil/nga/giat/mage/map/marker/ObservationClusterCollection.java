package mil.nga.giat.mage.map.marker;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import mil.nga.giat.mage.map.marker.ObservationClusterCollection.ObservationClusterItem;
import mil.nga.giat.mage.observation.ObservationViewActivity;
import mil.nga.giat.mage.sdk.datastore.common.PointGeometry;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.ClusterManager.OnClusterItemClickListener;
import com.google.maps.android.clustering.algo.GridBasedAlgorithm;
import com.google.maps.android.clustering.algo.PreCachingAlgorithmDecorator;

public class ObservationClusterCollection implements ObservationCollection, OnClusterItemClickListener<ObservationClusterItem> {

    private Context context;
    
    private Map<Long, Observation> observations = new ConcurrentHashMap<Long, Observation>();
    private Map<Long, ObservationClusterItem> items = new ConcurrentHashMap<Long, ObservationClusterItem>();

    private ClusterManager<ObservationClusterItem> clusterManager;

    public ObservationClusterCollection(Context context, GoogleMap map) {
        this.context = context;
        
        clusterManager = new ClusterManager<ObservationClusterItem>(context, map);
        clusterManager.setAlgorithm(new PreCachingAlgorithmDecorator<ObservationClusterItem>(new GridBasedAlgorithm<ObservationClusterItem>()));
        
        clusterManager.setOnClusterItemClickListener(this);
    }

    @Override
    public void add(Observation o) {
        ObservationClusterItem item = new ObservationClusterItem(o);
        items.put(o.getId(), item);
        observations.put(o.getId(), o);
        clusterManager.addItem(item);
        clusterManager.cluster();
    }

    @Override
    public void addAll(Collection<Observation> all) {
        System.out.println("Adding " + all.size() + " observations to the map");
        
        for (Observation o : all) {
            ObservationClusterItem item = new ObservationClusterItem(o);
            items.put(o.getId(), item);
            observations.put(o.getId(), o);
            clusterManager.addItem(item);
        }
        
        System.out.println("clustering " + all.size() + " observations");
        clusterManager.cluster();
        System.out.println("DONE clustering " + all.size() + " observations");

    }

    @Override
    public Collection<Observation> getObservations() {
        return observations.values();
    }
    
    @Override
    public void remove(Observation o) {
        observations.remove(o.getId());
        ObservationClusterItem item = items.remove(o.getId());
        clusterManager.removeItem(item);
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

    @Override
    public void clear() {
        items.clear();
        observations.clear();
        clusterManager.clearItems();
        clusterManager.cluster();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return clusterManager.onMarkerClick(marker);
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        clusterManager.onCameraChange(cameraPosition);
    }
}