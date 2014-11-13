package mil.nga.giat.mage.map;

import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.map.marker.PointCollection;
import mil.nga.giat.mage.sdk.Temporal;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import android.os.AsyncTask;

public class LocationTask extends AsyncTask<Location, Location, Void> {
    public enum Type {
        ADD,
        UPDATE,
        DELETE
    }
    
    private Type type;
    private PointCollection<Location> locationCollection;
    private Filter<Temporal> filter;

    public LocationTask(Type type, PointCollection<Location> locationCollection) {
        this.type = type;
        this.locationCollection = locationCollection;
    }

    public void setFilter(Filter<Temporal> filter) {
        this.filter = filter;
    }
    
    @Override
    protected Void doInBackground(Location... locations) {        
    	for (Location l : locations) {
            if (filter != null && !filter.passesFilter(l)) {
            	continue;
            }

            publishProgress(l);
        }
        
        return null;
    }

    @Override
    protected void onProgressUpdate(Location... locations) {
    	synchronized (locationCollection) {
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
}