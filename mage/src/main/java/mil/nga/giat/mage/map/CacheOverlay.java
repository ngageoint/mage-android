package mil.nga.giat.mage.map;

import java.io.File;

public class CacheOverlay {

    private String name;
    private File directory;
    
    public CacheOverlay(String name, File directory) {
        this.name = name;
        this.directory = directory;
    }

    public String getName() {
        return name;
    }

    public File getDirectory() {
        return directory;
    }
}