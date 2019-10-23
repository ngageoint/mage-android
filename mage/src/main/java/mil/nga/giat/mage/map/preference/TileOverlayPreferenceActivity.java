package mil.nga.giat.mage.map.preference;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.giat.mage.MageApplication;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.cache.CacheUtils;
import mil.nga.giat.mage.map.cache.CacheOverlay;
import mil.nga.giat.mage.map.cache.CacheOverlayFilter;
import mil.nga.giat.mage.map.cache.CacheProvider;
import mil.nga.giat.mage.map.cache.CacheProvider.OnCacheOverlayListener;
import mil.nga.giat.mage.map.cache.GeoPackageCacheOverlay;
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
        @Inject
        MageApplication application;

        private OverlayAdapter overlayAdapter;
        private final Object overlayAdapterLock = new Object();
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
                            synchronized (overlayAdapterLock) {
                                overlayAdapter.addOverlay(overlay, layer);
                                overlayAdapter.notifyDataSetChanged();
                            }
                        }
                    });
                }
            });

            AndroidSupportInjection.inject(this);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_cache_overlay, container, false);
            listView = view.findViewById(android.R.id.list);
            listView.setEnabled(true);

            progress = view.findViewById(R.id.progress);
            progress.setVisibility(View.VISIBLE);

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
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            ListView listView = getListView();
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        }

        @Override
        public void onResume() {
            super.onResume();

            downloadManager.onResume();
        }

        @Override
        public void onPause() {
            super.onPause();
            
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
            refreshButton.setEnabled(false);

            // This really should be done in the onResume, but I need to have the refreshButton
            // before I register as the callback will set it to enabled.
            // The problem is that onResume gets called before this so my menu is
            // not yet setup and I will not have a handle on this button
            CacheProvider.getInstance(getActivity()).registerCacheOverlayListener(this);
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
         * @param item
         */
        @UiThread
        private void manualRefresh(MenuItem item) {
            item.setEnabled(false);
            progress.setVisibility(View.VISIBLE);
            listView.setEnabled(false);

            fetchGeopackageLayers(new Callback<Collection<Layer>>() {
                @Override
                public void onResponse(Call<Collection<Layer>> call, Response<Collection<Layer>> response) {
                    if (response.isSuccessful()) {
                        saveGeopackageLayers(response.body());
                        //This will call onCacheOverlay, and we will download static layers there
                        CacheProvider.getInstance(getActivity()).refreshTileOverlays();
                    }
                }

                @Override
                public void onFailure(Call<Collection<Layer>> call, Throwable t) {
                    Log.e(LOG_NAME, "Error fetching event geopackage layers", t);
                }
            });
        }

        private void fetchStaticLayers(){
            //TODO make this act the same was as geopackage (i.e. not fully downloaded)
            //This executes off the current thread
            application.loadStaticFeatures(true, new StaticFeatureServerFetch.OnStaticLayersListener() {
                @Override
                public void onStaticLayersLoaded(final Collection<Layer> layers) {

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (overlayAdapterLock) {
                                overlayAdapter.getStaticFeatures().clear();
                                overlayAdapter.getStaticFeatures().addAll(layers);
                            }
                            refreshButton.setEnabled(true);
                            listView.setEnabled(true);
                            progress.setVisibility(View.GONE);
                            overlayAdapter.notifyDataSetChanged();
                        }
                    });
                }
            });
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

        private void saveGeopackageLayers(Collection<Layer> layers) {
            Context context = getActivity().getApplicationContext();
            LayerHelper layerHelper = LayerHelper.getInstance(context);
            try {
                layerHelper.deleteAll("GeoPackage");

                GeoPackageManager manager = GeoPackageFactory.getManager(context);
                for (Layer layer : layers) {
                    // Check if its loaded
                    File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), String.format("MAGE/geopackages/%s/%s", layer.getRemoteId(), layer.getFileName()));
                    if (file.exists() && manager.existsAtExternalFile(file)) {
                        layer.setLoaded(true);
                    }
                    layerHelper.create(layer);
                }
            } catch (LayerException e) {
                Log.e(LOG_NAME, "Error saving geopackage layers", e);
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            CacheProvider.getInstance(getActivity()).unregisterCacheOverlayListener(this);
        }

        @Override
        public void onCacheOverlay(final List<CacheOverlay> cacheOverlays) {
            List<Layer> geopackages = Collections.EMPTY_LIST;
            final Event event = EventHelper.getInstance(getActivity().getApplicationContext()).getCurrentEvent();
            try {
                geopackages = LayerHelper.getInstance(getActivity().getApplicationContext()).readByEvent(event,"GeoPackage");
            } catch (LayerException e) {
            }

            if(getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                            @Override
                            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                                int itemType = ExpandableListView.getPackedPositionType(id);
                                if (itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                                    int childPosition = ExpandableListView.getPackedPositionChild(id);
                                    int groupPosition = ExpandableListView.getPackedPositionGroup(id);
                                    // Handle child row long clicks here
                                    return true;
                                } else if (itemType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                                    int groupPosition = ExpandableListView.getPackedPositionGroup(id);

                                    synchronized (overlayAdapterLock) {
                                        Object group = overlayAdapter.getGroup(groupPosition);
                                        if (group instanceof CacheOverlay) {
                                            CacheOverlay cacheOverlay = (CacheOverlay) overlayAdapter.getGroup(groupPosition);
                                            deleteCacheOverlayConfirm(cacheOverlay);
                                            return true;
                                        }
                                    }

                                    return false;
                                }

                                return false;
                            }
                        });
                    }
                });
            } else {
                Log.e(LOG_NAME, "No activity available to register callback on the UI thread");
            }

            downloadManager.reconcileDownloads(geopackages, new GeoPackageDownloadManager.GeoPackageLoadListener() {
                @Override
                public void onReady(List<Layer> layers) {
                    synchronized (overlayAdapterLock) {
                        overlayAdapter = new OverlayAdapter(getActivity(), event, cacheOverlays, layers, downloadManager);
                        listView.setAdapter(overlayAdapter);
                    }

                    //TODO test that this gets called even if there are no geopackages
                    fetchStaticLayers();

                    synchronized (timerLock) {
                        downloadRefreshTimer = new Timer();
                        downloadRefreshTimer.schedule(new TimerTask() {
                            private boolean canceled = false;

                            @Override
                            public void run() {
                                synchronized(timerLock) {
                                    if (!canceled) {
                                        updateGeopackageDownloadProgress();
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
                        }, 0, 2000);
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
                    };

                    break;
                }
            }
        }

        /**
         * Get the selected cache overlays and child cache overlays
         *
         * @return
         */
        @UiThread
        public ArrayList<String> getSelectedOverlays() {
            //TODO grab the static ids as well
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

        private void updateGeopackageDownloadProgress() {
            /*
            TODO this method was difficult to put on the UI thread, so as it sits, it is not robust
            */
            if(getActivity() == null) {
                return;
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    synchronized (overlayAdapterLock) {
                        try {
                            List<Layer> layers = overlayAdapter.getGeopackages();
                            for (int i = 0; i < layers.size(); i++) {
                                final Layer layer = layers.get(i);

                                if (layer.getDownloadId() != null && !layer.isLoaded()) {
                                    // layer is currently downloading, get progress and refresh view
                                    final View view = listView.getChildAt(i - listView.getFirstVisiblePosition());
                                    if (view == null) {
                                        continue;
                                    }

                                    Activity activity = getActivity();
                                    if (activity == null) {
                                        continue;
                                    }

                                    overlayAdapter.updateDownloadProgress(view, downloadManager.getProgress(layer), layer.getFileSize());
                                }
                            }
                        }catch(Exception e) {
                            //This is sort of a hack to manage the craziness in the lifecycle
                            //Typically this is an NPE
                        }
                    }
                }
            });
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
                            .setTitle("Delete Cache")
                            .setMessage("Delete " + cacheOverlay.getName() + " Cache?")
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

            progress.setVisibility(View.VISIBLE);
            listView.setEnabled(false);

            switch(cacheOverlay.getType()) {

                case XYZ_DIRECTORY:
                    deleteXYZCacheOverlay((XYZDirectoryCacheOverlay)cacheOverlay);
                    break;

                case GEOPACKAGE:
                    deleteGeoPackageCacheOverlay((GeoPackageCacheOverlay)cacheOverlay);
                    break;

            }

            CacheProvider.getInstance(getActivity()).refreshTileOverlays();
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

    }

    /**
     * Cache Overlay Expandable list adapter
     *
     * <p></p>
     * <b>ALL public methods MUST be made on the UI thread to ensure concurrency.</b>
     */
    @UiThread
    public static class OverlayAdapter extends BaseExpandableListAdapter {

        /**
         * Context
         */
        private final Activity activity;

        /**
         * List of geopackage cache overlays
         */
        private final List<CacheOverlay> geopackageOverlays;

        /**
         * List of geopackages
         */
        private final List<Layer> geopackages;

        /**
         * List of static features such as KML
         */
        private final List<Layer> staticFeatures = new ArrayList<>();

        private final GeoPackageDownloadManager downloadManager;

        /**
         * Constructor
         *
         * @param activity
         * @param overlays
         */
        public OverlayAdapter(Activity activity, Event event, List<CacheOverlay> overlays, List<Layer> geopackages, GeoPackageDownloadManager downloadManager) {
            this.activity = activity;
            this.geopackageOverlays = new CacheOverlayFilter(activity.getApplicationContext(), event).filter(overlays);
            Iterator<CacheOverlay> it = this.geopackageOverlays.iterator();
            while(it.hasNext()){
                CacheOverlay overlay = it.next();
                if(!(overlay instanceof GeoPackageCacheOverlay)) {
                    it.remove();
                }
            }

            this.geopackages = geopackages;
            this.downloadManager = downloadManager;
        }

        public void addOverlay(CacheOverlay overlay, Layer layer) {

            if(overlay instanceof GeoPackageCacheOverlay) {
                if (layer.isLoaded()) {
                    geopackages.remove(layer);
                    geopackageOverlays.add(overlay);
                }
            }
        }

        public List<Layer> getGeopackages() {
            return geopackages;
        }

        public List<Layer> getStaticFeatures(){return this.staticFeatures;}

        public void updateDownloadProgress(View view, int progress, long size) {
            if (progress <= 0) {
                return;
            }

            ProgressBar progressBar = view.findViewById(R.id.layer_progress);
            if (progressBar == null) {
                return;
            }

            int currentProgress = (int) (progress / (float) size * 100);
            progressBar.setProgress(currentProgress);

            TextView layerSize = view.findViewById(R.id.layer_size);
            layerSize.setText(String.format("Downloading: %s of %s",
                Formatter.formatFileSize(activity.getApplicationContext(), progress),
                Formatter.formatFileSize(activity.getApplicationContext(), size)));
        }

        @Override
        public int getGroupCount() {
            return geopackageOverlays.size() + geopackages.size() + staticFeatures.size();
        }

        @Override
        public int getChildrenCount(int i) {
            if (i < geopackages.size() + staticFeatures.size()) {
                return 0;
            } else {
                int children = geopackageOverlays.get(i - geopackages.size() - staticFeatures.size()).getChildren().size();

                for (Layer geopackage : geopackages) {
                    if (geopackage.isLoaded()) {
                        children++;
                    }
                }

                return children;
            }
        }

        @Override
        public Object getGroup(int i) {
            if (i < geopackages.size()) {
                return geopackages.get(i);
            } else if(i < geopackages.size() + staticFeatures.size()) {
                return staticFeatures.get(i - geopackages.size());
            }else {
                return geopackageOverlays.get(i - geopackages.size() - staticFeatures.size());
            }
        }

        @Override
        public Object getChild(int i, int j) {
            return geopackageOverlays.get(i - geopackages.size() - staticFeatures.size()).getChildren().get(j);
        }

        @Override
        public long getGroupId(int i) {
            return i;
        }

        @Override
        public long getChildId(int i, int j) {
            return j;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int i, boolean isExpanded, View view, ViewGroup viewGroup) {
            if (i < geopackages.size()) {
                return getLayerView(i, isExpanded, view, viewGroup);
            } else if(i < geopackages.size() + staticFeatures.size()) {
                return getLayerView(i, isExpanded, view, viewGroup);
            }else {
                return getOverlayView(i, isExpanded, view, viewGroup);
            }
        }

        private View getOverlayView(int i, boolean isExpanded, View view, ViewGroup viewGroup) {
            LayoutInflater inflater = LayoutInflater.from(activity);
            view = inflater.inflate(R.layout.cache_overlay_group, viewGroup, false);

            final CacheOverlay overlay = geopackageOverlays.get(i - geopackages.size() - staticFeatures.size());

            //TODO check this index against static features
            view.findViewById(R.id.section_header).setVisibility(i == geopackages.size() ? View.VISIBLE : View.GONE);

            ImageView imageView = view.findViewById(R.id.cache_overlay_group_image);
            TextView cacheName =  view.findViewById(R.id.cache_overlay_group_name);
            TextView childCount =  view.findViewById(R.id.cache_overlay_group_count);
            CheckBox checkBox =  view.findViewById(R.id.cache_overlay_group_checkbox);

            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean checked = ((CheckBox) v).isChecked();

                    overlay.setEnabled(checked);

                    boolean modified = false;
                    for (CacheOverlay childCache : overlay.getChildren()) {
                        if (childCache.isEnabled() != checked) {
                            childCache.setEnabled(checked);
                            modified = true;
                        }
                    }

                    if (modified) {
                        notifyDataSetChanged();
                    }
                }
            });

            Integer imageResource = overlay.getIconImageResourceId();
            if (imageResource != null) {
                imageView.setImageResource(imageResource);
            }

            Layer layer = null;
            if (overlay instanceof GeoPackageCacheOverlay) {
                String filePath = ((GeoPackageCacheOverlay) overlay).getFilePath();
                if (filePath.startsWith(String.format("%s/MAGE/geopackages", activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)))) {
                    try {
                        String relativePath = filePath.split(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/")[1];
                        layer = LayerHelper.getInstance(activity.getApplicationContext()).getByRelativePath(relativePath);
                    } catch(Exception e) {
                        Log.e(LOG_NAME, "Error getting layer by relative path", e);
                    }
                }
            }
            cacheName.setText(layer != null ? layer.getName() : overlay.getName());

            if (overlay.isSupportsChildren()) {
                childCount.setText("(" + getChildrenCount(i) + ")");
            } else {
                childCount.setText("");
            }
            checkBox.setChecked(overlay.isEnabled());

            return view;
        }

        private View getLayerView(int i, boolean isExpanded, View view, ViewGroup viewGroup) {
            LayoutInflater inflater = LayoutInflater.from(activity);
            view = inflater.inflate(R.layout.layer_overlay, viewGroup, false);

            Layer layer;
            if (i < geopackages.size()) {
                layer = geopackages.get(i);
            } else {
                layer = staticFeatures.get(i - geopackages.size());
            }

            view.findViewById(R.id.section_header).setVisibility(i == 0 ? View.VISIBLE : View.GONE);

            TextView cacheName = view.findViewById(R.id.layer_name);
            cacheName.setText(layer.getName());

            TextView layerSize = view.findViewById(R.id.layer_size);


            final ProgressBar progressBar = view.findViewById(R.id.layer_progress);
            final View download = view.findViewById(R.id.layer_download);
            if (!layer.getType().equals("Feature")) {
                if (downloadManager.isDownloading(layer)) {
                    int progress = downloadManager.getProgress(layer);
                    long fileSize = layer.getFileSize();
                    progressBar.setVisibility(View.VISIBLE);
                    download.setVisibility(View.GONE);

                    view.setEnabled(false);
                    view.setOnClickListener(null);

                    int currentProgress = (int) (progress / (float) layer.getFileSize() * 100);
                    progressBar.setProgress(currentProgress);

                    layerSize.setVisibility(View.VISIBLE);
                    layerSize.setText(String.format("Downloading: %s of %s",
                            Formatter.formatFileSize(activity.getApplicationContext(), progress),
                            Formatter.formatFileSize(activity.getApplicationContext(), fileSize)));
                } else {
                    progressBar.setVisibility(View.GONE);
                    download.setVisibility(View.VISIBLE);
                }
            }

            if (layer.getType().equals("Feature")) {
                //TODO KML is always downloaded
                progressBar.setVisibility(View.GONE);
                download.setVisibility(View.GONE);
            } else {
                final Layer threadLayer = layer;
                download.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        progressBar.setVisibility(View.VISIBLE);
                        download.setVisibility(View.GONE);
                        downloadManager.downloadGeoPackage(threadLayer);
                    }
                });
            }

            return view;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(activity);
                convertView = inflater.inflate(R.layout.cache_overlay_child, parent, false);
            }

            final CacheOverlay overlay = geopackageOverlays.get(groupPosition - geopackages.size() - staticFeatures.size());
            final CacheOverlay childCache = overlay.getChildren().get(childPosition);

            ImageView imageView =  convertView.findViewById(R.id.cache_overlay_child_image);
            TextView tableName =  convertView.findViewById(R.id.cache_overlay_child_name);
            TextView info =  convertView.findViewById(R.id.cache_overlay_child_info);
            CheckBox checkBox =  convertView.findViewById(R.id.cache_overlay_child_checkbox);

            convertView.findViewById(R.id.divider).setVisibility(isLastChild ? View.VISIBLE : View.INVISIBLE);

            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean checked = ((CheckBox) v).isChecked();

                    childCache.setEnabled(checked);

                    boolean modified = false;
                    if (checked) {
                        if (!overlay.isEnabled()) {
                            overlay.setEnabled(true);
                            modified = true;
                        }
                    } else if (overlay.isEnabled()) {
                        modified = true;
                        for (CacheOverlay childCache : overlay.getChildren()) {
                            if (childCache.isEnabled()) {
                                modified = false;
                                break;
                            }
                        }
                        if (modified) {
                            overlay.setEnabled(false);
                        }
                    }

                    if (modified) {
                        notifyDataSetChanged();
                    }
                }
            });

            tableName.setText(childCache.getName());
            info.setText(childCache.getInfo());
            checkBox.setChecked(childCache.isEnabled());

            Integer imageResource = childCache.getIconImageResourceId();
            if (imageResource != null){
                imageView.setImageResource(imageResource);
            }

            return convertView;
        }

        @Override
        public boolean isChildSelectable(int i, int j) {
            return true;
        }
    }
}