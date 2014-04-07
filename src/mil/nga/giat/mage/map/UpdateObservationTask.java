package mil.nga.giat.mage.map;

import mil.nga.giat.mage.map.marker.ObservationCollection;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import android.os.AsyncTask;

public class UpdateObservationTask extends AsyncTask<Observation, Observation, Void> {
    private ObservationCollection observationCollection;

    public UpdateObservationTask(ObservationCollection observationCollection) {
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
        // Observations updates happen so rarley lets just
        // delete the old observation and insert the updated one.
        observationCollection.remove(observations[0]);
        observationCollection.add(observations[0]);
    }
}