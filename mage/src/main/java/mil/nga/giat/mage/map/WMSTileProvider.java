package mil.nga.giat.mage.map;

import android.util.Log;

import com.google.android.gms.maps.model.UrlTileProvider;

import java.net.MalformedURLException;
import java.net.URL;

import mil.nga.giat.mage.map.cache.URLCacheOverlay;
import mil.nga.giat.mage.map.cache.WMSCacheOverlay;

public class WMSTileProvider extends UrlTileProvider {

    private static final String LOG_NAME = WMSTileProvider.class.getName();

    private final WMSCacheOverlay myOverlay;
    private final int myWidth;
    private final int myHeight;

    public WMSTileProvider(int width, int height, URLCacheOverlay overlay) {
        super(width, height);
        myWidth = width;
        myHeight = height;
        myOverlay = (WMSCacheOverlay)overlay;
    }

    public WMSCacheOverlay getOverlay (){return myOverlay;}

    @Override
    public URL getTileUrl(int x, int y, int z) {
        final String version = myOverlay.getWmsVersion();
        String epsgKey = "SRS";
        if (version != null && (version.equals("1.3") || version.equals("1.3.0"))) {
            epsgKey = "CRS";
        }

        String transparentValue = "false";
        if (myOverlay.getWmsTransparent() != null) {
            Boolean transparent = Boolean.parseBoolean(myOverlay.getWmsTransparent());
            if (transparent.equals(Boolean.TRUE)) {
                transparentValue = "true";
            }
        }

        final StringBuilder path = new StringBuilder(myOverlay.getURL().toString());

        path.append("?request=GetMap&service=WMS");
        if(myOverlay.getWmsStyles() != null){
            path.append("&styles=" + myOverlay.getWmsStyles());
        }
        if(myOverlay.getWmsLayers() != null){
            path.append("&layers=" + myOverlay.getWmsLayers());
        }
        if(version != null) {
            path.append("&version=" + version);
        }
        path.append("&" + epsgKey + "=EPSG:3857");
        path.append("&width=" + myWidth);
        path.append("&height=" + myHeight);
        path.append("&format=" + myOverlay.getWmsFormat());
        path.append("&transparent=" + transparentValue);
        path.append(buildBBox(x,y,z));

        URL newPath = null;

        try {
            newPath = new URL(path.toString());
        } catch (MalformedURLException e) {
            Log.w(LOG_NAME, "Problem with URL " + path, e);
        }

        return newPath;
    }

    private double getX(int x, int z){
        return x / Math.pow(2.0, z) * 360.0 - 180;
    }

    private double getY(int y, int z){
        double n = Math.PI - 2.0 * Math.PI * y / Math.pow(2.0, z);
        return 180.0 / Math.PI * Math.atan(0.5 * (Math.exp(n) - Math.exp (-n)));
    }

    private double mercatorXOfLongitude(double lon){
        return lon * 20037508.34 / 180;
    }

    private double mercatorYOfLatitude(double lat){
        double y = Math.log(Math.tan((90 + lat) * Math.PI / 360)) / (Math.PI / 180);
        y = y * 20037508.34 / 180;
        return y;
    }

    private String buildBBox(int x, int y, int z){
        double left =  mercatorXOfLongitude(getX(x, z));
        double right = mercatorXOfLongitude(getX(x+1 ,z));
        double bottom = mercatorYOfLatitude(getY(y+1, z));
        double top = mercatorYOfLatitude(getY(y, z));

        return "&BBOX=" + left +"," + bottom + "," + right + "," + top;
    }
}