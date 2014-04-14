package mil.nga.giat.mage.map.marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;

import org.apache.commons.lang3.StringUtils;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.DisplayMetrics;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class ObservationBitmapFactory {

    private static final String DEFAULT_ASSET = "markers/default.png";
    private static final String TYPE_PROPERTY = "type";
    private static final String LEVEL_PROPERTY = "EVENTLEVEL";
    
    private static Pattern pattern = Pattern.compile("\\W");
       
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static Bitmap bitmap(Context context, Observation observation) {
        String asset = getAsset(observation);

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
    
    public static BitmapDescriptor bitmapDescriptor(Context context, Observation observation) {
        Bitmap bitmap = bitmap(context, observation);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
    
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static void setDensity(Bitmap bitmap) {

    }
    
    private static String getAsset(Observation observation) {
        if (observation == null) {
            return DEFAULT_ASSET;
        }
        
        Map<String, String> properties = observation.getPropertiesMap();
        String level = properties.get(LEVEL_PROPERTY);     
        String type = properties.get(TYPE_PROPERTY);

        Collection<String> paths = new ArrayList<String>();
        paths.add("markers");
        
        if (level != null) {
            paths.add(level.replaceAll(pattern.pattern(), "_").toLowerCase(Locale.ENGLISH));
            
            if (type != null) {
                paths.add(type.replaceAll(pattern.pattern(), "_").toLowerCase(Locale.ENGLISH));
            } else {
                paths.add("default");
            }
        } else if (type != null) {
            paths.add(type.replaceAll(pattern.pattern(), "_").toLowerCase(Locale.ENGLISH));
        } else {            
            paths.add("default");
        }
 
        return StringUtils.join(paths, "/") + ".png";
    }
}