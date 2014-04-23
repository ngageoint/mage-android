package mil.nga.giat.mage.map;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.MAGE.OnCacheOverlayListener;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.filter.DateTimeFilter;
import mil.nga.giat.mage.map.GoogleMapWrapper.OnMapPanListener;
import mil.nga.giat.mage.map.marker.LocationMarkerCollection;
import mil.nga.giat.mage.map.marker.PointCollection;
import mil.nga.giat.mage.map.marker.ObservationMarkerCollection;
import mil.nga.giat.mage.map.preference.MapPreferencesActivity;
import mil.nga.giat.mage.observation.ObservationEditActivity;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeatureHelper;
import mil.nga.giat.mage.sdk.event.ILocationEventListener;
import mil.nga.giat.mage.sdk.event.IObservationEventListener;
import mil.nga.giat.mage.sdk.event.IStaticFeatureEventListener;
import mil.nga.giat.mage.sdk.exceptions.LayerException;
import mil.nga.giat.mage.sdk.location.LocationService;
import mil.nga.giat.mage.sdk.utils.Temporal;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.maps.android.PolyUtil;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class MapFragment extends Fragment implements 
        OnMapClickListener,
        OnMapLongClickListener, 
        OnMarkerClickListener,
        OnMapPanListener, 
        OnMyLocationButtonClickListener, 
        OnClickListener, 
        LocationSource, 
        LocationListener, 
        OnCacheOverlayListener, 
        OnSharedPreferenceChangeListener, 
        IObservationEventListener,
        ILocationEventListener,
        IStaticFeatureEventListener {

    private MAGE mage;
    private MapView mapView;
    private GoogleMap map;
    private int mapType = 1;
    private Location location;
    private boolean followMe = false;
    private GoogleMapWrapper mapWrapper;
    private OnLocationChangedListener locationChangedListener;
    
    private final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(5);
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 3, 10, TimeUnit.SECONDS, queue);

    private Filter<Temporal> temporalFilter;
    private PointCollection<Observation> observations;
    private PointCollection<mil.nga.giat.mage.sdk.datastore.location.Location> locations;

    private Map<String, TileOverlay> tileOverlays = new HashMap<String, TileOverlay>();
    private Collection<String> featureIds = new ArrayList<String>();
    private Map<String, Collection<Marker>> featureMarkers = new HashMap<String, Collection<Marker>>();
    private Map<String, Collection<Polyline>> featurePolylines = new HashMap<String, Collection<Polyline>>();
    private Map<String, Collection<Polygon>> featurePolygons = new HashMap<String, Collection<Polygon>>();

    private LocationService locationService;

    SharedPreferences preferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        setHasOptionsMenu(true);
        
        mage = (MAGE) getActivity().getApplication();

        mapWrapper = new GoogleMapWrapper(getActivity().getApplicationContext());
        mapWrapper.addView(view);

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

        mapView = (MapView) view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        
		MapsInitializer.initialize(getActivity().getApplicationContext());
        
        ImageButton mapSettings = (ImageButton) view.findViewById(R.id.map_settings);
        mapSettings.setOnClickListener(this);
        
        locationService = mage.getLocationService();
                
        return mapWrapper;
    }
    
    @Override
    public void onDestroy() {
        mapView.onDestroy();
        map = null;
        
        observations.clear();
        observations = null;
        
        locations.clear();
        locations = null;

        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        mapView.onResume();

        PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
        
        if (map == null) {
            map = mapView.getMap();
        }
        map.setOnMapClickListener(this);
        map.setOnMarkerClickListener(this);
        map.setOnMapLongClickListener(this);
        map.setOnMyLocationButtonClickListener(this);
        
        updateMapType();
        updateStaticFeatureLayers();
        
        temporalFilter = getTemporalFilter();
        if (observations == null) {
            observations = new ObservationMarkerCollection(getActivity(), map);
        }
        ObservationHelper.getInstance(getActivity().getApplicationContext()).addListener(this);
        ObservationLoadTask observationLoad = new ObservationLoadTask(getActivity(), observations);
        observationLoad.setFilter(temporalFilter);
        observationLoad.executeOnExecutor(executor);
        
        boolean showObservations = preferences.getBoolean(getResources().getString(R.string.showObservationsKey), true);
        observations.setVisibility(showObservations); 
        
        if (locations == null) {
            locations = new LocationMarkerCollection(getActivity(), map);            
        }
        LocationHelper.getInstance(getActivity().getApplicationContext()).addListener(this);
        LocationLoadTask locationLoad = new LocationLoadTask(getActivity(), locations);
        locationLoad.setFilter(temporalFilter);
        locationLoad.executeOnExecutor(executor);
        
        boolean showLocations = preferences.getBoolean(getResources().getString(R.string.showLocationsKey), true);
        locations.setVisibility(showLocations);
        

        
        mage.registerCacheOverlayListener(this);
        StaticFeatureHelper.getInstance(getActivity().getApplicationContext()).addListener(this);

        // Check if any map preferences changed that I care about
        boolean locationServiceEnabled = preferences.getBoolean(getResources().getString(R.string.locationServiceEnabledKey), false);
        map.setMyLocationEnabled(locationServiceEnabled);

        if (locationServiceEnabled) {
            map.setLocationSource(this);
            locationService.registerOnLocationListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        
        mapView.onPause();
        
        ObservationHelper.getInstance(getActivity().getApplicationContext()).removeListener(this);   
        LocationHelper.getInstance(getActivity().getApplicationContext()).removeListener(this);

        PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);

        mage.unregisterCacheOverlayListener(this);
        StaticFeatureHelper.getInstance(getActivity().getApplicationContext()).removeListener(this);

        boolean locationServiceEnabled = Integer.parseInt(preferences.getString(getResources().getString(R.string.userReportingFrequencyKey), "0")) > 0;
        if (locationServiceEnabled) {
            map.setLocationSource(null);
            locationService.unregisterOnLocationListener(this);
        }
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        
        inflater.inflate(R.menu.observation_new, menu);
        inflater.inflate(R.menu.filter, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.observation_new:
                 Intent intent = new Intent(getActivity().getApplicationContext(), ObservationEditActivity.class);
                 LocationService ls = ((MAGE) getActivity().getApplication()).getLocationService();
                 Location l = ls.getLocation();
                 intent.putExtra(ObservationEditActivity.LOCATION, l);
                 intent.putExtra(ObservationEditActivity.INITIAL_LOCATION,  map.getCameraPosition().target);
                 intent.putExtra(ObservationEditActivity.INITIAL_ZOOM, map.getCameraPosition().zoom);
                 startActivity(intent);
                 break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onObservationCreated(Collection<Observation> o) {        
        ObservationTask task = new ObservationTask(ObservationTask.Type.ADD, observations);
        task.setFilter(temporalFilter);
        task.execute(o.toArray(new Observation[o.size()]));
    }

    @Override
    public void onObservationUpdated(Observation o) {
        ObservationTask task = new ObservationTask(ObservationTask.Type.UPDATE, observations);
        task.setFilter(temporalFilter);
        task.execute(o);    
    }

    @Override
    public void onObservationDeleted(Observation o) {
        new ObservationTask(ObservationTask.Type.DELETE, observations).execute(o);
    }

    @Override
    public void onLocationCreated(Collection<mil.nga.giat.mage.sdk.datastore.location.Location> l) {
        LocationTask task = new LocationTask(LocationTask.Type.ADD, locations);
        task.setFilter(temporalFilter);
        task.execute(l.toArray(new mil.nga.giat.mage.sdk.datastore.location.Location[l.size()]));  
    }

    @Override
    public void onLocationUpdated(mil.nga.giat.mage.sdk.datastore.location.Location l) {
        LocationTask task = new LocationTask(LocationTask.Type.UPDATE, locations);
        task.setFilter(temporalFilter);
        task.execute(l);      
    }

    @Override
    public void onLocationDeleted(String pUserLocalId) {
        // TODO travis why userId here but Location the rest of the time
//        new LocationTask(LocationTask.Type.DELETE, locations).execute(l);        
    }
    

    @Override
    public boolean onMarkerClick(Marker marker) {
        // You can only have one marker click listener per map.
        // Lets listen here and shell out the click event to all
        // my marker collections.  Each one need to handle
        // gracefully if it does not actually contain the marker
        if (observations.onMarkerClick(marker)) {
            return true;
        }
        
        if (locations.onMarkerClick(marker)) {
            return true;
        }
        
        map.setInfoWindowAdapter(null);
        marker.showInfoWindow();
        return true;
    }
    
    @Override
    public void onMapClick(LatLng latLng) {
        Log.i("static feature", "map clicked at: " + latLng.toString());

        
        Polyline polyline = null;
        for (Map.Entry<String, Collection<Polyline>> entry : featurePolylines.entrySet()) {
            for (Polyline p : entry.getValue()) {
                if (PolyUtil.isLocationOnPath(latLng, p.getPoints(), true)) {
                    polyline = p;
                    break;
                }
            }
            
            if (polyline != null) break;
        }
        if (polyline != null) {
            // found it open a info window
            Log.i("static feature", "static feature polyline clicked at: " + latLng.toString());
            return;
        }
        
        Polygon polygon = null;
        for (Map.Entry<String, Collection<Polygon>> entry : featurePolygons.entrySet()) {
            for (Polygon p : entry.getValue()) {
                if (PolyUtil.containsLocation(latLng, p.getPoints(), true)) {
                    polygon = p;
                    break;
                }
            }
            
            if (polygon != null) break;
        }
        if (polygon != null) {
            // found it open a info window
            Log.i("static feature", "static feature polgon clicked at: " + latLng.toString());
            return;
        }
    }
    
    @Override
    public void onMapLongClick(LatLng point) {
        Intent intent = new Intent(getActivity().getApplicationContext(), ObservationEditActivity.class);
        Location l = new Location("manual");
        l.setAccuracy(0.0f);
        l.setLatitude(point.latitude);
        l.setLongitude(point.longitude);
        intent.putExtra(ObservationEditActivity.LOCATION, l);
        startActivity(intent);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.map_settings: {
                Intent i = new Intent(getActivity().getApplicationContext(), MapPreferencesActivity.class);
                startActivity(i);
                break;
            }
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {

        if (location != null) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            float zoom = map.getCameraPosition().zoom < 15 ? 15 : map.getCameraPosition().zoom;
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom), new CancelableCallback() {

                @Override
                public void onCancel() {
                    mapWrapper.setOnMapPanListener(MapFragment.this);
                    followMe = true;
                }

                @Override
                public void onFinish() {
                    mapWrapper.setOnMapPanListener(MapFragment.this);
                    followMe = true;
                }
            });
        }

        return true;
    }

    @Override
    public void activate(OnLocationChangedListener listener) {
        locationChangedListener = listener;
    }

    @Override
    public void deactivate() {
        locationChangedListener = null;
    }

    @Override
    public void onMapPan() {
        mapWrapper.setOnMapPanListener(null);
        followMe = false;
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        if (locationChangedListener != null) {
            locationChangedListener.onLocationChanged(location);
        }

        if (followMe) {
            LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            if (!bounds.contains(latLng)) {
                // Move the camera to the user's location once it's available!
                map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onCacheOverlay(List<CacheOverlay> cacheOverlays) {
        Set<String> overlays = preferences.getStringSet(getResources().getString(R.string.mapTileOverlaysKey), Collections.<String> emptySet());

        // Add all overlays that are in the preferences
        // For now there is no ordering in how tile overlays are stacked
        Set<String> removedOverlays = new HashSet<String>(tileOverlays.keySet());

        for (CacheOverlay cacheOverlay : cacheOverlays) {
            // The user has asked for this overlay
            if (overlays.contains(cacheOverlay.getName())) {
                if (!tileOverlays.keySet().contains(cacheOverlay.getName())) {
                    TileProvider tileProvider = new FileSystemTileProvider(256, 256, cacheOverlay.getDirectory().getAbsolutePath());
                    TileOverlay tileOverlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
                    tileOverlays.put(cacheOverlay.getName(), tileOverlay);
                }

                removedOverlays.remove(cacheOverlay.getName());
            }
        }

        // Remove any overlays that are on the map but no longer selected in
        // preferences
        for (String overlay : removedOverlays) {
            tileOverlays.remove(overlay).remove();
        }
    }
    
    private void updateStaticFeatureLayers() {
        removeStaticFeatureLayers();
        
        try {
            for (Layer l : LayerHelper.getInstance(getActivity().getApplicationContext()).readAllStaticLayers()) {
                onStaticFeatureLayer(l);                
            }
        } catch (LayerException e) {
            e.printStackTrace();
        }
    }
    
    private void removeStaticFeatureLayers() {
        Set<String> selectedLayerIds = preferences.getStringSet(getResources().getString(R.string.mapFeatureOverlaysKey), Collections.<String> emptySet());
        
        Set<String> currentLayerIds = new HashSet<String>();
        currentLayerIds.addAll(featureMarkers.keySet());
        currentLayerIds.addAll(featurePolylines.keySet());
        currentLayerIds.addAll(featurePolygons.keySet());
        
        for (String currentLayerId : currentLayerIds) {
            if (!selectedLayerIds.contains(currentLayerId)) {
              featureIds.remove(currentLayerId);
              removeFeatures(currentLayerId);
            }
        }
    }
    
    @Override
    public void onStaticFeaturesCreated(final Layer layer) {
        getActivity().runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                onStaticFeatureLayer(layer);               
            }
        });
    }
    
    private void onStaticFeatureLayer(Layer layer) {
        Set<String> layers = preferences.getStringSet(getResources().getString(R.string.mapFeatureOverlaysKey), Collections.<String> emptySet());

        // The user has asked for this feature layer
        String layerId = layer.getId().toString();
        if (layers.contains(layerId) && layer.isLoaded()) {
            if (!featureIds.contains(layerId)) {                    
                featureIds.add(layerId);
                featureMarkers.put(layerId, new ArrayList<Marker>());
                featurePolylines.put(layerId, new ArrayList<Polyline>());
                featurePolygons.put(layerId, new ArrayList<Polygon>());
                addFeatures(layer);
            }
        }
    }
    
    private void addFeatures(Layer layer) {
        String layerId = layer.getId().toString();
        
        Log.i("static feature", "static feature layer: " + layer.getName() + " is enabled, it has " + layer.getStaticFeatures().size() + " features");
        
        for (StaticFeature feature : layer.getStaticFeatures()) {
            Geometry geometry = feature.getStaticFeatureGeometry().getGeometry();
            String type = geometry.getGeometryType();            
            if (type.equals("Point")) {
                MarkerOptions options = new MarkerOptions()
                    .position(new LatLng(geometry.getCoordinate().y, geometry.getCoordinate().x))
                    .title(layer.getName())
                    .snippet("Temp static feature snippet");
                Marker m = map.addMarker(options);
                featureMarkers.get(layerId).add(m);
            } else if (type.equals("LineString")) {
                PolylineOptions options = new PolylineOptions();
                for (Coordinate c : geometry.getCoordinates()) {
                    options.add(new LatLng(c.y, c.x));
                }

                Polyline p = map.addPolyline(options);
                featurePolylines.get(layerId).add(p);
            } else if (type.equals("Polygon")) {
                PolygonOptions options = new PolygonOptions();
                for (Coordinate c : geometry.getCoordinates()) {
                    options.add(new LatLng(c.y, c.x));
                }
                
                Polygon p = map.addPolygon(options);
                featurePolygons.get(layerId).add(p);
            }
        }
    }
    
    private void removeFeatures(String layerId) {
        for (Marker m : featureMarkers.remove(layerId)) {
            m.remove();
        }
        
        for (Polyline p : featurePolylines.remove(layerId)) {
            p.remove();
        }
        
        for (Polygon p : featurePolygons.remove(layerId)) {
            p.remove();
        }
    }

    private void updateMapType() {
        int mapType = Integer.parseInt(preferences.getString(getResources().getString(R.string.baseLayerKey), "1"));
        if (mapType != this.mapType) {
            this.mapType = mapType;
            map.setMapType(this.mapType);
        }
    }


