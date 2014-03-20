package mil.nga.giat.mage;

import mil.nga.giat.mage.sdk.location.LocationService;
import android.app.Application;

public class MAGE extends Application {

    private LocationService locationService;
    
    public LocationService getLocationService() {
        return locationService;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        locationService = new LocationService(getApplicationContext());
        locationService.start();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        
        locationService.stop();
    }   
}