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
import mil.nga.giat.mage.filter.DateTimeFilter;
import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.map.GoogleMapWrapper.OnMapPanListener;
import mil.nga.giat.mage.map.marker.LocationMarkerCollection;
import mil.nga.giat.mage.map.marker.ObservationMarkerCollection;
import mil.nga.giat.mage.map.marker.PointCollection;
import mil.nga.giat.mage.map.marker.StaticGeometryCollection;
import mil.nga.giat.mage.map.preference.MapPreferencesActivity;
import mil.nga.giat.mage.observation.ObservationEditActivity;
import mil.nga.giat.mage.sdk.Temporal;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeatureHelper;
import mil.nga.giat.mage.sdk.event.ILocationEventListener;
import mil.nga.giat.mage.sdk.event.IObservationEventListener;
import mil.nga.giat.mage.sdk.event.IStaticFeatureEventListener;
import mil.nga.giat.mage.sdk.exceptions.LayerException;
import mil.nga.giat.mage.sdk.location.LocationService;

import org.apache.commons.lang3.StringUtils;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
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
import android.webkit.WebView;
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
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.maps.android.PolyUtil;

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

	private static final String LOG_NAME = MapFragment.class.getName();
	
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
    private StaticGeometryCollection staticGeometryCollection;

    private Map<String, TileOverlay> tileOverlays = new HashMap<String, TileOverlay>();
    private Collection<String> featureIds = new ArrayList<String>();

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
        
        staticGeometryCollection = null;

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
        
        updateMapView();
        
		if (staticGeometryCollection == null) {
			staticGeometryCollection = new StaticGeometryCollection();
		}
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
        
        saveMapView();
        
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
    public void onLocationDeleted(mil.nga.giat.mage.sdk.datastore.location.Location l) {
        new LocationTask(LocationTask.Type.DELETE, locations).execute(l);
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
        
        View markerInfoWindow = LayoutInflater.from(getActivity()).inflate(R.layout.marker_infowindow, null, false);
		WebView webView = ((WebView) markerInfoWindow.findViewById(R.id.infowindowcontent));
		webView.loadData(marker.getSnippet(), "text/html; charset=UTF-8", null);
		new AlertDialog.Builder(getActivity()).setView(markerInfoWindow).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			}
		}).show();
        return true;
    }
    
    @Override
    public void onMapClick(LatLng latLng) {
        Log.i("static feature", "map clicked at: " + latLng.toString());

        // how many meters away form the click can the geomerty be?
        Double circumferenceOfEarthInMeters = 2*Math.PI*6371000;
        //Double tileWidthAtZoomLevelAtEquatorInDegrees = 360.0/Math.pow(2.0, map.getCameraPosition().zoom);
        Double pixelSizeInMetersAtLatitude = (circumferenceOfEarthInMeters*Math.cos(map.getCameraPosition().target.latitude * (Math.PI /180.0))) / Math.pow(2.0, map.getCameraPosition().zoom + 8.0);
        Double tolerance = pixelSizeInMetersAtLatitude*Math.sqrt(2.0)*10.0;        
        
        // TODO : find the 'closest' line or polygon to the click.
		for (Polyline p : staticGeometryCollection.getPolylines()) {
			if (PolyUtil.isLocationOnPath(latLng, p.getPoints(), true, tolerance)) {
	            // found it open a info window
				Log.i("static feature", "static feature polyline clicked at: " + latLng.toString());
				View markerInfoWindow = LayoutInflater.from(getActivity()).inflate(R.layout.marker_infowindow, null, false);
				WebView webView = ((WebView) markerInfoWindow.findViewById(R.id.infowindowcontent));
				webView.loadData(staticGeometryCollection.getPopupHTML(p), "text/html; charset=UTF-8", null);
				new AlertDialog.Builder(getActivity()).setView(markerInfoWindow).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
	            return;
			}
		}

		for (Polygon p : staticGeometryCollection.getPolygons()) {
			if (PolyUtil.containsLocation(latLng, p.getPoints(), true)) {
				// found it open a info window
				Log.i("static feature", "static feature polgon clicked at: " + latLng.toString());

				View markerInfoWindow = LayoutInflater.from(getActivity()).inflate(R.layout.marker_infowindow, null, false);
				WebView webView = ((WebView) markerInfoWindow.findViewById(R.id.infowindowcontent));
				webView.loadData(staticGeometryCollection.getPopupHTML(p), "text/html; charset=UTF-8", null);
				new AlertDialog.Builder(getActivity()).setView(markerInfoWindow).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
				return;
			}
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
        Log.i(LOG_NAME, "map location, activate");
        locationChangedListener = listener;
        if (location != null) {
            Log.i(LOG_NAME, "map location, activate we have a location, let our listener know");
            locationChangedListener.onLocationChanged(location);
        }
    }

    @Override
    public void deactivate() {
        Log.i(LOG_NAME, "map location, deactivate");
        locationChangedListener = null;
    }

    @Override
    public void onMapPan() {
        mapWrapper.setOnMapPanListener(null);
        followMe = false;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(LOG_NAME, "map location, location changed");
        
        this.location = location;
        if (locationChangedListener != null) {
            Log.i(LOG_NAME, "map location, location changed we have a listener let them know");
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
        
        for (String currentLayerId : staticGeometryCollection.getLayers()) {
            if (!selectedLayerIds.contains(currentLayerId)) {
              featureIds.remove(currentLayerId);
              staticGeometryCollection.removeLayer(currentLayerId);
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
        Set<String> layers = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).getStringSet(getResources().getString(R.string.mapFeatureOverlaysKey), Collections.<String> emptySet());

        // The user has asked for this feature layer
        String layerId = layer.getId().toString();
        if (layers.contains(layerId) && layer.isLoaded()) {
            if (!featureIds.contains(layerId)) {
                featureIds.add(layerId);
                new StaticFeatureLoadTask(staticGeometryCollection, map).executeOnExecutor(executor, new Layer[]{ layer });
            }
        }
    }

    private void updateMapView() {
        // Check the map type
        int mapType = Integer.parseInt(preferences.getString(getResources().getString(R.string.baseLayerKey), "1"));
        if (mapType != this.mapType) {
            this.mapType = mapType;
            map.setMapType(this.mapType);
        }
        
        // Check the map location and zoom
        String xyz = preferences.getString(getResources().getString(R.string.mapXYZKey), null);
        if (xyz != null) {
            String[] values = StringUtils.split(xyz, ",");
            LatLng latLng = new LatLng(Double.valueOf(values[1]), Double.valueOf(values[0]));
            Float zoom = Float.valueOf(values[2]);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));                
        }
    }
    
    private void saveMapView() {
        CameraPosition position = map.getCameraPosition();
        
        String xyz = new StringBuilder()
            .append(Double.valueOf(position.target.longitude).toString()).append(",")
            .append(Double.valueOf(position.target.latitude).toString()).append(",")
            .append(Float.valueOf(position.zoom).toString())
            .toString();
        
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(getResources().getString(R.string.mapXYZKey), xyz);
        editor.commit();
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