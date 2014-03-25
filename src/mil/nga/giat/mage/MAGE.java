package mil.nga.giat.mage;

import mil.nga.giat.mage.sdk.location.LocationService;
import android.app.Application;

public class MAGE extends Application {

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
}