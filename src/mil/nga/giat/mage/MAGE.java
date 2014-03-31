package mil.nga.giat.mage;

import mil.nga.giat.mage.sdk.fetch.RoleServerFetchAsyncTask;
import mil.nga.giat.mage.sdk.location.LocationService;
import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

public class MAGE extends Application {

	private static final String LOG_NAME = MAGE.class.getName();
	
	private LocationService locationService;

	public void initLocationService() {
		if (locationService == null) {
			locationService = new LocationService(getApplicationContext());
			locationService.init();
		}
	}

	public void destroyLocationService() {
		if (locationService != null) {
			locationService.destroy();
			locationService = null;
		}
	}

	public LocationService getLocationService() {
		return locationService;
	}

	public void startFetching() {
		RoleServerFetchAsyncTask roleTask = new RoleServerFetchAsyncTask(getApplicationContext());
		try {
			roleTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} catch (Exception e) {
			Log.e(LOG_NAME, "Error fetching!  Could not populate role table!");
		}
	}
}