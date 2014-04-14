package mil.nga.giat.mage.map;

import mil.nga.giat.mage.map.marker.LocationMarkerCollection;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import android.os.AsyncTask;

public class LocationTask extends AsyncTask<Location, Location, Void> {
    public enum Type {
        ADD,
        UPDATE,
        DELETE
    }
    
    private Type type;
    private LocationMarkerCollection locationCollection;
    
    public LocationTask(Type type, LocationMarkerCollection locationCollection) {
        this.type = type;
        this.locationCollection = locationCollection;
    }

    @Override
    protected Void doInBackground(Location... locations) {
        for (Location l : locations) {
            publishProgress(l);
        }
        
        return null;
    }

    @Override
    protected void onProgressUpdate(Location... locations) {
        switch (type) {
            case ADD: {
                locationCollection.add(locations[0]);
                break;
            }
            case UPDATE : {
                locationCollection.remove(locations[0]);
                locationCollection.add(locations[0]);
                break;
            }
            case DELETE : {
                locationCollection.remove(locations[0]);
                break;
            }
        }
    }
}