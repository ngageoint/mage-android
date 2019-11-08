package mil.nga.giat.mage.map.preference;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ListFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ExpandableListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.cache.CacheUtils;
import mil.nga.giat.mage.map.cache.CacheOverlay;
import mil.nga.giat.mage.map.cache.CacheOverlayFilter;
import mil.nga.giat.mage.map.cache.CacheProvider;
import mil.nga.giat.mage.map.cache.CacheProvider.OnCacheOverlayListener;
import mil.nga.giat.mage.map.cache.GeoPackageCacheOverlay;
import mil.nga.giat.mage.map.cache.StaticFeatureCacheOverlay;
import mil.nga.giat.mage.map.cache.XYZDirectoryCacheOverlay;
import mil.nga.giat.mage.map.download.GeoPackageDownloadManager;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.exceptions.LayerException;
import mil.nga.giat.mage.sdk.fetch.StaticFeatureServerFetch;
import mil.nga.giat.mage.sdk.gson.deserializer.LayerDeserializer;
import mil.nga.giat.mage.sdk.http.HttpClientManager;
import mil.nga.giat.mage.sdk.http.resource.LayerResource;
import mil.nga.giat.mage.sdk.utils.StorageUtility;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * This activity is the downloadable layers section and deals with geopackages and static features
 */
public class TileOverlayPreferenceActivity extends AppCompatActivity {

    private static final String LOG_NAME = TileOverlayPreferenceActivity.class.getName();

    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 100;

