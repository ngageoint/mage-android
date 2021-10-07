package mil.nga.giat.mage.map.marker;

import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.observation.MapMarkerObservation;
import mil.nga.giat.mage.observation.MapObservation;
import mil.nga.giat.mage.observation.MapObservationManager;
import mil.nga.giat.mage.observation.MapObservations;
import mil.nga.giat.mage.observation.MapShapeObservation;
import mil.nga.giat.mage.sdk.Temporal;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.utils.DateFormatFactory;
import mil.nga.sf.Geometry;
import mil.nga.sf.Point;
import mil.nga.sf.util.GeometryUtils;

public class ObservationMarkerCollection implements PointCollection<Observation>, OnMarkerClickListener {
    private static final String LOG_NAME = ObservationMarkerCollection.class.getName();

    private GoogleMap map;
    private Context context;
    private Date latestObservationDate = new Date(0);
    protected Pair<String, Circle> observationAccuracyCircle = null;
    private DateFormat dateFormat;

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
        dateFormat = DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), context);

        mapObservationManager = new MapObservationManager(context, map);
    }

    @Override
    public void add(MarkerOptions options, Observation observation) {
        // If I got an observation that I already have remove it
        mapObservations.remove(observation.getId());

        // Add the new observation to the map and maintain it
        options.visible(visible);
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

        if (observationAccuracyCircle != null) {
            observationAccuracyCircle.second.setVisible(visible);
        }
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    @Override
    public void remove(Observation o) {
        mapObservations.remove(o.getId());

        if (observationAccuracyCircle != null && observationAccuracyCircle.second.getId().equals(o.getRemoteId())) {
            observationAccuracyCircle.second.remove();
            observationAccuracyCircle = null;
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        boolean handled = false;

        Observation observation = mapObservations.getMarkerObservation(marker.getId());
        if (observation != null) {
            final Geometry g = observation.getGeometry();
            if (g != null) {
                Point point = GeometryUtils.getCentroid(g);
                LatLng latLng = new LatLng(point.getY(), point.getX());
                Float accuracy = observation.getAccuracy();
                if (accuracy != null) {
                    try {
                        if (observationAccuracyCircle != null) {
                            observationAccuracyCircle.second.remove();
                        }

                        Circle circle = map.addCircle(new CircleOptions()
                            .center(latLng)
                            .radius(accuracy)
                            .fillColor(context.getResources().getColor(R.color.accuracy_circle_fill))
                            .strokeColor(context.getResources().getColor(R.color.accuracy_circle_stroke))
                            .strokeWidth(2.0f));

                        observationAccuracyCircle = new Pair<>(observation.getRemoteId(), circle);
                    } catch (NumberFormatException nfe) {
                        Log.e(LOG_NAME, "Problem adding accuracy circle to the map.", nfe);
                    }
                }
            }

            map.setInfoWindowAdapter(infoWindowAdapter);
            marker.showInfoWindow();
            handled = true;
        }

        return handled;
    }

    @Override
    public void refresh(Observation observation) {

    }

    @Override
    public void refreshMarkerIcons(Filter<Temporal> filter) {
        for (MapMarkerObservation mapMarkerObservation : mapObservations.getMarkers()) {
            Marker marker = mapMarkerObservation.getMarker();
            Observation observation = mapMarkerObservation.getObservation();
			if (observation != null) {
                if (filter != null && !filter.passesFilter(observation)) {
                    this.remove(observation);
                } else {
                    boolean showWindow = marker.isInfoWindowShown();
                    // make sure to set the Anchor after this call as well, because the size of the icon might have changed
                    marker.setIcon(ObservationBitmapFactory.bitmapDescriptor(context, observation));
                    marker.setAnchor(0.5f, 1.0f);
                    if (showWindow) {
                        marker.showInfoWindow();
                    }
                }
            }
        }
    }

	@Override
	public int count() {
		return mapObservations.size();
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

        if (observationAccuracyCircle != null) {
            observationAccuracyCircle.second.remove();
            observationAccuracyCircle = null;
        }
    }

    @Override
    public void clear() {
        mapObservations.clear();
        latestObservationDate = new Date(0);

        if (observationAccuracyCircle != null) {
            observationAccuracyCircle.second.remove();
            observationAccuracyCircle = null;
        }
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
    public Observation pointForMarker(Marker marker) {
        return mapObservations.getMarkerObservation(marker.getId());
    }

    private class ObservationInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        @Override
        public View getInfoWindow(Marker marker) {
            final Observation observation = mapObservations.getMarkerObservation(marker.getId());
            if (observation == null) {
                return null;
            }

            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.observation_infowindow, null);
            view.findViewById(R.id.content).setVisibility(View.VISIBLE);

            TextView overline = view.findViewById(R.id.overline);
            overline.setText(dateFormat.format(observation.getTimestamp()));

            ObservationProperty primaryProperty = observation.getPrimaryFeedField();
            String primaryField = primaryProperty != null ? primaryProperty.getValue().toString() : null;
            if (primaryField != null) {
                TextView primary = view.findViewById(R.id.primary);
                primary.setVisibility(View.VISIBLE);
                primary.setText(primaryField);
            }

            ObservationProperty secondaryProperty = observation.getSecondaryFeedField();
            String secondaryField = secondaryProperty != null ? secondaryProperty.getValue().toString() : null;
            if (secondaryField != null) {
                TextView secondary = view.findViewById(R.id.secondary);
                secondary.setVisibility(View.VISIBLE);
                secondary.setText(secondaryField);
            }

            return view;
        }

        @Override
        public View getInfoContents(Marker marker) {
            return null;
        }
    }
}