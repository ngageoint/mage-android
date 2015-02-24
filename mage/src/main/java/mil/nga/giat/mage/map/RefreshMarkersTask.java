package mil.nga.giat.mage.map;

import mil.nga.giat.mage.map.marker.PointCollection;
import android.os.AsyncTask;
import android.util.Log;

public class RefreshMarkersTask extends AsyncTask<Integer, Void, Void> {

	private static final String LOG_NAME = RefreshMarkersTask.class.getName();

	PointCollection<?> collection;

	public RefreshMarkersTask(PointCollection<?> collection) {
		this.collection = collection;
	}

	@Override
	protected Void doInBackground(Integer... sleepTimeInSeconds) {
		long sleepTimeInMilli = 30000l;
		
		if(sleepTimeInSeconds != null && sleepTimeInSeconds.length > 0) {
			sleepTimeInMilli = sleepTimeInSeconds[0]*1000l;
		}		
		
		while (!isCancelled()) {
			publishProgress();
			try {
				Thread.sleep(sleepTimeInMilli);
			} catch (InterruptedException ie) {
				//Log.e(LOG_NAME, "Interrupted.  Unable to sleep ", ie);
			}
		}
		return null;
	}
	
	@Override
	protected void onProgressUpdate(Void... values) {
		super.onProgressUpdate(values);
		if(collection.isVisible()) {
			collection.refreshMarkerIcons();
		}
	}

}