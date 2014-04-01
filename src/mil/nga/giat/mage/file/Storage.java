package mil.nga.giat.mage.file;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import android.os.Environment;

public class Storage {

    
    public enum StorageType {
        LOCAL("SD Card"),
        EXTERNAL("External SD Card");
        
        private String name;
        
        private StorageType(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }
    
    public static File getDefaultStorageLocation() {
        return getDefaultStorageLocation(1);
    }
    
    public static File getDefaultStorageLocation(long bytes) {
        Map<StorageType, File> map = getAllStorageLocations();
        
        if(map.isEmpty()) {
            return null;
        }
        
        File ex = map.get(StorageType.EXTERNAL);
        if(ex != null && ex.exists() && ex.canWrite() && ex.getUsableSpace() >= bytes) {
            return ex;
        }
        ex = map.get(StorageType.LOCAL);
        if(ex != null && ex.exists() && ex.canWrite() && ex.getUsableSpace() >= bytes) {
            return ex;
        }
        return null;
    }
    
    /**
     * @return A map of all storage locations available
     */
    public static Map<StorageType, File> getAllStorageLocations() {
        Map<StorageType, File> map = new LinkedHashMap<StorageType, File>();

        List<String> mMounts = new ArrayList<String>();
        List<String> mVold = new ArrayList<String>();
        mMounts.add("/mnt/sdcard");
        mVold.add("/mnt/sdcard");

        try {
            File mountFile = new File("/proc/mounts");
            if (mountFile.exists()) {
                Scanner scanner = new Scanner(mountFile);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    if (line.startsWith("/dev/block/vold/")) {
                        String[] lineElements = line.split(" ");
                        String element = lineElements[1];

                        // don't add the default mount path
                        // it's already in the list.
                        if (!element.equals("/mnt/sdcard"))
                            mMounts.add(element);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            File voldFile = new File("/system/etc/vold.fstab");
            if (voldFile.exists()) {
                Scanner scanner = new Scanner(voldFile);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    if (line.startsWith("dev_mount")) {
                        String[] lineElements = line.split(" ");
                        String element = lineElements[2];

                        if (element.contains(":"))
                            element = element.substring(0, element.indexOf(":"));
                        if (!element.equals("/mnt/sdcard"))
                            mVold.add(element);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < mMounts.size(); i++) {
            String mount = mMounts.get(i);
            if (!mVold.contains(mount))
                mMounts.remove(i--);
        }
        mVold.clear();

        List<String> mountHash = new ArrayList<String>(10);

        for (String mount : mMounts) {
            File root = new File(mount);
            if (root.exists() && root.isDirectory() && root.canWrite()) {
                File[] list = root.listFiles();
                String hash = "[";
                if (list != null) {
                    for (File f : list) {
                        hash += f.getName().hashCode() + ":" + f.length() + ", ";
                    }
                }
                hash += "]";
                if (!mountHash.contains(hash)) {
                    StorageType key = StorageType.LOCAL;
                    if (map.size() == 0) {
                        key = StorageType.LOCAL;
                    } else if (map.size() == 1) {
                        key = StorageType.EXTERNAL;
                    }
                    mountHash.add(hash);
                    map.put(key, root);
                }
            }
        }

        mMounts.clear();

        if (map.isEmpty()) {
            map.put(StorageType.LOCAL, Environment.getExternalStorageDirectory());
        }
        
        return map;
    }
}