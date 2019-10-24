package mil.nga.giat.mage.map;

import android.support.v4.math.MathUtils;
import android.util.Log;

import com.google.android.gms.maps.model.UrlTileProvider;

import java.net.MalformedURLException;
import java.net.URL;

import mil.nga.giat.mage.map.cache.URLCacheOverlay;
import mil.nga.giat.mage.map.cache.WMSCacheOverlay;

public class WMSTileProvider extends UrlTileProvider {

    private static final String LOG_NAME = WMSTileProvider.class.getName();

    private final WMSCacheOverlay myOverlay;

    public WMSTileProvider(int width, int height, URLCacheOverlay overlay) {
        super(width, height);

        myOverlay = (WMSCacheOverlay)overlay;
    }

    @Override
    public URL getTileUrl(int x, int y, int z) {
        String version = myOverlay.getParameters().get("version");
        String epsgKey = "SRS";
        if(version.equals("1.3")){
            epsgKey = "CRS";
        }

        Integer transparent = Integer.parseInt(myOverlay.getParameters().get("transparent"));
        String transparentValue = "false";
        if(transparent.equals(new Integer(1))){
            transparentValue = "true";
        }

        String path = myOverlay.getURL().toString();

        path = path + "?request=GetMap&service=WMS&styles="
                + myOverlay.getParameters().get("styles")
                + "&layers=" + myOverlay.getParameters().get("layers")
                + "&version=" + myOverlay.getParameters().get("version")
                + "&" + epsgKey +" =EPSG:3857&width=256&height=256&format="
                + myOverlay.getParameters().get("format")
                + "&transparent=" + transparentValue;

        URL newPath = null;

        try {
            newPath = new URL(path);
        } catch (MalformedURLException e) {
            Log.w(LOG_NAME, "Problem with URL " + path, e);
        }

        return newPath;
    }

    public double getX(int x, int z){
        return x / Math.pow(2.0, z) * 360.0 - 180;
    }

    public double getY(int y, int z){
        double n = Math.PI - 2.0 * Math.PI * y / Math.pow(2.0, z);
        return 180.0 / Math.PI * Math.atan(0.5 * (Math.exp(n) - Math.exp (-n)));
    }

    public double mercatorXOfLongitude(double lon){
        return lon * 20037508.34 / 180;
    }

    public double mercatorYOfLatitude(double lat){
        double y = Math.log(Math.tan((90 + lat) * Math.PI / 360)) / (Math.PI / 180);
        y = y * 20037508.34 / 180;
        return y;
    }


}