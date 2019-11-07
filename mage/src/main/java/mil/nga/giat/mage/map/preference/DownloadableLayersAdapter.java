package mil.nga.giat.mage.map.preference;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.UiThread;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.map.cache.CacheOverlay;
import mil.nga.giat.mage.map.cache.CacheProvider;
import mil.nga.giat.mage.map.cache.GeoPackageCacheOverlay;
import mil.nga.giat.mage.map.cache.StaticFeatureCacheOverlay;
import mil.nga.giat.mage.map.download.GeoPackageDownloadManager;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.fetch.StaticFeatureServerFetch;
import mil.nga.giat.mage.utils.ByteUtils;

/**
 * Cache Overlay Expandable list adapter
 *
 * <p></p>
 * <b>ALL public methods MUST be made on the UI thread to ensure concurrency.</b>
 */
@UiThread
public class DownloadableLayersAdapter extends BaseExpandableListAdapter {

    private static final String LOG_NAME = DownloadableLayersAdapter.class.getName();

    /**
     * Context
     */
    private final Activity activity;

    /**
     * List of geopackage and static feature cache overlays
     */
    private final List<CacheOverlay> cacheOverlays = new ArrayList<>();

    /**
     * all layers
     */
    private final List<Layer> layers = new ArrayList<>();


    private final GeoPackageDownloadManager downloadManager;


    /**
     * Constructor
     *
     * @param activity
     */
    public DownloadableLayersAdapter(Activity activity, GeoPackageDownloadManager downloadManager) {
        this.activity = activity;
        this.downloadManager = downloadManager;
    }

    public void addOverlay(CacheOverlay overlay, Layer layer) {

        if(overlay instanceof GeoPackageCacheOverlay || overlay instanceof StaticFeatureCacheOverlay) {
            if (layer.isLoaded()) {
                layers.remove(layer);
                cacheOverlays.add(overlay);
            }
        }
    }

    public List<Layer> getLayers() {
        return layers;
    }

    public List<CacheOverlay> getOverlays() {return this.cacheOverlays;}

    void updateDownloadProgress(View view, int progress, long size) {
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
        return cacheOverlays.size() + layers.size();
    }

    @Override
    public int getChildrenCount(int i) {
        if (i < cacheOverlays.size()) {
            int children = cacheOverlays.get(i).getChildren().size();

            for (Layer layer : layers) {
                if(layer.getType().equalsIgnoreCase("geopackage")) {
                    if (layer.isLoaded()) {
                        children++;
                    }
                }
            }

            return children;
        } else {
            return 0;
        }
    }

    @Override
    public Object getGroup(int i) {
        if (i < cacheOverlays.size()) {
            return cacheOverlays.get(i);
        } else {
            return layers.get(i - cacheOverlays.size());
        }
    }

    @Override
    public Object getChild(int i, int j) {
        return cacheOverlays.get(i).getChildren().get(j);
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
        if (i < cacheOverlays.size()) {
            return getOverlayView(i, isExpanded, view, viewGroup);
        } else {
            return getLayerView(i, isExpanded, view, viewGroup);
        }
    }

    private View getOverlayView(int i, boolean isExpanded, View view, ViewGroup viewGroup) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        view = inflater.inflate(R.layout.cache_overlay_group, viewGroup, false);

        final CacheOverlay overlay = cacheOverlays.get(i);

        Event event = EventHelper.getInstance(activity.getApplicationContext()).getCurrentEvent();
        TextView groupView = view.findViewById(R.id.cache_over_group_text);
        groupView.setText(event.getName() +" Layers");

        view.findViewById(R.id.section_header).setVisibility(i == 0 ? View.VISIBLE : View.GONE);

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

        Layer layer = layers.get(i - cacheOverlays.size());


        view.findViewById(R.id.section_header).setVisibility(i == cacheOverlays.size() ? View.VISIBLE : View.GONE);

        TextView cacheName = view.findViewById(R.id.layer_name);
        cacheName.setText(layer.getName());
        TextView description = view.findViewById(R.id.layer_description);

        if (layer.getType().equalsIgnoreCase("geopackage")) {
            description.setText(ByteUtils.getInstance().getDisplay(layer.getFileSize(), true));
        } else {
            description.setText("Static feature data");
        }

        final ProgressBar progressBar = view.findViewById(R.id.layer_progress);
        final View download = view.findViewById(R.id.layer_download);
        if (layer.getType().equalsIgnoreCase("geopackage")) {
            if (downloadManager.isDownloading(layer)) {
                int progress = downloadManager.getProgress(layer);
                long fileSize = layer.getFileSize();
                progressBar.setVisibility(View.VISIBLE);
                download.setVisibility(View.GONE);

                view.setEnabled(false);
                view.setOnClickListener(null);

                int currentProgress = (int) (progress / (float) layer.getFileSize() * 100);
                progressBar.setProgress(currentProgress);

                TextView layerSize = view.findViewById(R.id.layer_size);
                layerSize.setVisibility(View.VISIBLE);
                layerSize.setText(String.format("Downloading: %s of %s",
                        Formatter.formatFileSize(activity.getApplicationContext(), progress),
                        Formatter.formatFileSize(activity.getApplicationContext(), fileSize)));
            } else {
                progressBar.setVisibility(View.GONE);
                download.setVisibility(View.VISIBLE);
            }
        } else if (layer.getType().equalsIgnoreCase("feature")) {
            progressBar.setVisibility(View.GONE);
            if (!layer.isLoaded()) {
                download.setVisibility(View.VISIBLE);
            }
        }

        final Layer threadLayer = layer;
        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                download.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                if (threadLayer.getType().equalsIgnoreCase("geopackage")) {
                    downloadManager.downloadGeoPackage(threadLayer);
                } else if (threadLayer.getType().equalsIgnoreCase("feature")) {
                    progressBar.setIndeterminate(true);
                    @SuppressLint("StaticFieldLeak") AsyncTask<Layer, Void, Layer> fetcher = new AsyncTask<Layer, Void, Layer>() {
                        @Override
                        protected Layer doInBackground(Layer... layers) {
                            StaticFeatureServerFetch staticFeatureServerFetch = new StaticFeatureServerFetch(activity.getApplicationContext());
                            try {
                                staticFeatureServerFetch.load(null, layers[0]);
                            } catch (Exception e) {
                                Log.w(LOG_NAME, "Error fetching static layers", e);
                            }
                            return layers[0];
                        }

                        @Override
                        protected void onPostExecute(Layer layer) {
                            super.onPostExecute(layer);
                            DownloadableLayersAdapter.this.layers.remove(layer);
                            notifyDataSetChanged();
                            CacheProvider.getInstance(activity.getApplicationContext()).refreshTileOverlays();
                        }
                    };
                   fetcher.execute(threadLayer);
                }
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

        final CacheOverlay overlay = cacheOverlays.get(groupPosition);
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
