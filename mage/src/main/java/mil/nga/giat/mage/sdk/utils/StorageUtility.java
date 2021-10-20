package mil.nga.giat.mage.sdk.utils;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class StorageUtility {
    
    public enum StorageType {
        LOCAL("SD Card"),
        EXTERNAL("External SD Card");
        
        private final String name;
        
        StorageType(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }
    
    public static Map<StorageType, File> getWritableStorageLocations() {
        return getStorageLocations(true);
    }

    public static Map<StorageType, File> getReadableStorageLocations() {
        return getStorageLocations(false);
    }
    
    private static Map<StorageType, File> getStorageLocations(boolean writeable) {
        Map<StorageType, File> locations = new LinkedHashMap<>();
        String externalStorage = System.getenv("EXTERNAL_STORAGE");
        if (externalStorage != null) {
            File externalStorageDir = new File(externalStorage);
            if (writeable && externalStorageDir.exists() && externalStorageDir.canWrite()) {
                locations.put(StorageType.LOCAL, externalStorageDir);
            }

            if (!writeable && externalStorageDir.exists() && externalStorageDir.canRead()) {
                locations.put(StorageType.LOCAL, externalStorageDir);
            }
        }
        String secondaryStorage = System.getenv("SECONDARY_STORAGE");
        if (secondaryStorage != null){
            File secondaryStorageDir = new File(secondaryStorage);
            if (writeable && secondaryStorageDir.exists() && secondaryStorageDir.canWrite()) {
                locations.put(StorageType.EXTERNAL, secondaryStorageDir);
            }

            if (!writeable && secondaryStorageDir.exists() && secondaryStorageDir.canRead()) {
                locations.put(StorageType.EXTERNAL, secondaryStorageDir);
            }
        }

        return locations;
    }
}