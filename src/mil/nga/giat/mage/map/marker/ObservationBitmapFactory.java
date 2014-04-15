package mil.nga.giat.mage.map.marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;

import org.apache.commons.lang3.StringUtils;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

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
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDensity = 480;
        options.inTargetDensity = context.getResources().getDisplayMetrics().densityDpi;
        try {
            bitmap = BitmapFactory.decodeStream(context.getAssets().open(asset), null, options); 
        } catch (IOException e1) {
            try {
                bitmap = BitmapFactory.decodeStream(context.getAssets().open(DEFAULT_ASSET), null, options);
            } catch (IOException e2) {
            }
            
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
        
		Map<String, ObservationProperty> properties = observation.getPropertiesMap();
		ObservationProperty level = properties.get(LEVEL_PROPERTY);
		ObservationProperty type = properties.get(TYPE_PROPERTY);

        Collection<String> paths = new ArrayList<String>();
        paths.add("markers");
        
        if (level != null) {
            paths.add(level.getValue().replaceAll(pattern.pattern(), "_").toLowerCase(Locale.ENGLISH));
            
            if (type != null) {
                paths.add(type.getValue().replaceAll(pattern.pattern(), "_").toLowerCase(Locale.ENGLISH));
            } else {
                paths.add("default");
            }
        } else if (type != null) {
            paths.add(type.getValue().replaceAll(pattern.pattern(), "_").toLowerCase(Locale.ENGLISH));
        } else {            
            paths.add("default");
        }
 
        return StringUtils.join(paths, "/") + ".png";
    }
}