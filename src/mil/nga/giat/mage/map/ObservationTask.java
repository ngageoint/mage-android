package mil.nga.giat.mage.map;

import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.map.marker.PointCollection;
import mil.nga.giat.mage.sdk.Temporal;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import android.os.AsyncTask;

public class ObservationTask extends AsyncTask<Observation, Observation, Void> {
    public enum Type {
        ADD, UPDATE, DELETE
    }

    private Type type;
    private PointCollection<Observation> observationCollection;
    private Filter<Temporal> filter;

    public ObservationTask(Type type, PointCollection<Observation> observationCollection) {
        this.type = type;
        this.observationCollection = observationCollection;
    }
    
    public void setFilter(Filter<Temporal> filter) {
        this.filter = filter;
    }

    @Override
    protected Void doInBackground(Observation... observations) {
        for (Observation o : observations) {
            if (filter != null && !filter.passesFilter(o)) {
            	continue;
            }
            
            publishProgress(o);
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Observation... observations) {
        switch (type) {
            case ADD: {
                observationCollection.add(observations[0]);
                break;
            }
            case UPDATE: {
                observationCollection.remove(observations[0]);
                observationCollection.add(observations[0]);
                break;
            }
            case DELETE: {
                observationCollection.remove(observations[0]);
                break;
            }
        }
    }
}