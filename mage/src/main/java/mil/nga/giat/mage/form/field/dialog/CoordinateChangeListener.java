package mil.nga.giat.mage.form.field.dialog;

import com.google.android.gms.maps.model.LatLng;

public interface CoordinateChangeListener {
    void onCoordinateChangeStart(LatLng coordinate);
    void onCoordinateChangeEnd(LatLng coordinate);
    void onCoordinateChanged(LatLng coordinate);
}
