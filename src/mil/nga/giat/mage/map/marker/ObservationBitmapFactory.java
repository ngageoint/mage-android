package mil.nga.giat.mage.map.marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class ObservationBitmapFactory {

    private static final String TYPE_PROPERTY = "TYPE";
    private static final String LEVEL_PROPERTY = "EVENTLEVEL";
    
    private static Pattern pattern = Pattern.compile("\\W");
       
    public static BitmapDescriptor bitmapDescriptor(Context context, Observation observation) {
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
 
        String asset = StringUtils.join(paths, "/") + ".png";
        try {
            context.getAssets().openFd(asset).close();
        } catch (IOException e) {
            asset = "markers/default.png";
        }

        return BitmapDescriptorFactory.fromAsset(asset);      
    }

}