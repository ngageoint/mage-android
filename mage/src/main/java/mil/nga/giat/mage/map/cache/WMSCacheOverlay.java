package mil.nga.giat.mage.map.cache;

import java.net.URL;
import java.util.Map;

public class WMSCacheOverlay extends URLCacheOverlay{

    private final Map<String, String> myParameters;

    public WMSCacheOverlay(String name, URL url, String format, Map<String, String> params) {
        super(name, url, format);
        this.myParameters = params;
    }

    public Map<String, String> getParameters(){return this.myParameters;};
}
