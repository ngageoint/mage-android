package mil.nga.giat.mage.cache;

import android.app.Activity;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

/**
 * Task for copying a cache file Uri stream to the cache folder location
 */
public class CopyCacheStreamTask extends AsyncTask<Void, Void, String> {

    /**
     * Activity
     */
    private Activity activity;

    /**
     * Intent Uri used to launch MAGE
     */
    private Uri uri;

    /**
     * Cache file to create
     */
    private File cacheFile;

    /**
     * Constructor
     *
     * @param activity
     * @param uri       Uri containing stream
     * @param cacheFile copy to cache file location
     */
    public CopyCacheStreamTask(Activity activity, Uri uri, File cacheFile) {
        this.activity = activity;
        this.uri = uri;
        this.cacheFile = cacheFile;
    }

    /**
     * Copy the cache stream to cache file location
     *
     * @param params
     * @return
     */
    @Override
    protected String doInBackground(Void... params) {

        String error = null;

        final ContentResolver resolver = activity.getContentResolver();
        try {
            InputStream stream = resolver.openInputStream(uri);
            MediaUtility.copyStream(stream, cacheFile);
        } catch (IOException e) {
            error = e.getMessage();
        }

        return error;
    }

    /**
     * Enable the new cache file and refresh the overlays
     *
     * @param result
     */
    @Override
    protected void onPostExecute(String result) {
        if (result == null) {
            MAGE mage = ((MAGE) activity.getApplication());
            String cacheName = MediaUtility.getFileNameWithoutExtension(cacheFile);
            mage.enableOverlay(cacheName);
            mage.refreshTileOverlays();
        }
    }

}
