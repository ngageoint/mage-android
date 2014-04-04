package mil.nga.giat.mage.map.marker;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import mil.nga.giat.mage.observation.ObservationViewActivity;
import mil.nga.giat.mage.sdk.datastore.common.PointGeometry;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.MarkerManager;

public class ObservationMarkerCollection implements ObservationCollection, OnMarkerClickListener {

    private Context context;

    private Map<Long, Marker> observationIdToMarker = new ConcurrentHashMap<Long, Marker>();
    private Map<String, Observation> markerIdToObservation = new ConcurrentHashMap<String, Observation>();

    private MarkerManager.Collection markerCollection;

    public ObservationMarkerCollection(Context context, GoogleMap map) {
        this.context = context;

        MarkerManager markerManager = new MarkerManager(map);
        markerCollection = markerManager.newCollection();
        map.setOnMarkerClickListener(this);
    }

    @Override
    public void add(Observation o) {
        PointGeometry point = (PointGeometry) o.getObservationGeometry().getGeometry();
        MarkerOptions options = new MarkerOptions().position(new LatLng(point.getLatitude(), point.getLongitude()));

        Marker marker = markerCollection.addMarker(options);
        observationIdToMarker.put(o.getId(), marker);
        markerIdToObservation.put(marker.getId(), o);
    }

    @Override
    public void addAll(Collection<Observation> observations) {
        for (Observation o : observations) {
            add(o);
        }
    }

    @Override
    public Collection<Observation> getObservations() {
        return markerIdToObservation.values();
    }
    
    @Override
    public void setVisible(boolean visible) {
        for (Marker m : observationIdToMarker.values()) {
            m.setVisible(visible);
        }
    }

    @Override
    public void remove(Observation o) {
        Marker marker = observationIdToMarker.remove(o.getId());
        if (marker != null) {
            markerCollection.remove(marker);
            markerIdToObservation.remove(marker.getId());
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Observation o = markerIdToObservation.get(marker.getId());

        Intent intent = new Intent(context, ObservationViewActivity.class);
        intent.putExtra(ObservationViewActivity.OBSERVATION_ID, o.getId());
        context.startActivity(intent);

        return false;
    }

    @Override
    public void clear() {
        observationIdToMarker.clear();
        markerIdToObservation.clear();
        markerCollection.clear();
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        // do nothing I don't care
    }
}