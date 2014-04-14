package mil.nga.giat.mage.map.marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import mil.nga.giat.mage.sdk.datastore.location.Location;

import org.apache.commons.lang3.StringUtils;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.DisplayMetrics;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class LocationBitmapFactory {

    private static final String DEFAULT_ASSET = "people/person.png";
           
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static Bitmap bitmap(Context context, Location location) {
        String asset = getAsset(location);

        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(context.getAssets().open(asset)); 
        } catch (IOException e1) {
            try {
                bitmap = BitmapFactory.decodeStream(context.getAssets().open(DEFAULT_ASSET));
            } catch (IOException e2) {
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            bitmap.setDensity(DisplayMetrics.DENSITY_XXHIGH);
        } else {
            bitmap.setDensity(DisplayMetrics.DENSITY_XHIGH);

        }
        
        return bitmap;
    }
    
    public static BitmapDescriptor bitmapDescriptor(Context context, Location location) {
        Bitmap bitmap = bitmap(context, location);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
    
    private static String getAsset(Location location) {
        if (location == null) {
            return DEFAULT_ASSET;
        }

        Collection<String> paths = new ArrayList<String>();
        paths.add("people");
        
        Long interval = System.currentTimeMillis() - location.getLastModified().getTime();
        if (interval < 600000) {  // 10 minutes
            paths.add("low");
        } else if (interval < 1800000) { // 30 minutes
            paths.add("medium");
        } else {
            paths.add("high");
        }
 
        paths.add("person");
        return StringUtils.join(paths, "/") + ".png";
    }
}