package mil.nga.giat.mage.map.marker;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.ClusterManager.OnClusterItemClickListener;
import com.google.maps.android.clustering.algo.GridBasedAlgorithm;
import com.google.maps.android.clustering.algo.PreCachingAlgorithmDecorator;
import com.vividsolutions.jts.geom.Point;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import mil.nga.giat.mage.map.marker.ObservationClusterCollection.ObservationClusterItem;
import mil.nga.giat.mage.observation.ObservationViewActivity;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;

public class ObservationClusterCollection implements PointCollection<Observation>, OnClusterItemClickListener<ObservationClusterItem> {

    private Context context;
    
    private static final String LOG_NAME = ObservationClusterCollection.class.getName();
    
    private Map<Long, Observation> observations = new ConcurrentHashMap<Long, Observation>();
    private Map<Long, ObservationClusterItem> items = new ConcurrentHashMap<Long, ObservationClusterItem>();

    private ClusterManager<ObservationClusterItem> clusterManager;
    
    private Date latestObservationDate = new Date(0);

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
        
        if (o.getLastModified().after(latestObservationDate)) {
            latestObservationDate = o.getLastModified();
        }
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
    public void refreshMarkerIcons() {
    	// TODO : figure this out?
    	Log.d(LOG_NAME, "TODO: refreshme");
    }
    
    @Override
    public void setVisibility(boolean visible) {
        // TODO not even sure what to do here with ClusterItem
        // its not a GoogleMap marker so you cannot hide it
    }
    
    @Override
    public boolean isVisible() {
    	// TODO not even sure what to do here with ClusterItem
        // its not a GoogleMap marker so you cannot hide it
    	return true;
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
            Point point = (Point) observation.getGeometry();
            return new LatLng(point.getY(), point.getX());
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

    @Override
    public Date getLatestDate() {
        return latestObservationDate;
    }

	@Override
	public void onInfoWindowClick(Marker arg0) {
		// TODO Auto-generated method stub
		
	}
}