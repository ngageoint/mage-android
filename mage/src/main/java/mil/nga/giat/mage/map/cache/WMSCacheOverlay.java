package mil.nga.giat.mage.map.cache;

import java.net.URL;

import mil.nga.giat.mage.database.model.layer.Layer;

public class WMSCacheOverlay extends URLCacheOverlay{

    private final String wmsFormat;

    private final String wmsVersion;

    private final String wmsLayers;

    private final String wmsStyles;

    private final String wmsTransparent;

    public WMSCacheOverlay(String name, URL url, Layer layer) {
        super(name, url, layer);
        this.wmsFormat = layer.getWmsFormat();
        this.wmsVersion = layer.getWmsVersion();
        this.wmsTransparent = layer.getWmsTransparent();
        this.wmsStyles = layer.getWmsStyles();
        this.wmsLayers = layer.getWmsLayers();

    }

    public String getWmsFormat() {
        return wmsFormat;
    }

    public String getWmsVersion() {
        return wmsVersion;
    }

    public String getWmsTransparent() {
        return wmsTransparent;
    }

    public String getWmsLayers() {
        return wmsLayers;
    }

    public String getWmsStyles() {
        return wmsStyles;
    }
}

