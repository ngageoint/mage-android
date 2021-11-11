package mil.nga.giat.mage.coordinate;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;

import mil.nga.giat.mage.R;
import mil.nga.mgrs.MGRS;

public class CoordinateFormatter {
    private CoordinateSystem coordinateSystem;
    private DecimalFormat latLngFormat = new DecimalFormat("###.00000");

    public CoordinateFormatter(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int preferenceValue = preferences.getInt(context.getString(R.string.coordinateSystemViewKey), context.getResources().getInteger(R.integer.coordinateSystemViewDefaultValue));
        coordinateSystem = CoordinateSystem.get(preferenceValue);
    }

    public CoordinateSystem getCoordinateSystem() {
        return coordinateSystem;
    }

    public String format(LatLng latLng) {
        switch(coordinateSystem) {
            case MGRS:
                return getMGRS(latLng);
            default:
                return getWGS84(latLng);
        }
    }

    private String getWGS84(LatLng latLng) {
        return latLngFormat.format(latLng.latitude) + ", " + latLngFormat.format(latLng.longitude);
    }

    private String getMGRS(LatLng latLng) {
        MGRS mgrs = MGRS.from(new mil.nga.mgrs.wgs84.LatLng(latLng.latitude, latLng.longitude));
        return mgrs.format(5);
    }
}
