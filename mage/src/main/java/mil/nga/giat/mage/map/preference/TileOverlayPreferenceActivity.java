package mil.nga.giat.mage.map.preference;

import android.Manifest;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.cache.CacheUtils;
import mil.nga.giat.mage.map.cache.CacheOverlay;
import mil.nga.giat.mage.map.cache.CacheOverlayFilter;
import mil.nga.giat.mage.map.cache.CacheProvider;
import mil.nga.giat.mage.map.cache.CacheProvider.OnCacheOverlayListener;
import mil.nga.giat.mage.map.cache.GeoPackageCacheOverlay;
import mil.nga.giat.mage.map.cache.XYZDirectoryCacheOverlay;
import mil.nga.giat.mage.map.marker.GeoPackageDownloadManager;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.exceptions.LayerException;
import mil.nga.giat.mage.sdk.gson.deserializer.LayerDeserializer;
import mil.nga.giat.mage.sdk.http.HttpClientManager;
import mil.nga.giat.mage.sdk.http.resource.LayerResource;
import mil.nga.giat.mage.sdk.utils.StorageUtility;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

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

        private OverlayAdapter overlayAdapter;
        private ExpandableListView listView;
        private View progress;
        private MenuItem refreshButton;
        private GeoPackageDownloadManager downloadManager;
        private Timer downloadRefreshTimer;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setHasOptionsMenu(true);

            downloadManager = new GeoPackageDownloadManager(getContext(), new GeoPackageDownloadManager.GeoPackageDownloadListener() {
                @Override
                public void onGeoPackageDownloaded(Layer layer, CacheOverlay overlay) {
                    overlayAdapter.addOverlay(overlay, layer);
                    overlayAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_cache_overlay, container, false);
            listView = (ExpandableListView) view.findViewById(android.R.id.list);
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
        public void onResume() {
            super.onResume();

            downloadManager.onResume();
        }

        @Override
        public void onPause() {
            super.onPause();

            downloadManager.onPause();

            if (downloadRefreshTimer != null) {
                downloadRefreshTimer.cancel();
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
                    refresh(item);
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }

        private void refresh(MenuItem item) {
            item.setEnabled(false);
            progress.setVisibility(View.VISIBLE);
            listView.setEnabled(false);

            fetchGeopackageLayers(new Callback<Collection<Layer>>() {
                @Override
                public void onResponse(Response<Collection<Layer>> response, Retrofit retrofit) {
                    if (response.isSuccess()) {
                        saveGeopackageLayers(response.body());
                        CacheProvider.getInstance(getActivity()).refreshTileOverlays();
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e(LOG_NAME, "Error fetching event geopackage layers", t);
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
                    .client(HttpClientManager.getInstance(context).httpClient())
                    .build();

            Collection<Layer> layers = new ArrayList<>();
            LayerResource.LayerService service = retrofit.create(LayerResource.LayerService.class);

            service.getLayers(event.getRemoteId(), "GeoPackage").enqueue(callback);
        }

        private void saveGeopackageLayers(Collection<Layer> layers) {
            Context context = getContext();
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
            final Event event = EventHelper.getInstance(getContext()).getCurrentEvent();
            try {
                geopackages = LayerHelper.getInstance(getContext()).readByEvent(event,"GeoPackage");
            } catch (LayerException e) {
            }

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

                        Object group = overlayAdapter.getGroup(groupPosition);
                        if (group instanceof CacheOverlay) {
                            CacheOverlay cacheOverlay = (CacheOverlay) overlayAdapter.getGroup(groupPosition);
                            deleteCacheOverlayConfirm(cacheOverlay);
                            return true;
                        }

                        return false;
                    }

                    return false;
                }
            });

            downloadManager.reconcileDownloads(geopackages, new GeoPackageDownloadManager.GeoPackageLoadListener() {
                @Override
                public void onReady(List<Layer> layers) {
                    overlayAdapter = new OverlayAdapter(getActivity(), event, cacheOverlays, layers, downloadManager);
                    listView.setAdapter(overlayAdapter);

                    refreshButton.setEnabled(true);
                    listView.setEnabled(true);
                    progress.setVisibility(View.GONE);

                    downloadRefreshTimer = new Timer();
                    downloadRefreshTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            refreshDownloads();
                        }
                    }, 0, 2000);
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
        public ArrayList<String> getSelectedOverlays() {
            ArrayList<String> overlays = new ArrayList<>();
            if (overlayAdapter != null) {
                for (CacheOverlay cacheOverlay : overlayAdapter.getOverlays()) {

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

        private void refreshDownloads() {
            List<Layer> layers = overlayAdapter.getGeopackages();
            for (int i = 0; i < layers.size(); i++) {
                final Layer layer = layers.get(i);

                if (layer.getDownloadId() != null && !layer.isLoaded()) {
                    // layer is currently downloading, get progress and refresh view
                    final View view =  listView.getChildAt(i - listView.getFirstVisiblePosition());
                    if (view == null) {
                        continue;
                    }

                    Activity activity = getActivity();
                    if (activity == null) {
                        continue;
                    }

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            overlayAdapter.updateDownloadProgress(view, downloadManager.getProgress(layer), layer.getFileSize());
                        }
                    });
                }
            }
        }

        /**
         * Delete the cache overlay
         * @param cacheOverlay
         */
        private void deleteCacheOverlayConfirm(final CacheOverlay cacheOverlay) {
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
                LayerHelper layerHelper = LayerHelper.getInstance(getContext());

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
                    Log.e(LOG_NAME, "Error deleting geopackge file from filesystem for path " + path);
                }
            }
        }

    }

    /**
     * Cache Overlay Expandable list adapter
     */
    public static class OverlayAdapter extends BaseExpandableListAdapter {

        /**
         * Context
         */
        private Activity activity;

        /**
         * List of cache overlays
         */
        private List<CacheOverlay> overlays;

        /**
         * List of geopackages
         */
        private List<Layer> geopackages = new ArrayList<>();

        private GeoPackageDownloadManager downloadManager;

        /**
         * Constructor
         *
         * @param activity
         * @param overlays
         */
        public OverlayAdapter(Activity activity, Event event, List<CacheOverlay> overlays, List<Layer> geopackages, GeoPackageDownloadManager downloadManager) {
            this.activity = activity;
            this.overlays = new CacheOverlayFilter(activity.getApplicationContext(), event).filter(overlays);
            this.geopackages = geopackages;
            this.downloadManager = downloadManager;
        }

        public void addOverlay(CacheOverlay overlay, Layer layer) {
            if (layer.isLoaded()) {
                geopackages.remove(layer);
                overlays.add(overlay);
            }
        }

        /**
         * Get the overlays
         *
         * @return
         */
        public List<CacheOverlay> getOverlays() {
            return overlays;
        }

        public List<Layer> getGeopackages() {
            return geopackages;
        }

        public void updateDownloadProgress(View view, int progress, long size) {
            if (progress <= 0) {
                return;
            }

            ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.layer_progress);
            if (progressBar == null) {
                return;
            }

            int currentProgress = (int) (progress / (float) size * 100);
            progressBar.setProgress(currentProgress);

            TextView layerSize = (TextView) view.findViewById(R.id.layer_size);
            layerSize.setText(String.format("Downloading: %s of %s",
                Formatter.formatFileSize(activity.getApplicationContext(), progress),
                Formatter.formatFileSize(activity.getApplicationContext(), size)));
        }

        @Override
        public int getGroupCount() {
            return overlays.size() + geopackages.size();
        }

        @Override
        public int getChildrenCount(int i) {
            if (i < geopackages.size()) {
                return 0;
            } else {
                int children = overlays.get(i - geopackages.size()).getChildren().size();

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
            } else {
                return overlays.get(i - geopackages.size());
            }
        }

        @Override
        public Object getChild(int i, int j) {
            return overlays.get(i - geopackages.size()).getChildren().get(j);
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
            } else {
                return getOverlayView(i, isExpanded, view, viewGroup);
            }
        }

        private View getOverlayView(int i, boolean isExpanded, View view, ViewGroup viewGroup) {
            LayoutInflater inflater = LayoutInflater.from(activity);
            view = inflater.inflate(R.layout.cache_overlay_group, viewGroup, false);

            final CacheOverlay overlay = overlays.get(i - geopackages.size());

            view.findViewById(R.id.section_header).setVisibility(i == geopackages.size() ? View.VISIBLE : View.GONE);

            ImageView imageView = (ImageView) view.findViewById(R.id.cache_overlay_group_image);
            TextView cacheName = (TextView) view.findViewById(R.id.cache_overlay_group_name);
            TextView childCount = (TextView) view.findViewById(R.id.cache_overlay_group_count);
            CheckBox checkBox = (CheckBox) view.findViewById(R.id.cache_overlay_group_checkbox);

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
            } else {
                imageView.setImageResource(-1);
            }

            Layer layer = null;
            if (overlay instanceof GeoPackageCacheOverlay) {
                String filePath = ((GeoPackageCacheOverlay) overlay).getFilePath();
                if (filePath.startsWith(String.format("%s/MAGE/geopackages", activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)))) {
                    try {
                        String relativePath = filePath.split(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/")[1];
                        layer = LayerHelper.getInstance(activity).getByRelativePath(relativePath);
                    } catch(Exception e) {
                        Log.e(LOG_NAME, "Error getting layer by relative paht", e);
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

            final Layer layer = geopackages.get(i);

            view.findViewById(R.id.section_header).setVisibility(i == 0 ? View.VISIBLE : View.GONE);

            ImageView imageView = (ImageView) view.findViewById(R.id.layer_image);
            imageView.setImageResource(R.drawable.ic_geopackage);

            TextView cacheName = (TextView) view.findViewById(R.id.layer_name);
            cacheName.setText(layer.getName());

            TextView layerSize = (TextView) view.findViewById(R.id.layer_size);

            final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.layer_progress);
            final View download = view.findViewById(R.id.layer_download);

            if (downloadManager.isDownloading(layer)) {
                int progress =  downloadManager.getProgress(layer);
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

            download.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    progressBar.setVisibility(View.VISIBLE );
                    download.setVisibility(View.GONE);

                    downloadManager.downloadGeoPackage(layer);
                }
            });

            return view;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(activity);
                convertView = inflater.inflate(R.layout.cache_overlay_child, parent, false);
            }

            final CacheOverlay overlay = overlays.get(groupPosition - geopackages.size());
            final CacheOverlay childCache = overlay.getChildren().get(childPosition);

            ImageView imageView = (ImageView) convertView.findViewById(R.id.cache_overlay_child_image);
            TextView tableName = (TextView) convertView.findViewById(R.id.cache_overlay_child_name);
            TextView info = (TextView) convertView.findViewById(R.id.cache_overlay_child_info);
            CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.cache_overlay_child_checkbox);

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
            } else {
                imageView.setImageResource(-1);
            }

            return convertView;
        }

        @Override
        public boolean isChildSelectable(int i, int j) {
            return true;
        }
    }
}