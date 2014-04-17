package mil.nga.giat.mage.map.marker;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.utils.DateUtility;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
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

    private Context context;
    private Collection<Filter<Location>> filters = new ArrayList<Filter<Location>>();

    private boolean collectionVisible = true;

    private Map<Long, Marker> locationIdToMarker = new ConcurrentHashMap<Long, Marker>();
    private Map<String, Location> markerIdToLocation = new ConcurrentHashMap<String, Location>();

    private MarkerManager.Collection markerCollection;

    public LocationMarkerCollection(Context context, GoogleMap map) {
        this.context = context;

        MarkerManager markerManager = new MarkerManager(map);
        markerCollection = markerManager.newCollection();
        
        map.setInfoWindowAdapter(new LocationInfoWindowAdapter());
    }

    public void add(Location l) {
        Point point = l.getLocationGeometry().getGeometry().getCentroid();
        
        MarkerOptions options = new MarkerOptions()
            .position(new LatLng(point.getY(), point.getX()))
            .icon(LocationBitmapFactory.bitmapDescriptor(context, l))
            .visible(isLocationVisible(l));

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

    public void setVisible(boolean collectionVisible) {
        if (this.collectionVisible == collectionVisible)
            return;
        
        this.collectionVisible = collectionVisible;
        for (Marker m : locationIdToMarker.values()) {
            Location l = markerIdToLocation.get(m.getId());
            m.setVisible(isLocationVisible(l));
        }
    }

    public void setLocationVisibility(Location o, boolean visible) {        
        locationIdToMarker.get(o.getId()).setVisible(this.collectionVisible && visible);
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
                
        if (l == null) return true;
        
        marker.showInfoWindow();
        return true;
    }
    
    public void clear() {
        locationIdToMarker.clear();
        markerIdToLocation.clear();
        markerCollection.clear();
    }

    public void setFilters(Collection<Filter<Location>> filters) {
        this.filters = filters;

        // re-filter based on new filter
        new FilterLocationsTask().execute();
    }

    private boolean isLocationVisible(Location l) {
        boolean isVisible = collectionVisible;

        // Only check filter if the collection is visible
        if (isVisible) {
            for (Filter<Location> filter : filters) {
                if (!filter.passesFilter(l)) {
                    isVisible = false;
                    break;
                }
            }
        }

        return isVisible;
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

    private class FilterLocationsTask extends AsyncTask<Void, Map<Location, Boolean>, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            for (Location l : markerIdToLocation.values()) {
                publishProgress(Collections.<Location, Boolean> singletonMap(l, isLocationVisible(l)));
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Map<Location, Boolean>... locations) {
            for (Map.Entry<Location, Boolean> entry : locations[0].entrySet()) {
                LocationMarkerCollection.this.setLocationVisibility(entry.getKey(), entry.getValue());
            }            
        }
    }
}