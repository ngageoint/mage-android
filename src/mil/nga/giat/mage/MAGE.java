package mil.nga.giat.mage;

import mil.nga.giat.mage.sdk.location.LocationService;
import android.app.Application;

public class MAGE extends Application {

    private LocationService locationService;
    
    public void startLocationService() {
        if (locationService == null) {
            locationService = new LocationService(getApplicationContext());
            locationService.start();
          }
    }
    
    public void stopLocationService() {
        if (locationService != null) {
            locationService.stop();
            locationService = null;
        }
    }
    
    public LocationService getLocationService() {
        return locationService;
    }
}