    private OverlayListFragment overlayFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cache_overlay);

        overlayFragment = (OverlayListFragment) getSupportFragmentManager().findFragmentById(R.id.overlay_fragment);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putStringArrayListExtra(MapPreferencesActivity.OVERLAY_EXTENDED_DATA_KEY, overlayFragment.getSelectedOverlays());
        setResult(Activity.RESULT_OK, intent);


        synchronized (overlayFragment.timerLock) {
            if (this.overlayFragment.downloadRefreshTimer != null) {
                this.overlayFragment.downloadRefreshTimer.cancel();
            }
        }

        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class OverlayListFragment extends ListFragment implements OnCacheOverlayListener {
        private DownloadableLayersAdapter adapter;
        private final Object adapterLock = new Object();
        private ExpandableListView listView;
        private View progress;
        private MenuItem refreshButton;
        private GeoPackageDownloadManager downloadManager;
        private Timer downloadRefreshTimer;
        private final Object timerLock = new Object();

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setHasOptionsMenu(true);

            downloadManager = new GeoPackageDownloadManager(getActivity().getApplicationContext(), new GeoPackageDownloadManager.GeoPackageDownloadListener() {
                @Override
                public void onGeoPackageDownloaded(final Layer layer, final CacheOverlay overlay) {

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (adapterLock) {
                                adapter.addOverlay(overlay, layer);
                                adapter.notifyDataSetChanged();
                            }
                        }
                    });
                }
            });

            adapter = new DownloadableLayersAdapter(getActivity(), downloadManager);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_cache_overlay, container, false);
            listView = view.findViewById(android.R.id.list);
            listView.setEnabled(true);
            listView.setAdapter(adapter);

            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    int itemType = ExpandableListView.getPackedPositionType(id);
                    if (itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                        // TODO Handle child row long clicks here
                        return true;
                    } else if (itemType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                        int groupPosition = ExpandableListView.getPackedPositionGroup(id);

                        synchronized (adapterLock) {
                            Object group = adapter.getGroup(groupPosition);
                            if (group instanceof CacheOverlay) {
                                CacheOverlay cacheOverlay = (CacheOverlay) adapter.getGroup(groupPosition);
                                deleteCacheOverlayConfirm(cacheOverlay);
                                return true;
                            }
                        }

                        return false;
                    }

                    return false;
                }
            });

            progress = view.findViewById(R.id.progress);
            progress.setVisibility(View.GONE);

            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle)
                            .setTitle(R.string.overlay_access_title)
                            .setMessage(R.string.overlay_access_message)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                                }
                            })
                            .create()
                            .show();

                } else {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                }
            }

            return view;
        }

        @Override
        public void onResume() {
            super.onResume();

            CacheProvider.getInstance(getActivity()).registerCacheOverlayListener(this, false);

            downloadManager.onResume();

            synchronized (timerLock) {
                downloadRefreshTimer = new Timer();
                downloadRefreshTimer.schedule(new GeopackageDownloadProgressTimer(getActivity()), 0, 2000);
            }
        }

        @Override
        public void onPause() {
            super.onPause();

            CacheProvider.getInstance(getActivity()).unregisterCacheOverlayListener(this);
            
            downloadManager.onPause();

            synchronized (timerLock) {
                if (downloadRefreshTimer != null) {
                    downloadRefreshTimer.cancel();
                }
            }
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.tile_overlay_menu, menu);

            refreshButton = menu.findItem(R.id.tile_overlay_refresh);
            refreshButton.setEnabled(true);

            manualRefresh(refreshButton);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.tile_overlay_refresh:
                    manualRefresh(item);
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }

        /**
         * This is called when the user click the refresh button
         *
         * @param item refresh button
         */
        @UiThread
        private void manualRefresh(MenuItem item) {
            item.setEnabled(false);
            progress.setVisibility(View.VISIBLE);
            listView.setEnabled(false);
            synchronized (adapterLock) {
                adapter.getDownloadableLayers().clear();
                adapter.getOverlays().clear();
                adapter.notifyDataSetChanged();
            }

            @SuppressLint("StaticFieldLeak") AsyncTask<Void, Void, List<Layer>> fetcher = new AsyncTask<Void, Void, List<Layer>>() {
                @Override
                protected List<Layer> doInBackground(Void... objects) {
                    final Semaphore lock = new Semaphore(1);
                    lock.drainPermits();
                    fetchGeopackageLayers(new Callback<Collection<Layer>>() {
                        @Override
                        public void onResponse(Call<Collection<Layer>> call, Response<Collection<Layer>> response) {
                            try {
                                if (response.isSuccessful()) {
                                    saveGeopackageLayers(response.body());
                                }
                            } finally {
                                lock.release();
                            }
                        }

                        @Override
                        public void onFailure(Call<Collection<Layer>> call, Throwable t) {
                            Log.e(LOG_NAME, "Error fetching event geopackage layers", t);
                            lock.release();
                        }
                    });
                    try {
                        lock.acquire();
                    }catch(InterruptedException e) {
                        Log.d(LOG_NAME, "Interrupted while waiting for semaphore",e);
                    }
                    fetchStaticLayers();

                    final Event event = EventHelper.getInstance(getActivity().getApplicationContext()).getCurrentEvent();

                    List<Layer> layers = new ArrayList<>();
                    try {
                        for (Layer layer : LayerHelper.getInstance(getActivity().getApplicationContext()).readByEvent(event, "GeoPackage")) {
                            if (!layer.isLoaded()) {
                                layers.add(layer);
                            }
                        }
                        for (Layer layer : LayerHelper.getInstance(getActivity().getApplicationContext()).readByEvent(event, "Feature")) {
                            if (!layer.isLoaded()) {
                                layers.add(layer);
                            }
                        }
                    } catch (LayerException e) {
                    }

                    CacheProvider.getInstance(getActivity().getApplicationContext()).refreshTileOverlays();

                    return layers;
                }

                @Override
                protected void onPostExecute(List<Layer> layers) {
                    super.onPostExecute(layers);

                    synchronized (adapterLock){
                        adapter.getDownloadableLayers().addAll(layers);
                        Collections.sort(adapter.getDownloadableLayers(), new LayerNameComparator());
                        adapter.notifyDataSetChanged();
                    }
                    refreshButton.setEnabled(true);
                    progress.setVisibility(View.GONE);
                    listView.setEnabled(true);
                }
            };
            fetcher.execute();
        }

        /**
         * This reads the remote layers from the server but does not download them
         *
         */
        private void fetchStaticLayers(){
            StaticFeatureServerFetch staticFeatureServerFetch = new StaticFeatureServerFetch(getContext());
            staticFeatureServerFetch.fetch(false, null);
        }

        private void fetchGeopackageLayers(Callback<Collection<Layer>> callback) {
            Context context = getContext();
            Event event = EventHelper.getInstance(context).getCurrentEvent();
            String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(mil.nga.giat.mage.sdk.R.string.serverURLKey), context.getString(mil.nga.giat.mage.sdk.R.string.serverURLDefaultValue));
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create(LayerDeserializer.getGsonBuilder(event)))
                    .client(HttpClientManager.getInstance().httpClient())
                    .build();

            LayerResource.LayerService service = retrofit.create(LayerResource.LayerService.class);

            service.getLayers(event.getRemoteId(), "GeoPackage").enqueue(callback);
        }

        private void saveGeopackageLayers(Collection<Layer> remoteLayers) {
            Context context = getActivity().getApplicationContext();
            LayerHelper layerHelper = LayerHelper.getInstance(context);
            try {
                // get local layers
                Collection<Layer> localLayers = layerHelper.readAll("GeoPackage");

                Map<String, Layer> remoteIdToLayer = new HashMap<>(localLayers.size());
                for(Layer layer : localLayers){
                    remoteIdToLayer.put(layer.getRemoteId(), layer);
                }

                GeoPackageManager manager = GeoPackageFactory.getManager(context);
                for (Layer remoteLayer : remoteLayers) {
                    // Check if its loaded
                    File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                            String.format("MAGE/geopackages/%s/%s", remoteLayer.getRemoteId(), remoteLayer.getFileName()));
                    if (file.exists() && manager.existsAtExternalFile(file)) {
                        remoteLayer.setLoaded(true);
                    }
                    if(!localLayers.contains(remoteLayer)) {
                        layerHelper.create(remoteLayer);
                    }else {
                        Layer localLayer = remoteIdToLayer.get(remoteLayer.getRemoteId());
                        //Only remove a local layer if the even has changed
                        if (!remoteLayer.getEvent().equals(localLayer.getEvent())) {
                            layerHelper.delete(localLayer.getId());
                            layerHelper.create(remoteLayer);
                        }
                    }
                }
            } catch (LayerException e) {
                Log.e(LOG_NAME, "Error saving geopackage layers", e);
            }
        }

        @Override
        public void onCacheOverlay(final List<CacheOverlay> cacheOverlays) {
            final Event event = EventHelper.getInstance(getActivity().getApplicationContext()).getCurrentEvent();

            synchronized (adapterLock) {
                this.adapter.getOverlays().removeAll(cacheOverlays);

                //Here we are only handling static overlays.  geopackage overlays will be handled later on
                for (CacheOverlay overlay : cacheOverlays) {
                    if(overlay instanceof StaticFeatureCacheOverlay) {
                        adapter.getOverlays().add(overlay);
                    }
                }
                Collections.sort(adapter.getOverlays());
                adapter.notifyDataSetChanged();
            }

            List<Layer> geopackages = Collections.EMPTY_LIST;
            try {
                geopackages = LayerHelper.getInstance(getActivity().getApplicationContext()).readByEvent(event, "GeoPackage");
            } catch (LayerException e) {
                Log.w(LOG_NAME, "Error reading geopackage layers",e);
            }

            downloadManager.reconcileDownloads(geopackages, new GeoPackageDownloadManager.GeoPackageLoadListener() {
                @Override
                public void onReady(List<Layer> layers) {

                    synchronized (adapterLock){
                        adapter.getDownloadableLayers().removeAll(layers);
                        adapter.getDownloadableLayers().addAll(layers);
                        Collections.sort(adapter.getDownloadableLayers(), new LayerNameComparator());
                        List<CacheOverlay> filtered = new CacheOverlayFilter(getContext(), event).filter(cacheOverlays);
                        for(CacheOverlay overlay : filtered) {
                            if(overlay instanceof GeoPackageCacheOverlay) {
                                adapter.getOverlays().add(overlay);
                            }
                        }
                        Collections.sort(adapter.getOverlays());
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            switch (requestCode) {
                case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                    if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        CacheProvider.getInstance(getActivity()).refreshTileOverlays();
                    }

                    break;
                }
            }
        }

        /**
         * Get the selected cache overlays and child cache overlays
         *
         * @return added cache overlays
         */
        @UiThread
        public ArrayList<String> getSelectedOverlays() {
            ArrayList<String> overlays = new ArrayList<>();
            for (CacheOverlay cacheOverlay : CacheProvider.getInstance(getContext()).getCacheOverlays()) {
                if(cacheOverlay instanceof GeoPackageCacheOverlay) {
                    boolean childAdded = false;
                    for (CacheOverlay childCache : cacheOverlay.getChildren()) {
                        if (childCache.isEnabled()) {
                            overlays.add(childCache.getCacheName());
                            childAdded = true;
                        }
                    }

                    if (!childAdded && cacheOverlay.isEnabled()) {
                        overlays.add(cacheOverlay.getCacheName());
                    }
                }
            }

            return overlays;
        }

        /**
         * Delete the cache overlay
         * @param cacheOverlay
         */
        private void deleteCacheOverlayConfirm(final CacheOverlay cacheOverlay) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog deleteDialog = new AlertDialog.Builder(getActivity())
                            .setTitle("Delete Layer")
                            .setMessage("Delete " + cacheOverlay.getName() + " Layer?")
                            .setPositiveButton("Delete",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog,
                                                            int which) {
                                            deleteCacheOverlay(cacheOverlay);
                                        }
                                    })

                            .setNegativeButton(getString(R.string.cancel),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog,
                                                            int which) {
                                            dialog.dismiss();
                                        }
                                    }).create();
                    deleteDialog.show();
                }
            });
        }

        /**
         * Delete the XYZ cache overlay
         * @param xyzCacheOverlay
         */
        private void deleteXYZCacheOverlay(XYZDirectoryCacheOverlay xyzCacheOverlay){

            File directory = xyzCacheOverlay.getDirectory();

            if(directory.canWrite()){
                deleteFile(directory);
            }

        }

        /**
         * Delete the base directory file
         * @param base directory
         */
        private void deleteFile(File base) {
            if (base.isDirectory()) {
                for (File file : base.listFiles()) {
                    deleteFile(file);
                }
            }
            base.delete();
        }

        /**
         * Delete the cache overlay
         * @param cacheOverlay
         */
        @UiThread
        private void deleteCacheOverlay(CacheOverlay cacheOverlay){

            switch(cacheOverlay.getType()) {

                case XYZ_DIRECTORY:
                    deleteXYZCacheOverlay((XYZDirectoryCacheOverlay)cacheOverlay);
                    break;

                case GEOPACKAGE:
                    deleteGeoPackageCacheOverlay((GeoPackageCacheOverlay)cacheOverlay);
                    break;

                case STATIC_FEATURE:
                    deleteStaticFeatureCacheOverlay((StaticFeatureCacheOverlay)cacheOverlay);
                    break;

            }

            synchronized (adapterLock) {
                adapter.getOverlays().clear();
                adapter.notifyDataSetChanged();
            }

            manualRefresh(refreshButton);
        }

        /**
         * Delete the GeoPackage cache overlay
         * @param geoPackageCacheOverlay
         */
        private void deleteGeoPackageCacheOverlay(GeoPackageCacheOverlay geoPackageCacheOverlay) {

            String database = geoPackageCacheOverlay.getName();

            // Get the GeoPackage file
            GeoPackageManager manager = GeoPackageFactory.getManager(getActivity());
            File path = manager.getFile(database);

            // Delete the cache from the GeoPackage manager
            manager.delete(database);

            // Attempt to delete the cache file if it is in the cache directory
            File pathDirectory = path.getParentFile();
            if(path.canWrite() && pathDirectory != null) {
                Map<StorageUtility.StorageType, File> storageLocations = StorageUtility.getWritableStorageLocations();
                for (File storageLocation : storageLocations.values()) {
                    File root = new File(storageLocation, getString(R.string.overlay_cache_directory));
                    if (root.equals(pathDirectory)) {
                        path.delete();
                        break;
                    }
                }
            }

            // Check internal/external application storage
            File applicationCacheDirectory = CacheUtils.getApplicationCacheDirectory(getActivity());
            if (applicationCacheDirectory != null && applicationCacheDirectory.exists()) {
                for (File cache : applicationCacheDirectory.listFiles()) {
                    if (cache.equals(path)) {
                        path.delete();
                        break;
                    }
                }
            }

            if (path.getAbsolutePath().startsWith(String.format("%s/MAGE/geopackages", getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)))) {
                LayerHelper layerHelper = LayerHelper.getInstance(getActivity().getApplicationContext());

                try {
                    String relativePath = path.getAbsolutePath().split(getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/")[1];
                    Layer layer = layerHelper.getByRelativePath(relativePath);
                    if (layer != null) {
                        layer.setLoaded(false);
                        layer.setDownloadId(null);
                        layerHelper.update(layer);
                    }
                } catch (LayerException e) {
                    Log.e(LOG_NAME, "Error setting loaded to false for path " + path, e);
                }

                if (!path.delete()) {
                    Log.e(LOG_NAME, "Error deleting geopackage file from filesystem for path " + path);
                }
            }
        }

        private void deleteStaticFeatureCacheOverlay(StaticFeatureCacheOverlay cacheOverlay) {
            try {
                LayerHelper.getInstance(getContext()).delete(cacheOverlay.getId());
            } catch (LayerException e) {
                Log.w(LOG_NAME, "Failed to delete static feature " + cacheOverlay.getCacheName() ,e);
            }
        }

        private class GeopackageDownloadProgressTimer extends TimerTask {
            private boolean canceled = false;

            private final Activity myActivity;

            public GeopackageDownloadProgressTimer(Activity activity){
                myActivity = activity;
            }

            @Override
            public void run() {
                synchronized (timerLock) {
                    if (!canceled) {
                        try {
                            updateGeopackageDownloadProgress();
                        }catch(Exception e){
                            //ignore
                        }
                    }
                }
            }

            @Override
            public boolean cancel() {
                synchronized (timerLock) {
                    canceled = true;
                }
                return super.cancel();
            }

            private void updateGeopackageDownloadProgress() {
                myActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (adapterLock) {
                            if(adapter == null){
                                return;
                            }
                            try {
                                List<Layer> layers = adapter.getDownloadableLayers();
                                for(Layer layer : layers){
                                    if(layer.getDownloadId() == null || layer.isLoaded()){
                                        continue;
                                    }

                                    for(int i = 0; i < layers.size(); i++) {
                                        final View view = listView.getChildAt(i);
                                        if (view == null || view.getTag() == null || !view.getTag().equals(layer.getName())) {
                                            continue;
                                        }

                                        adapter.updateDownloadProgress(view, downloadManager.getProgress(layer), layer.getFileSize());
                                    }
                                }
                            } catch (Exception e) {
                                //ignore
                            }
                        }
                    }
                });
            }
        }
    }
}