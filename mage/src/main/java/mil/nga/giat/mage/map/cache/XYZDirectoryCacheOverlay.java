package mil.nga.giat.mage.map.cache;

import com.google.android.gms.maps.model.TileOverlay;

import java.io.File;

import mil.nga.giat.mage.R;

/**
 * XYZ Directory of tiles cache overlay
 *
 * @author osbornb
 */
public class XYZDirectoryCacheOverlay extends CacheOverlay {

    /**
     * Tile directory
     */
    private File directory;

    /**
     * Tile Overlay
     */
    private TileOverlay tileOverlay;

    /**
     * Constructor
     *
     * @param name      cache name
     * @param directory tile directory
     */
    public XYZDirectoryCacheOverlay(String name, File directory) {
        super(name, CacheOverlayType.XYZ_DIRECTORY, false);
        this.directory = directory;
    }

    @Override
    public void removeFromMap() {
        if (tileOverlay != null) {
            tileOverlay.remove();
            tileOverlay = null;
        }
    }

    @Override
    public Integer getIconImageResourceId() {
        return R.drawable.ic_layers;
    }

    /**
     * Get the directory
     *
     * @return
     */
    public File getDirectory() {
        return directory;
    }

    /**
     * Get the tile overlay
     *
     * @return
     */
    public TileOverlay getTileOverlay() {
        return tileOverlay;
    }

    /**
     * Set the tile overlay
     *
     * @param tileOverlay
     */
    public void setTileOverlay(TileOverlay tileOverlay) {
        this.tileOverlay = tileOverlay;
    }

}
