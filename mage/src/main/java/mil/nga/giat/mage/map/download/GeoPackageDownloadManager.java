package mil.nga.giat.mage.map.download;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mil.nga.geopackage.GeoPackageFactory;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.map.cache.CacheOverlay;
import mil.nga.giat.mage.map.cache.CacheProvider;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.exceptions.LayerException;

public class GeoPackageDownloadManager {

    public interface GeoPackageLoadListener {
        void onReady(List<Layer> layers);
    }

    public interface GeoPackageDownloadListener {
        void onGeoPackageDownloaded(Layer layer, CacheOverlay overlay);
    }

    private static final String LOG_NAME = GeoPackageDownloadManager.class.getName();

    private final Context context;
    private final String baseUrl;
    private final LayerHelper layerHelper;
    private final GeoPackageDownloadListener listener;
    private final DownloadManager downloadManager;
    private final BroadcastReceiver downloadReceiver = new GeoPackageDownloadReceiver();

    private final Object downloadLock = new Object();

    public GeoPackageDownloadManager(Context context, GeoPackageDownloadListener listener) {
        this.context = context;
        baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        this.listener = listener;
        layerHelper = LayerHelper.getInstance(context);
        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    public void onResume() {
        context.registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    public void onPause() {
        context.unregisterReceiver(downloadReceiver);
    }

    public void downloadGeoPackage(Layer layer) {
        String token = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.tokenKey), null);
        final Event event = EventHelper.getInstance(context).getCurrentEvent();

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(String.format("%s/api/events/%s/layers/%s", baseUrl, event.getRemoteId(), layer.getRemoteId())));
        request.setTitle("MAGE GeoPackage Download");
        request.setDescription(String.format("Downloading MAGE GeoPackage %s", layer.getName()));
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, getRelativePath(layer));
        request.addRequestHeader("Authorization", String.format("Bearer %s", token));
        request.addRequestHeader("Accept", "application/octet-stream");
        // TODO test no notification

        synchronized(downloadLock) {
            long downloadId = downloadManager.enqueue(request);

            try {
                layer.setDownloadId(downloadId);
                layerHelper.update(layer);
            } catch (LayerException e) {
                Log.e(LOG_NAME, "Error saving layer download id", e);
            }
        }
    }

    public boolean isDownloading(Layer layer) {
        int status = -1;
        Long downloadId = layer.getDownloadId();

        if (downloadId != null) {
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            try(Cursor cursor = downloadManager.query(query)) {
                status = getDownloadStatus(cursor);
            }
        }

        return status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING;
    }

    public String isFailed(Layer layer) {
        String status = null;
        Long downloadId = layer.getDownloadId();

        if (downloadId != null) {
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            try(Cursor cursor = downloadManager.query(query)) {
                if(DownloadManager.STATUS_FAILED == getDownloadStatus(cursor)) {
                    status = getDownloadFailureStatus(cursor);
                    try {
                        layer.setDownloadId(null);
                        layerHelper.update(layer);
                    } catch (LayerException e) {
                        Log.e(LOG_NAME, "Error saving layer download id", e);
                    }
                }
            }
        }

        return status;
    }

    public void reconcileDownloads(Collection<Layer> layers, GeoPackageLoadListener listener) {
        Predicate notDownloadedPredicate = new Predicate<Layer>() {
            @Override
            public boolean apply(Layer layer) {
                return !layer.isLoaded();
            }
        };

        List<Layer> notDownloaded = Lists.newArrayList(Iterables.filter(layers, notDownloadedPredicate));
        new GeoPackageLoaderTask(listener).execute(notDownloaded.toArray(new Layer[notDownloaded.size()]));
    }

    public int getProgress(Layer layer) {
        int progress = 0;

        Long downloadId = layer.getDownloadId();
        if (downloadId != null) {
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            try (Cursor cursor = downloadManager.query(query)) {

                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                    if(columnIndex != -1) {
                        progress = cursor.getInt(columnIndex);
                    }
                }
            }
        }

        return progress;
    }

    public String getRelativePath(Layer layer) {
        return String.format("MAGE/geopackages/%s/%s", layer.getRemoteId(), layer.getFileName());
    }

    private void loadGeopackage(long downloadId, GeoPackageDownloadListener listener) {
        synchronized (downloadLock) {
            try {
                Layer layer = layerHelper.getByDownloadId(downloadId);

                if (layer != null) {
                    String relativePath = getRelativePath(layer);
                    GeoPackageManager geoPackageManager = GeoPackageFactory.getManager(context);
                    CacheProvider cacheProvider = CacheProvider.getInstance(context);
                    File cache = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), relativePath);
                    CacheOverlay overlay = cacheProvider.getGeoPackageCacheOverlay(context, cache, geoPackageManager);
                    if (overlay != null) {
                        cacheProvider.addCacheOverlay(overlay);
                    }

                    layer.setRelativePath(relativePath);
                    layer.setLoaded(true);
                    layerHelper.update(layer);

                    if (listener != null) {
                        listener.onGeoPackageDownloaded(layer, overlay);
                    }
                }
            } catch (LayerException e) {
                Log.e(LOG_NAME, "Error saving layer", e);
            }
        }
    }

    private int getDownloadStatus(Cursor cursor) {
        int status = -1;

        if (cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            status = cursor.getInt(columnIndex);
        }

        return status;
    }

    private String getDownloadFailureStatus(Cursor cursor) {
        String status = null;

        if (cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
            if(columnIndex != -1) {
                int reason = cursor.getInt(columnIndex);

                switch (reason) {
                    case DownloadManager.ERROR_CANNOT_RESUME:
                        status = "Cannot Resume";
                        break;
                    case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                        status = "Device Not Found";
                        break;
                    case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                        status = "File Already Exists";
                        break;
                    case DownloadManager.ERROR_FILE_ERROR:
                        status = "File Error";
                        break;
                    case DownloadManager.ERROR_HTTP_DATA_ERROR:
                        status = "HTTP Data Error";
                        break;
                    case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                        status = "Insufficient Space";
                        break;
                    case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                        status = "Too Many Redirects";
                        break;
                    case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                        status = "Unhandled HTTP Code";
                        break;
                    case DownloadManager.ERROR_UNKNOWN:
                    default:
                        status = "Unknown Error";
                        break;
                }
            }
        }

        return status;
    }

    private class GeoPackageDownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO what thread is this running on?

            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                try(Cursor cursor = downloadManager.query(query)) {
                    int status = getDownloadStatus(cursor);

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        loadGeopackage(downloadId, listener);
                    }
                }
            }
        }
    }

    private class GeoPackageLoaderTask extends AsyncTask<Layer, Void, List<Layer>> {

        private GeoPackageLoadListener listener;

        public GeoPackageLoaderTask(GeoPackageLoadListener listener) {
            this.listener = listener;
        }

        @Override
        protected List<Layer> doInBackground(Layer... layers) {
            List<Layer> layersToDownload = new ArrayList<>();

            for (Layer layer : layers) {
                Long downloadId = layer.getDownloadId();
                if (downloadId == null) {
                    layersToDownload.add(layer);
                } else {
                    DownloadManager.Query ImageDownloadQuery = new DownloadManager.Query();
                    ImageDownloadQuery.setFilterById(downloadId);
                    try(Cursor cursor = downloadManager.query(ImageDownloadQuery)) {
                        int status = getDownloadStatus(cursor);

                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            loadGeopackage(downloadId, null);
                        } else {
                            layersToDownload.add(layer);
                        }
                    }
                }
            }

            return layersToDownload;
        }

        @Override
        protected void onPostExecute(List<Layer> layers) {
            listener.onReady(layers);
        }
    }
}
