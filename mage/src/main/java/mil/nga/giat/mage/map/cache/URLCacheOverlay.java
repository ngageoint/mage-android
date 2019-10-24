package mil.nga.giat.mage.map.cache;

import com.google.android.gms.maps.model.TileOverlay;

import java.net.URL;

public class URLCacheOverlay extends CacheOverlay {

    private final URL myUrl;

    private final String myFormat;

    /**
     * Tile Overlay
     */
    private TileOverlay tileOverlay;

    public URLCacheOverlay(String name, URL url, String format){
        super(name, CacheOverlayType.URL, false);
        myUrl = url;
        myFormat = format;
    }

    @Override
    public synchronized void removeFromMap() {
        if (tileOverlay != null) {
            tileOverlay.remove();
            tileOverlay = null;
        }
    }

    public URL getURL(){
        return myUrl;
    }
    public String getFormat() {return myFormat;}

    /**
     * Get the tile overlay
     *
     * @return
     */
    public synchronized TileOverlay getTileOverlay() {
        return tileOverlay;
    }

    /**
     * Set the tile overlay
     *
     * @param tileOverlay
     */
    public synchronized void setTileOverlay(TileOverlay tileOverlay) {
        this.tileOverlay = tileOverlay;
    }
}
