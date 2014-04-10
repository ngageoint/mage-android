package mil.nga.giat.mage.map;

import java.util.Date;

import mil.nga.giat.mage.map.marker.ObservationCollection;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import android.os.AsyncTask;

public class FilterObservationsTask extends AsyncTask<Observation, Observation, Void> {
    private ObservationCollection observationCollection;
    private Date startDate;
    private Date endDate;

    public FilterObservationsTask(ObservationCollection observationCollection, Date startDate, Date endDate) {
        this.observationCollection = observationCollection;
        this.startDate = startDate;
        this.endDate = endDate;
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
    	if (observations[0].getLastModified().after(startDate) && (endDate == null || observations[0].getLastModified().before(endDate))) {
    		observationCollection.show(observations[0]);
    	} else {
    		observationCollection.hide(observations[0]);
    	}
    }

}
