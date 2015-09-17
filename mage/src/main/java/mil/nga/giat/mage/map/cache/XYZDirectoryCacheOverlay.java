package mil.nga.giat.mage.map.cache;

import java.io.File;

import mil.nga.giat.mage.R;

/**
 * XYZ Directory of tiles cache overlay
 */
public class XYZDirectoryCacheOverlay extends CacheOverlay {

    /**
     * Tile directory
     */
    private File directory;

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

    public Integer getIconImageResourceId() {
        return R.drawable.ic_layers;
    }

    public File getDirectory() {
        return directory;
    }

}
