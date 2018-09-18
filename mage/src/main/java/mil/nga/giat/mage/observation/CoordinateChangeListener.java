package mil.nga.giat.mage.observation;

import com.google.android.gms.maps.model.LatLng;

public interface CoordinateChangeListener {
    void onCoordinateChangeStart(LatLng coordinate);
    void onCoordinateChangeEnd(LatLng coordinate);
    void onCoordinateChanged(LatLng coordinate);
}
