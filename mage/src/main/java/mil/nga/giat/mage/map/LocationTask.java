package mil.nga.giat.mage.map;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Collection;

import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.filter.LocationEventFilter;
import mil.nga.giat.mage.map.marker.LocationBitmapFactory;
import mil.nga.giat.mage.map.marker.PointCollection;
import mil.nga.giat.mage.sdk.Temporal;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.sf.Point;
import mil.nga.sf.util.GeometryUtils;

public class LocationTask extends AsyncTask<Location, Pair<MarkerOptions, Pair<Location, User>>, Void> {
    public enum Type {
        ADD,
        UPDATE,
        DELETE
    }

    private Context context;
    private Type type;
    private final PointCollection<Pair<Location, User>> locationCollection;
    private Collection<Filter<?>> filters = new ArrayList<>();

    public LocationTask(Context context, Type type, PointCollection<Pair<Location, User>> locationCollection) {
        this.context = context;
        this.type = type;
        this.locationCollection = locationCollection;

        Event currentEvent = EventHelper.getInstance(context).getCurrentEvent();
        filters.add(new LocationEventFilter(currentEvent));
    }

    public void addFilter(Filter<Temporal> filter) {
        if (filter == null) return;

        filters.add(filter);
    }
    
    @Override
    protected Void doInBackground(Location... locations) {        
    	for (Location location : locations) {
            User user = location.getUser();
            if (user == null) {
                continue;
            }

            boolean passesFilter = true;
            for (Filter filter : filters) {
                passesFilter = filter.passesFilter(location);
                if (!passesFilter) {
                    break;
                }
            }

            if (passesFilter) {
                Point point = GeometryUtils.getCentroid(location.getGeometry());
                LatLng latLng = new LatLng(point.getY(), point.getX());
                MarkerOptions options = new MarkerOptions().position(latLng).icon(LocationBitmapFactory.bitmapDescriptor(context, location, user));

                publishProgress(new Pair<>(options, new Pair<>(location, user)));
            }
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