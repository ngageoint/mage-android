package mil.nga.giat.mage.map;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.observation.ObservationEditActivity;
import mil.nga.giat.mage.sdk.location.LocationService;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

/**
 * 
 * 
 * @author newmanw
 * 
 */

public class MapFragment extends Fragment implements OnClickListener, LocationSource, LocationListener {

    private GoogleMap map;
    private int mapType = 1;
    private boolean followMe = false;
    private OnLocationChangedListener locationChangedListener;
    private Map<String, TileOverlay> tileOverlays = new HashMap<String, TileOverlay>();
        
    private LocationService locationService;
    
    SharedPreferences preferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        map = ((SupportMapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

        mapType = Integer.parseInt(preferences.getString("baseLayer", "1"));
        map.setMapType(mapType);

        map.setOnMapLongClickListener(new OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng point) {
                // TODO Auto-generated method stub
                Intent intent = new Intent(getActivity(), ObservationEditActivity.class);
                intent.putExtra("latitude", point.latitude);
                intent.putExtra("longitude", point.longitude);
                startActivity(intent);
            }
        });

        ImageButton mapSettings = (ImageButton) view.findViewById(R.id.map_settings);
        mapSettings.setOnClickListener(this);
        
        locationService = ((MAGE) getActivity().getApplication()).getLocationService();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check if any map preferences changed that I care about        
        followMe = preferences.getBoolean("followMe", false);
        
        boolean locationServiceEnabled = preferences.getBoolean("locationServiceEnabled", false);
        map.setMyLocationEnabled(locationServiceEnabled);

        if (locationServiceEnabled) {
            map.setLocationSource(this);
            locationService.registerOnLocationListener(this);
        }

        updateMapType();
        updateMapOverlays();
    }
    
    @Override
    public void onPause() {
        super.onPause();

        boolean locationServiceEnabled = Integer.parseInt(preferences.getString("userReportingFrequency", "0")) > 0;
        if (locationServiceEnabled) {
            map.setLocationSource(null);
            locationService.unregisterOnLocationListener(this);
        }
    }

    public void onMapSettingsClick(View target) {
        Intent i = new Intent(getActivity(), MapPreferencesActivity.class);
        startActivity(i);
    }

    private void updateMapType() {
        int mapType = Integer.parseInt(preferences.getString("mapBaseLayer", "1"));
        if (mapType != this.mapType) {
            this.mapType = mapType;
            map.setMapType(this.mapType);
        }
    }

    private void updateMapOverlays() {
        Set<String> overlays = preferences.getStringSet("mapTileOverlays", Collections.<String> emptySet());

        // Add all overlays that are in the preferences
        // For now there is no ordering in how tile overlays are stacked

        Set<String> removedOverlays = new HashSet<String>(tileOverlays.keySet());
        for (String overlay : overlays) {
            if (!tileOverlays.keySet().contains(overlay)) {
                TileProvider tileProvider = new FileSystemTileProvider(256, 256, overlay);
                TileOverlay tileOverlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
                tileOverlays.put(overlay, tileOverlay);
            }

            removedOverlays.remove(overlay);
        }

        // Remove any overlays that are on the map but no longer in the
        // preferences
        for (String overlay : removedOverlays) {
            tileOverlays.remove(overlay).remove();
        }
    }

    public void showLayersPopup(View view) {
        Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.map_preference);
        dialog.setCancelable(true);
        dialog.show();
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
    public void activate(OnLocationChangedListener listener) {
        locationChangedListener = listener;
    }

    @Override
    public void deactivate() {
        locationChangedListener = null;        
    }

    @Override
    public void onLocationChanged(Location location) {        
        if (locationChangedListener != null) {
            locationChangedListener.onLocationChanged(location);
        }    
        
        if (!followMe) return;

        LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (!bounds.contains(latLng)) {
            // Move the camera to the user's location once it's available!
            map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}
}