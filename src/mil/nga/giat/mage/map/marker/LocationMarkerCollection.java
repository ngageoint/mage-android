package mil.nga.giat.mage.map.marker;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.utils.DateUtility;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.MarkerManager;
import com.vividsolutions.jts.geom.Point;

public class LocationMarkerCollection implements OnMarkerClickListener {

    private GoogleMap map;
    private Context context;
    
    private InfoWindowAdapter infoWindowAdpater = new LocationInfoWindowAdapter();

    private boolean visible = true;

    private Map<Long, Marker> locationIdToMarker = new ConcurrentHashMap<Long, Marker>();
    private Map<String, Location> markerIdToLocation = new ConcurrentHashMap<String, Location>();

    private MarkerManager.Collection markerCollection;

    public LocationMarkerCollection(Context context, GoogleMap map) {
        this.context = context;
        this.map = map;

        MarkerManager markerManager = new MarkerManager(map);
        markerCollection = markerManager.newCollection();        
    }

    public void add(Location l) {
        Point point = l.getLocationGeometry().getGeometry().getCentroid();
        
        MarkerOptions options = new MarkerOptions()
            .position(new LatLng(point.getY(), point.getX()))
            .icon(LocationBitmapFactory.bitmapDescriptor(context, l))
            .visible(visible);

        Marker marker = markerCollection.addMarker(options);

        locationIdToMarker.put(l.getId(), marker);
        markerIdToLocation.put(marker.getId(), l);
    }

    public void addAll(Collection<Location> locations) {
        for (Location l : locations) {
            add(l);
        }
    }

    public Collection<Location> getLocations() {
        return markerIdToLocation.values();
    }

    public void setVisible(boolean visible) {
        if (this.visible == visible)
            return;
        
        this.visible = visible;
        for (Marker m : locationIdToMarker.values()) {
            m.setVisible(visible);
        }
    }

    public void setLocationVisibility(Location o, boolean visible) {        
        locationIdToMarker.get(o.getId()).setVisible(this.visible && visible);
    }

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
        marker.showInfoWindow();
        return true;
    }
    
    public void clear() {
        locationIdToMarker.clear();
        markerIdToLocation.clear();
        markerCollection.clear();
    }
    
    private class LocationInfoWindowAdapter implements InfoWindowAdapter {
        
        private DateFormat iso8601 =  DateUtility.getISO8601();
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
            String dateText = location.getPropertiesMap().get("timestamp");
            try {
                Date date = iso8601.parse(dateText);
                dateText = sdf.format(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            dateView.setText(dateText);
            
            return view;
        }

        @Override
        public View getInfoWindow(Marker marker) {           
            return null;  // Use default info window for now
        }
    }
}