package mil.nga.giat.mage.map.marker;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.MarkerManager;
import com.vividsolutions.jts.geom.Point;

public class LocationMarkerCollection implements PointCollection<Location>, OnMarkerClickListener {

	private static final String LOG_NAME = LocationMarkerCollection.class.getName();
	
    private GoogleMap map;
    private Context context;
    private Date latestLocationDate = new Date(0);

    private InfoWindowAdapter infoWindowAdpater = new LocationInfoWindowAdapter();

    private boolean visible = true;

    private Map<Long, Marker> locationIdToMarker = new ConcurrentHashMap<Long, Marker>();
    private Map<String, Location> markerIdToLocation = new ConcurrentHashMap<String, Location>();

    private MarkerManager.Collection markerCollection;
    
    protected User currentUser = null;
    
    public LocationMarkerCollection(Context context, GoogleMap map) {
        this.context = context;
        this.map = map;

        MarkerManager markerManager = new MarkerManager(map);
        markerCollection = markerManager.newCollection();
        
		try {
			currentUser = UserHelper.getInstance(context.getApplicationContext()).readCurrentUser();
		} catch (UserException e) {
			Log.e(LOG_NAME, "Problem retriving current user.", e);
		}
    }

    @Override
    public void add(Location l) {
        // If I got an observation that I already have in my list
        // remove it from the map and clean-up my collections
        Marker marker = locationIdToMarker.remove(l.getId());
        if (marker != null) {
            markerIdToLocation.remove(marker.getId());
            marker.remove();
        }
        
        removeOldMarkers();
        //only add markers that are NOT the current user
		if (currentUser != null && !currentUser.getRemoteId().equals(l.getUser().getRemoteId())) {
		
			Point point = l.getLocationGeometry().getGeometry().getCentroid();

			MarkerOptions options = new MarkerOptions()
					.position(new LatLng(point.getY(), point.getX()))
					.icon(LocationBitmapFactory.bitmapDescriptor(context, l))
					.visible(visible);

			marker = markerCollection.addMarker(options);

			locationIdToMarker.put(l.getId(), marker);
			markerIdToLocation.put(marker.getId(), l);

			if (l.getTimestamp().after(latestLocationDate)) {
				latestLocationDate = l.getTimestamp();
			}
		}
    }

    @Override
    public void addAll(Collection<Location> locations) {
        for (Location l : locations) {
            add(l);
        }
    }
    
    @Override
    public void remove(Location l) {
        Marker marker = locationIdToMarker.remove(l.getId());
        if (marker != null) {
            markerIdToLocation.remove(marker.getId());
            markerCollection.remove(marker);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Location l = markerIdToLocation.get(marker.getId());
                
        if (l == null) return false;
        
        map.setInfoWindowAdapter(infoWindowAdpater);
        marker.setIcon(LocationBitmapFactory.bitmapDescriptor(context, l));
        marker.showInfoWindow();
        return true;
    }
    
    @Override
    public void clear() {
        locationIdToMarker.clear();
        markerIdToLocation.clear();
        markerCollection.clear();
        latestLocationDate = new Date(0);
    }   

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        // Don't care about this, I am not clustered
    }

    @Override
    public void setVisibility(boolean visible) {
        if (this.visible == visible)
            return;
        
        this.visible = visible;
        for (Marker m : locationIdToMarker.values()) {
            m.setVisible(visible);
        }        
    }

    @Override
    public Date getLatestDate() {
        return latestLocationDate;
    }
    
    /**
     * Used to remove markers for locations that have been removed from the local datastore.
     */
    public void removeOldMarkers() {
    	LocationHelper lh = LocationHelper.getInstance(context.getApplicationContext());
    	Set<Long> locationIds = locationIdToMarker.keySet();
    	for(Long locationId : locationIds) {    		    		    		
  			Location locationExists = new Location();
   			locationExists.setId(locationId);
   			if(!lh.exists(locationExists)) {   				
   				Marker marker = locationIdToMarker.remove(locationId);
   		        if (marker != null) {
   		            markerIdToLocation.remove(marker.getId());
   		            marker.remove();
   		        }   				
   			}
   		}   		
   	}    	
    
    private class LocationInfoWindowAdapter implements InfoWindowAdapter {
        
        private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm zz", Locale.ENGLISH);
        
        @Override
        public View getInfoContents(Marker marker) {
            Location location = markerIdToLocation.get(marker.getId());
            Log.i("marker", "location marker getInfoContents event");
            
            if (location == null) return null;
            
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.people_list_item, null);    

            ImageView iconView = (ImageView) view.findViewById(R.id.iconImageView);
            Bitmap iconMarker = LocationBitmapFactory.bitmap(context, location);
            if (iconMarker != null)
                iconView.setImageBitmap(iconMarker);            
            TextView userView = (TextView) view.findViewById(R.id.username);
            User user = location.getUser();
            if (user != null) userView.setText(user.getFirstname() + " " + user.getLastname());

            
            TextView dateView = (TextView) view.findViewById(R.id.location_date);
            Date date = location.getTimestamp();
			dateView.setText(sdf.format(date));
            
            return view;
        }

        @Override
        public View getInfoWindow(Marker marker) {           
            return null;  // Use default info window for now
        }
    }
}
