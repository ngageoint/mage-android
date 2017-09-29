package mil.nga.giat.mage.map.marker;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.observation.ObservationViewActivity;
import mil.nga.giat.mage.sdk.Temporal;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;

public class ObservationMarkerCollection implements PointCollection<Observation>, OnMarkerClickListener {

    private GoogleMap map;
    private Context context;
    private Date latestObservationDate = new Date(0);

    private boolean visible = true;

    /**
     * Used to create and add observations to the map
     */
    private final MapObservationManager mapObservationManager;

    /**
     * Maintains the collection of map observations including markers and shapes
     */
    private final MapObservations mapObservations = new MapObservations();

    protected GoogleMap.InfoWindowAdapter infoWindowAdapter = new ObservationInfoWindowAdapter();

    public ObservationMarkerCollection(Context context, GoogleMap map) {
        this.map = map;
        this.context = context;

        mapObservationManager = new MapObservationManager(context, map);
    }

    @Override
    public Iterator<Observation> iterator() {
		return markerIdToObservation.values().iterator();
	}

    @Override
    public void add(MarkerOptions options, Observation observation) {
        // If I got an observation that I already have remove it
        mapObservations.remove(observation.getId());

        // Add the new observation to the map and maintain it
        MapObservation mapObservation = mapObservationManager.addToMap(observation, options, visible);
        mapObservations.add(mapObservation);

        if (observation.getLastModified().after(latestObservationDate)) {
            latestObservationDate = observation.getLastModified();
        }
    }

    @Override
    public void setVisibility(boolean visible) {
        if (this.visible == visible)
            return;

        this.visible = visible;
        mapObservations.setVisible(visible);
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    @Override
    public void remove(Observation o) {
        mapObservations.remove(o.getId());
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        boolean handled = false;

        Observation observation = mapObservations.getMarkerObservation(marker.getId());
        if (observation != null) {
            map.setInfoWindowAdapter(infoWindowAdapter);
            marker.showInfoWindow();
            handled = true;
        }

        return handled;
    }
    
	@Override
	public void refreshMarkerIcons(Filter<Temporal> filter) {
		for (Marker m : observationIdToMarker.values()) {
			Observation to = markerIdToObservation.get(m.getId());
			if (to != null) {
				if (filter != null && !filter.passesFilter(to)) {
					this.remove(to);
				} else {
					boolean showWindow = m.isInfoWindowShown();
					// make sure to set the Anchor after this call as well, because the size of the icon might have changed
					m.setIcon(ObservationBitmapFactory.bitmapDescriptor(context, markerIdToObservation.get(m.getId())));
					m.setAnchor(0.5f, 1.0f);
					if (showWindow) {
						m.showInfoWindow();
					}
				}
			}
		}
	}

	@Override
	public int count() {
		return observationIdToMarker.size();
	}

	@Override
    public void clear() {
        for (Marker marker : observationIdToMarker.values()) {
            marker.remove();
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {

        MapShapeObservation mapShapeObservation = mapObservations.getClickedShape(map, latLng);
        if (mapShapeObservation != null) {
            Marker shapeMarker = mapObservationManager.addShapeMarker(latLng, visible);
            mapObservations.setShapeMarker(shapeMarker, mapShapeObservation);
            map.setInfoWindowAdapter(infoWindowAdapter);
            shapeMarker.showInfoWindow();
        }
    }

    @Override
    public void offMarkerClick(){
        mapObservations.clearShapeMarker();
    }

    @Override
    public void clear() {
        mapObservations.clear();
        latestObservationDate = new Date(0);
    }

    @Override
    public Date getLatestDate() {
        return latestObservationDate;
    }

    @Override
    public void onCameraIdle() {
        // do nothing I don't care
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        Observation observation = mapObservations.getMarkerObservation(marker.getId());

        if (observation != null) {
            Intent intent = new Intent(context, ObservationViewActivity.class);
            intent.putExtra(ObservationViewActivity.OBSERVATION_ID, observation.getId());
            intent.putExtra(ObservationViewActivity.INITIAL_LOCATION, map.getCameraPosition().target);
            intent.putExtra(ObservationViewActivity.INITIAL_ZOOM, map.getCameraPosition().zoom);
            context.startActivity(intent);
        }
    }

    private class ObservationInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        @Override
        public View getInfoContents(Marker marker) {
            final Observation observation = mapObservations.getMarkerObservation(marker.getId());
            if (observation == null) {
                return null;
            }

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.observation_infowindow, null);

            ObservationProperty observationPropertyType = observation.getPropertiesMap().get("type");

            String type = observationPropertyType != null ? observationPropertyType.getValue().toString() : "";

            TextView observation_infowindow_type = (TextView) v.findViewById(R.id.observation_infowindow_type);
            observation_infowindow_type.setText(type);

            TextView observation_infowindow_date = (TextView) v.findViewById(R.id.observation_infowindow_date);
            observation_infowindow_date.setText(new PrettyTime().format(observation.getTimestamp()));

            return v;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null; // Use default info window
        }
    }

}