//    private void updateObservations() {        
//        //TODO think of a way to fix this if we want users to be able to swap
//        //between clusters and not clusters
//        boolean cluster = preferences.getBoolean("clusterObservations", false);
//        if (observations == null) {
//            this.cluster = cluster;
//
//            // Create the observations collection and start listening for
//            // updates
//            observations = cluster ? new ObservationClusterCollection(getActivity(), map) : new ObservationMarkerCollection(getActivity(), map);
//
//            try {
//                ObservationHelper.getInstance(getActivity()).addListener(this);
//            } catch (ObservationException e) {
//                e.printStackTrace();
//            }
//        } else if (this.cluster != cluster) {
//            this.cluster = cluster;
//
//            Collection<Observation> existing = observations != null ? new ArrayList<Observation>(observations.getObservations()) : Collections.<Observation> emptyList();
//            observations.clear();
//            observations = cluster ? new ObservationClusterCollection(getActivity(), map) : new ObservationMarkerCollection(getActivity(), map);
//            observations.addAll(existing);
//
//        }
//    }

    @Override
    public void onError(Throwable error) {
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (getResources().getString(R.string.activeTimeFilterKey).equalsIgnoreCase(key)) {
		    observations.clear();
	        ObservationLoadTask observationLoad = new ObservationLoadTask(getActivity(), observations);
	        observationLoad.setFilter(getTemporalFilter());
	        observationLoad.executeOnExecutor(executor);
	        
	        locations.clear();
            LocationLoadTask locationLoad = new LocationLoadTask(getActivity(), locations);
            locationLoad.setFilter(getTemporalFilter());
            locationLoad.executeOnExecutor(executor);
		}
	}
	
	private Filter<Temporal> getTemporalFilter() {
	    int timeFilter = preferences.getInt(getResources().getString(R.string.activeTimeFilterKey), R.id.none_rb);
	    
	    Filter<Temporal> filter = null;
	    
		Calendar c = Calendar.getInstance();
		String title = null;
		switch (timeFilter) {
    		case R.id.last_hour_rb:
    			title = "Last Hour";
    			c.add(Calendar.HOUR, -1);
    			break;
    		case R.id.last_six_hours_rb:
    			title = "Last 6 Hours";
    			c.add(Calendar.HOUR, -6);
    			break;
    		case R.id.last_twelve_hours_rb:
    			title = "Last 12 Hours";
    			c.add(Calendar.HOUR, -12);
    			break;
    		case R.id.last_24_hours_rb:
    			title = "Last 24 Hours";
    			c.add(Calendar.HOUR, -24);
    			break;
    		case R.id.since_midnight_rb:
    			title = "Since Midnight";
    			c.set(Calendar.HOUR_OF_DAY, 0);
    			c.set(Calendar.MINUTE, 0);
    			c.set(Calendar.SECOND, 0);
    			c.set(Calendar.MILLISECOND, 0);
    			break;
    		default:
    			// no filter
    			title = "MAGE";
    			c = null;
		}
		
		getActivity().getActionBar().setTitle(title);
		
		if (c != null) {
		    Date start = c.getTime();
		    Date end = null;
		    
		    filter = new DateTimeFilter(start, end);
		}
		
		return filter;
	}
}