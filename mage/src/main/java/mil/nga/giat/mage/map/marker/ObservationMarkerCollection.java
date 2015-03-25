package mil.nga.giat.mage.map.marker;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.MarkerManager;
import com.vividsolutions.jts.geom.Point;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.observation.ObservationViewActivity;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;

public class ObservationMarkerCollection implements PointCollection<Observation>, OnMarkerClickListener {

    private GoogleMap map;
    private Context context;
    private Date latestObservationDate = new Date(0);

    private boolean visible = true;

    private Map<Long, Marker> observationIdToMarker = new ConcurrentHashMap<Long, Marker>();
    private Map<String, Observation> markerIdToObservation = new ConcurrentHashMap<String, Observation>();

	protected GoogleMap.InfoWindowAdapter infoWindowAdapter = new ObservationInfoWindowAdapter();

    private MarkerManager.Collection markerCollection;

    public ObservationMarkerCollection(Context context, GoogleMap map) {
        this.map = map;
        this.context = context;

        MarkerManager markerManager = new MarkerManager(map);
        markerCollection = markerManager.newCollection();
    }

    @Override
    public void add(Observation o) {
        // If I got an observation that I already have in my list
        // remove it from the map and clean-up my collections
        Marker marker = observationIdToMarker.remove(o.getId());
        if (marker != null) {
            markerIdToObservation.remove(marker.getId());
            marker.remove();
        }

        Point point = (Point) o.getGeometry();
        MarkerOptions options = new MarkerOptions()
            .position(new LatLng(point.getY(), point.getX()))
            .icon(ObservationBitmapFactory.bitmapDescriptor(context, o))
            .visible(visible);

        marker = markerCollection.addMarker(options);
        observationIdToMarker.put(o.getId(), marker);
        markerIdToObservation.put(marker.getId(), o);
        
        if (o.getLastModified().after(latestObservationDate)) {
            latestObservationDate = o.getLastModified();
        }
    }

    @Override
    public void addAll(Collection<Observation> observations) {
        for (Observation o : observations) {
            add(o);
        }
    }

    @Override
    public void setVisibility(boolean visible) {
        if (this.visible == visible)
            return;
        
        this.visible = visible;
        for (Marker m : observationIdToMarker.values()) {
            m.setVisible(visible);
        }
    }
    
    @Override
    public boolean isVisible() {
    	return this.visible;
    }

    @Override
    public void remove(Observation o) {
        Marker marker = observationIdToMarker.remove(o.getId());
        if (marker != null) {
            markerIdToObservation.remove(marker.getId());
            markerCollection.remove(marker);
            marker.remove();
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Observation o = markerIdToObservation.get(marker.getId());
        
        if (o == null) return false;  // Not an observation let someone else handle it

		map.setInfoWindowAdapter(infoWindowAdapter);
		marker.showInfoWindow();

        return true;
    }
    
	@Override
	public void refreshMarkerIcons() {
		for (Marker m : markerCollection.getMarkers()) {
			Observation to = markerIdToObservation.get(m.getId());
			if (to != null) {
				boolean showWindow = m.isInfoWindowShown();
				m.setIcon(ObservationBitmapFactory.bitmapDescriptor(context, markerIdToObservation.get(m.getId())));
				if(showWindow) {
					m.showInfoWindow();
				}
			}
		}
	}

    @Override
    public void clear() {
        observationIdToMarker.clear();
        markerIdToObservation.clear();
        markerCollection.clear();
        latestObservationDate = new Date(0);
    }

    @Override
    public Date getLatestDate() {
        return latestObservationDate;
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        // do nothing I don't care
    }

	@Override
	public void onInfoWindowClick(Marker marker) {
		Observation o = markerIdToObservation.get(marker.getId());

		if (o == null) {
			return;
		}

		Intent intent = new Intent(context, ObservationViewActivity.class);
        intent.putExtra(ObservationViewActivity.OBSERVATION_ID, o.getId());
        intent.putExtra(ObservationViewActivity.INITIAL_LOCATION, map.getCameraPosition().target);
        intent.putExtra(ObservationViewActivity.INITIAL_ZOOM, map.getCameraPosition().zoom);
        context.startActivity(intent);
	}

	private class ObservationInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

		@Override
		public View getInfoContents(Marker marker) {
			final Observation observation = markerIdToObservation.get(marker.getId());
			if (observation == null) {
				return null;
			}

			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = inflater.inflate(R.layout.observation_infowindow, null);

			ObservationProperty observationPropertyType = observation.getPropertiesMap().get("type");

			String type = observationPropertyType!=null?observationPropertyType.getValue().toString():"";

			TextView observation_infowindow_type = (TextView)v.findViewById(R.id.observation_infowindow_type);
			observation_infowindow_type.setText(type);

			TextView observation_infowindow_date = (TextView)v.findViewById(R.id.observation_infowindow_date);
			observation_infowindow_date.setText(new PrettyTime().format(observation.getTimestamp()));

			return v;
		}

		@Override
		public View getInfoWindow(Marker marker) {
			return null; // Use default info window
		}
	}
}