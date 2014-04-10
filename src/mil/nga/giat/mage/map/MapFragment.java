package mil.nga.giat.mage.map;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.MAGE.OnCacheOverlayListener;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.map.GoogleMapWrapper.OnMapPanListener;
import mil.nga.giat.mage.map.marker.ObservationCollection;
import mil.nga.giat.mage.map.marker.ObservationMarkerCollection;
import mil.nga.giat.mage.map.preference.MapPreferencesActivity;
import mil.nga.giat.mage.observation.ObservationEditActivity;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.event.IObservationEventListener;
import mil.nga.giat.mage.sdk.exceptions.ObservationException;
import mil.nga.giat.mage.sdk.location.LocationService;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

public class MapFragment extends Fragment implements OnMapLongClickListener, OnMapPanListener, OnMyLocationButtonClickListener, OnClickListener, LocationSource, LocationListener, OnCacheOverlayListener, OnSharedPreferenceChangeListener, IObservationEventListener {

    private MAGE mage;
    private GoogleMap map;
    private int mapType = 1;
    private Location location;
    private boolean followMe = false;
    private GoogleMapWrapper mapWrapper;
    private OnLocationChangedListener locationChangedListener;

    private ObservationCollection observations;

    private Map<String, TileOverlay> tileOverlays = new HashMap<String, TileOverlay>();

    private LocationService locationService;

    SharedPreferences preferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        setHasOptionsMenu(true);
        
        mage = (MAGE) getActivity().getApplication();

        mapWrapper = new GoogleMapWrapper(getActivity());
        mapWrapper.addView(view);

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        map = ((com.google.android.gms.maps.MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        
        mapType = Integer.parseInt(preferences.getString("baseLayer", "1"));
        map.setMapType(mapType);

        map.setOnMapLongClickListener(this);
        map.setOnMyLocationButtonClickListener(this);

        ImageButton mapSettings = (ImageButton) view.findViewById(R.id.map_settings);
        mapSettings.setOnClickListener(this);

        locationService = mage.getLocationService();

        observations = new ObservationMarkerCollection(getActivity(), map);
        try {
            ObservationHelper.getInstance(getActivity()).addListener(this);
        } catch (ObservationException e) {
            e.printStackTrace();
        }
        PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
        return mapWrapper;
    }
    
    private void killOldMap() {
    	PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);
        com.google.android.gms.maps.MapFragment mapFragment = 
                ((com.google.android.gms.maps.MapFragment) getFragmentManager().findFragmentById(R.id.map));

        if (mapFragment != null && !getActivity().isDestroyed()) {
            FragmentManager manager = getFragmentManager();
            FragmentTransaction t = manager.beginTransaction();
            FragmentTransaction t2 = t.remove(mapFragment).detach(mapFragment);
            t2.commitAllowingStateLoss();
        }
    }
    
    @Override
    public void onDestroy() { 
        ObservationHelper.getInstance(getActivity()).removeListener(this);
        killOldMap();
        super.onDestroy();
    }
    
    @Override
    public void onDestroyView() {
    	killOldMap();
    	super.onDestroyView();
    }

    @Override
    public void onDetach() {
    	killOldMap();
    	super.onDetach();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.i("map test", "on resume called");
        mage.registerCacheOverlayListener(this);

        // Check if any map preferences changed that I care about
        boolean locationServiceEnabled = preferences.getBoolean("locationServiceEnabled", false);
        map.setMyLocationEnabled(locationServiceEnabled);

        if (locationServiceEnabled) {
            map.setLocationSource(this);
            locationService.registerOnLocationListener(this);
        }

        updateMapType();
        updateObservations();
    }

    @Override
    public void onPause() {
        super.onPause();

        mage.unregisterCacheOverlayListener(this);

        boolean locationServiceEnabled = Integer.parseInt(preferences.getString("userReportingFrequency", "0")) > 0;
        if (locationServiceEnabled) {
            map.setLocationSource(null);
            locationService.unregisterOnLocationListener(this);
        }
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // TODO Add your menu entries here
        super.onCreateOptionsMenu(menu, inflater);
        
        inflater.inflate(R.menu.landing, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.observation_new:
                 Intent intent = new Intent(getActivity(), ObservationEditActivity.class);
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
    public void onObservationCreated(final Collection<Observation> o) {
        new AddObservationTask(observations).execute(o.toArray(new Observation[o.size()]));
    }

    @Override
    public void onObservationUpdated(final Observation o) {
        new UpdateObservationTask(observations).execute(o);
    }

    @Override
    public void onObservationDeleted(final Observation o) {
        new DeleteObservationTask(observations).execute(o);
    }

    @Override
    public void onMapLongClick(LatLng point) {
        // TODO Auto-generated method stub
        Intent intent = new Intent(getActivity(), ObservationEditActivity.class);
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
            Intent i = new Intent(getActivity(), MapPreferencesActivity.class);
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
        Set<String> overlays = preferences.getStringSet("mapTileOverlays", Collections.<String> emptySet());

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

    private void updateMapType() {
        int mapType = Integer.parseInt(preferences.getString("mapBaseLayer", "1"));
        if (mapType != this.mapType) {
            this.mapType = mapType;
            map.setMapType(this.mapType);
        }
    }


    private void updateObservations() {
      boolean showObservations = preferences.getBoolean("showObservations", true);
      observations.setVisible(showObservations);

        
        // TODO think of a way to fix this if we want users to be able to swap
        // between clusters and not clusters
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
    }

    @Override
    public void onError(Throwable error) {
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if ("activeTimeFilter".equalsIgnoreCase(key)) {
			Log.i("map test", "Active filter changed to: " + sharedPreferences.getInt(key, 0));
			updateTimeFilter(sharedPreferences.getInt(key, 0));
		}
		
	}
	
	private void updateTimeFilter(int filterId) {
		switch(filterId) {
		case R.id.none_rb:
			// no filter
			break;
		case R.id.last_hour_rb:
			
			break;
		case R.id.last_six_hours_rb:
			
			break;
		case R.id.last_twelve_hours_rb:
			
			break;
		case R.id.last_24_hours_rb:
			
			break;
		case R.id.since_midnight_rb:
			
			break;
		default:
			// just set no filter
			break;
		}
	}
}