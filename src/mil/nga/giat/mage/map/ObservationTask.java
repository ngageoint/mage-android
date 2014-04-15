package mil.nga.giat.mage.map;

import mil.nga.giat.mage.map.marker.ObservationCollection;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import android.os.AsyncTask;

public class ObservationTask extends AsyncTask<Observation, Observation, Void> {
    public enum Type {
        ADD, UPDATE, DELETE
    }

    private Type type;
    private ObservationCollection observationCollection;

    public ObservationTask(Type type, ObservationCollection observationCollection) {
        this.type = type;
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