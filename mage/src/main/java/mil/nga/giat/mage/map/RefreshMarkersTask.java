package mil.nga.giat.mage.map;

import android.os.AsyncTask;

import java.util.Iterator;

import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.map.marker.PointCollection;
import mil.nga.giat.mage.sdk.Temporal;

public class RefreshMarkersTask extends AsyncTask<Integer, Void, Void> {

	private static final String LOG_NAME = RefreshMarkersTask.class.getName();

	PointCollection<?> collection;
	Filter<Temporal> filter;

	public RefreshMarkersTask(PointCollection<?> collection, Filter<Temporal> filter) {
		this.collection = collection;
		this.filter = filter;
	}

	@Override
	protected Void doInBackground(Integer... sleepTimeInSeconds) {
		System.out.println("Refreshing markers");

//		long sleepTimeInMilli = 30000l;
		
//		if(sleepTimeInSeconds != null && sleepTimeInSeconds.length > 0) {
//			sleepTimeInMilli = sleepTimeInSeconds[0]*1000l;
//		}
		
//		while (!isCancelled()) {
		// loop the collection and publish progress on certain ones but for now...
			publishProgress();
//			try {
//				Thread.sleep(sleepTimeInMilli);
//			} catch (InterruptedException ie) {
//				//Log.e(LOG_NAME, "Interrupted.  Unable to sleep ", ie);
//			}
//		}
		return null;
	}
	
	@Override
	protected void onProgressUpdate(Void... values) {
		super.onProgressUpdate(values);
		if(collection.isVisible()) {
			collection.refreshMarkerIcons(filter);
		}
	}

}