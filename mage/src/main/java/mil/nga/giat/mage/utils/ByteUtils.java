package mil.nga.giat.mage.utils;

import android.annotation.SuppressLint;

import java.util.Locale;

public class ByteUtils {

    private static final ByteUtils sInstance = new ByteUtils();

    /**
     * singleton
     */
    private ByteUtils(){

    }

    public static ByteUtils getInstance(){
        return sInstance;
    }

    public String getDisplay(long bytes, boolean isSI){

        int unit = isSI ? 1000 : 1024;

        String txt = "";

        if (bytes < unit) {
            txt = bytes + " B";
        }else {
            int exp = (int) (Math.log(bytes) / Math.log(unit));
            String pre = (isSI ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (isSI ? "" : "i");
            txt = String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(unit, exp), pre);
        }

        return txt;
    }
}
