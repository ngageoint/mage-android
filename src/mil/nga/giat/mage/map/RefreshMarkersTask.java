package mil.nga.giat.mage.map;

import mil.nga.giat.mage.map.marker.PointCollection;
import android.os.AsyncTask;

public class RefreshMarkersTask extends AsyncTask<Integer, Void, Void> {

	PointCollection<?> colleciton;

	public RefreshMarkersTask(PointCollection<?> colleciton) {
		this.colleciton = colleciton;
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
			} catch (InterruptedException e) {
			}
		}
		return null;
	}
	
	@Override
	protected void onProgressUpdate(Void... values) {
		super.onProgressUpdate(values);
		if(colleciton.isVisible()) {
			colleciton.refreshMarkerIcons();
		}
	}

}