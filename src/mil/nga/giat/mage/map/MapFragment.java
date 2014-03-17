package mil.nga.giat.mage.map;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.R.id;
import mil.nga.giat.mage.R.layout;
import mil.nga.giat.mage.observation.ObservationEditActivity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

/**
 * FIXME: Currently a mock of what a landing page might look like.
 * 
 * @author wiedemannse
 * 
 */
public class MapFragment extends Fragment {
	
    private GoogleMap map;
    private int mapType = 1;
    private Map<String, TileOverlay> tileOverlays = new HashMap<String, TileOverlay>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View view = inflater.inflate(R.layout.fragment_map, container, false);
      
      map = ((SupportMapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
      
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
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
      
      return view;
    }
    
	@Override
	public void onResume() {
		super.onResume();

		// Check if any map preferences changed that I care about
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		updateMapType(preferences);
		updateMapOverlays(preferences);
	}
	
	private void updateMapType(SharedPreferences preferences) {
		int mapType = Integer.parseInt(preferences.getString("mapBaseLayer", "1"));
		if (mapType != this.mapType) {
			this.mapType = mapType;
			map.setMapType(this.mapType);
		}
	}
	
	private void updateMapOverlays(SharedPreferences preferences) {
		Set<String> overlays = preferences.getStringSet("mapTileOverlays", Collections.<String>emptySet());
			
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
		
		// Remove any overlays that are on the map but no longer in the preferences
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
}