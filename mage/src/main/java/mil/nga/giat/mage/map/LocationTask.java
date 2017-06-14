package mil.nga.giat.mage.map;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.vividsolutions.jts.geom.Point;

import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.map.marker.LocationBitmapFactory;
import mil.nga.giat.mage.map.marker.PointCollection;
import mil.nga.giat.mage.sdk.Temporal;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.User;

public class LocationTask extends AsyncTask<Location, Pair<MarkerOptions, Pair<Location, User>>, Void> {
    public enum Type {
        ADD,
        UPDATE,
        DELETE
    }

    private Context context;
    private Type type;
    private final PointCollection<Pair<Location, User>> locationCollection;
    private Filter<Temporal> filter;

    public LocationTask(Context context, Type type, PointCollection<Pair<Location, User>> locationCollection) {
        this.context = context;
        this.type = type;
        this.locationCollection = locationCollection;
    }

    public void setFilter(Filter<Temporal> filter) {
        this.filter = filter;
    }
    
    @Override
    protected Void doInBackground(Location... locations) {        
    	for (Location location : locations) {
            if (filter != null && !filter.passesFilter(location)) {
            	continue;
            }

            Point point = location.getGeometry().getCentroid();
            LatLng latLng = new LatLng(point.getY(), point.getX());
            MarkerOptions options = new MarkerOptions().position(latLng).icon(LocationBitmapFactory.bitmapDescriptor(context, location, location.getUser()));

            publishProgress(new Pair<>(options, new Pair<>(location, location.getUser())));
        }
        
        return null;
    }

    @Override
    protected void onProgressUpdate(Pair<MarkerOptions, Pair<Location, User>>... pairs) {
        switch (type) {
            case ADD: {
                locationCollection.add(pairs[0].first, pairs[0].second);
                break;
            }
            case UPDATE : {
                locationCollection.remove(pairs[0].second);
                locationCollection.add(pairs[0].first, pairs[0].second);
                break;
            }
            case DELETE : {
                locationCollection.remove(pairs[0].second);
                break;
            }
        }
    }
}