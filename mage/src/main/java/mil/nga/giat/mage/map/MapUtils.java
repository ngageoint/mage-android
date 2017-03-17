package mil.nga.giat.mage.map;

import com.google.android.gms.maps.GoogleMap;

/**
 * Map utilities
 */
public class MapUtils {

    /**
     * Get the map point to line distance tolerance
     *
     * @param map map
     * @return
     */
    public static double lineTolerance(GoogleMap map) {
        // how many meters away form the click can the geometry be?
        double circumferenceOfEarthInMeters = 2 * Math.PI * 6371000;
        double pixelSizeInMetersAtLatitude = (circumferenceOfEarthInMeters * Math.cos(map.getCameraPosition().target.latitude * (Math.PI / 180.0))) / Math.pow(2.0, map.getCameraPosition().zoom + 8.0);
        double tolerance = pixelSizeInMetersAtLatitude * Math.sqrt(2.0) * 10.0;
        return tolerance;
    }

}
