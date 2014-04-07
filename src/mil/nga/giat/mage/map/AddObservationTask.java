package mil.nga.giat.mage.map;

import mil.nga.giat.mage.map.marker.ObservationCollection;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import android.os.AsyncTask;

public class AddObservationTask extends AsyncTask<Observation, Observation, Void> {
    private ObservationCollection observationCollection;
    
    public AddObservationTask(ObservationCollection observationCollection) {
        this.observationCollection = observationCollection;
    }

    @Override
    protected Void doInBackground(Observation... observations) {
        for (Observation o : observations) {
            publishProgress(o);
        }
        
        return null;
    }

    @Override
    protected void onProgressUpdate(Observation... observations) {
        observationCollection.add(observations[0]);
    }
}