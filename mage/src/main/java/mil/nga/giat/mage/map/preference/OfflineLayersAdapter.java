package mil.nga.giat.mage.map.preference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.MainThread;

import org.apache.commons.lang3.StringUtils;

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
 * Adapter used to control the downloadable layers
 *
 * <p></p>
 * <b>ALL public methods MUST be made on the UI thread to ensure concurrency.</b>
 */
@MainThread
public class OfflineLayersAdapter extends BaseExpandableListAdapter {

    /**
     * log identifier
     */
    private static final String LOG_NAME = OfflineLayersAdapter.class.getName();

    /**
     * Context
     */
    private final Context context;

    /**
     * List of geopackage and static feature cache overlays (downloaded)
     */
    private final List<CacheOverlay> cacheOverlays = new ArrayList<>();

    /**
     * Sideloaded layers.
     */
    private final List<CacheOverlay> sideloadedOverlays = new ArrayList<>();

    /**
     * Layers that can be downloaded
     */
    private final List<Layer> downloadableLayers = new ArrayList<>();


    private final GeoPackageDownloadManager downloadManager;


    /**
     * Constructor
     *
     * @param activity
     */
    public OfflineLayersAdapter(Context activity, GeoPackageDownloadManager downloadManager) {
        this.context = activity;
        this.downloadManager = downloadManager;
    }

    /**
     * Call when a layer is downloaded and turned into a cache overlay
     *
     * @param overlay the downloaded overlay
     * @param layer the layer to remove
     */
    public void addOverlay(CacheOverlay overlay, Layer layer) {

        if(overlay instanceof GeoPackageCacheOverlay || overlay instanceof StaticFeatureCacheOverlay) {
            if (layer.isLoaded()) {
                downloadableLayers.remove(layer);
                cacheOverlays.add(overlay);
            }
        }
    }

    /**
     * This is the live list of downloadable layers and any actions on
     * this list should be synchronized
     *
     * @return
     */
    public List<Layer> getDownloadableLayers() {
        return downloadableLayers;
    }

    /**
     * This is the live list of overlays and any actions on
     * this list should be synchronized
     *
     * @return
     */
    public List<CacheOverlay> getOverlays() {return this.cacheOverlays;}

    public List<CacheOverlay> getSideloadedOverlays() {return this.sideloadedOverlays; }

    public void updateDownloadProgress(View view, Layer layer) {
        int progress = downloadManager.getProgress(layer);
        long size = layer.getFileSize();

        final ProgressBar progressBar = view.findViewById(R.id.layer_progress);
        final View download = view.findViewById(R.id.layer_download);

        if (progress <= 0) {
            String reason = downloadManager.isFailed(layer);
            if(!StringUtils.isEmpty(reason)) {
                Toast.makeText(context, reason, Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
                download.setVisibility(View.VISIBLE);
            }
            return;
        }

        int currentProgress = (int) (progress / (float) size * 100);
        progressBar.setIndeterminate(false);
        progressBar.setProgress(currentProgress);

        TextView layerSize = view.findViewById(R.id.layer_size);
        layerSize.setText(String.format("Downloading: %s of %s",
                Formatter.formatFileSize(context, progress),
                Formatter.formatFileSize(context, size)));
    }

    @Override
    public int getGroupCount() {
        return cacheOverlays.size() + sideloadedOverlays.size() + downloadableLayers.size();
    }

    @Override
    public int getChildrenCount(int i) {
        int children = 0;

        if(i < cacheOverlays.size() ){
            children = cacheOverlays.get(i).getChildren().size();
        } else if( i - cacheOverlays.size() < sideloadedOverlays.size()){
            children = sideloadedOverlays.get(i - cacheOverlays.size()).getChildren().size();
        }

        for (Layer layer : downloadableLayers) {
            if(layer.getType().equalsIgnoreCase("geopackage")) {
                if (layer.isLoaded()) {
                    children++;
                }
            }
        }

        return children;
    }

    @Override
    public Object getGroup(int i) {
        Object group = null;
        if (i < cacheOverlays.size()) {
            group = cacheOverlays.get(i);
        } else if( i - cacheOverlays.size() < cacheOverlays.size()) {
            group = sideloadedOverlays.get(i - cacheOverlays.size());
        } else {
            group = downloadableLayers.get(i - cacheOverlays.size() - sideloadedOverlays.size());
        }
        return group;
    }

    @Override
    public Object getChild(int i, int j) {
        Object child = null;
        if (i < cacheOverlays.size()) {
            child = cacheOverlays.get(i).getChildren().get(j);
        } else if (i - cacheOverlays.size() < sideloadedOverlays.size()) {
            child = sideloadedOverlays.get(i - cacheOverlays.size()).getChildren().get(j);
        }

        return child;
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
        } else if(i - cacheOverlays.size() < sideloadedOverlays.size()) {
            return getOverlaySideloadedView(i, isExpanded, view, viewGroup);
        }else {
            return getDownloadableLayerView(i, isExpanded, view, viewGroup);
        }
    }

    private View getOverlayView(int i, boolean isExpanded, View view, ViewGroup viewGroup) {
        LayoutInflater inflater = LayoutInflater.from(context);
        view = inflater.inflate(R.layout.offline_layer_group, viewGroup, false);
        final CacheOverlay overlay = cacheOverlays.get(i);

        TextView groupView = view.findViewById(R.id.cache_over_group_text);
        Event event = EventHelper.getInstance(context).getCurrentEvent();
        groupView.setText(event.getName() + " Layers");

        view.findViewById(R.id.section_header).setVisibility(i == 0 ? View.VISIBLE : View.GONE);

        ImageView imageView = view.findViewById(R.id.cache_overlay_group_image);
        TextView cacheName = view.findViewById(R.id.cache_overlay_group_name);
        TextView childCount = view.findViewById(R.id.cache_overlay_group_count);
        View checkable = view.findViewById(R.id.cache_overlay_group_checkbox);

        checkable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean checked = ((Checkable) v).isChecked();

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
            if (filePath.startsWith(String.format("%s/MAGE/geopackages", context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)))) {
                try {
                    String relativePath = filePath.split(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/")[1];
                    layer = LayerHelper.getInstance(context.getApplicationContext()).getByRelativePath(relativePath);
                } catch (Exception e) {
                    Log.e(LOG_NAME, "Error getting layer by relative path", e);
                }
            }
        }
        cacheName.setText(layer != null ? layer.getName() : overlay.getName());

        if (overlay.isSupportsChildren()) {
            childCount.setText("(" + getChildrenCount(i) + " layers)");
        } else {
            childCount.setText("");
        }
        ((Checkable) checkable).setChecked(overlay.isEnabled());

        return view;
    }

    private View getOverlaySideloadedView(int i, boolean isExpanded, View view, ViewGroup viewGroup) {
        LayoutInflater inflater = LayoutInflater.from(context);
        view = inflater.inflate(R.layout.offline_layer_sideloaded, viewGroup, false);
        final CacheOverlay overlay = sideloadedOverlays.get(i - cacheOverlays.size());

        TextView groupView = view.findViewById(R.id.cache_overlay_side_group_text);

        groupView.setText("My Layers");

        view.findViewById(R.id.section_header).setVisibility(i - cacheOverlays.size() == 0 ? View.VISIBLE : View.GONE);

        ImageView imageView = view.findViewById(R.id.cache_overlay_side_group_image);
        TextView cacheName = view.findViewById(R.id.cache_overlay_side_group_name);
        TextView childCount = view.findViewById(R.id.cache_overlay_side_group_count);
        View checkable = view.findViewById(R.id.cache_overlay_side_group_checkbox);

        checkable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean checked = ((Checkable) v).isChecked();

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
            if (filePath.startsWith(String.format("%s/MAGE/geopackages", context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)))) {
                try {
                    String relativePath = filePath.split(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/")[1];
                    layer = LayerHelper.getInstance(context.getApplicationContext()).getByRelativePath(relativePath);
                } catch (Exception e) {
                    Log.e(LOG_NAME, "Error getting layer by relative path", e);
                }
            }
        }
        cacheName.setText(layer != null ? layer.getName() : overlay.getName());

        if (overlay.isSupportsChildren()) {
            childCount.setText("(" + getChildrenCount(i) + " layers)");
        } else {
            childCount.setText("");
        }
        ((Checkable) checkable).setChecked(overlay.isEnabled());

        return view;
    }

    private View getDownloadableLayerView(int i, boolean isExpanded, View view, ViewGroup viewGroup) {
        LayoutInflater inflater = LayoutInflater.from(context);
        view = inflater.inflate(R.layout.layer_overlay, viewGroup, false);

        Layer layer = downloadableLayers.get(i - cacheOverlays.size() - sideloadedOverlays.size());

        view.findViewById(R.id.section_header).setVisibility(i - cacheOverlays.size() - sideloadedOverlays.size() == 0 ? View.VISIBLE : View.GONE);

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
                progressBar.setIndeterminate(false);
                progressBar.setProgress(currentProgress);

                TextView layerSize = view.findViewById(R.id.layer_size);
                layerSize.setVisibility(View.VISIBLE);
                layerSize.setText(String.format("Downloading: %s of %s",
                        Formatter.formatFileSize(context, progress),
                        Formatter.formatFileSize(context, fileSize)));
            } else {
                String reason = downloadManager.isFailed(layer);
                if(!StringUtils.isEmpty(reason)) {
                    Toast.makeText(context, reason, Toast.LENGTH_LONG).show();
                }
                progressBar.setVisibility(View.GONE);
                download.setVisibility(View.VISIBLE);
            }
        } else if (layer.getType().equalsIgnoreCase("feature")) {
            if (!layer.isLoaded() && layer.getDownloadId() == null) {
                download.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }else {
                download.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(true);
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
                    @SuppressLint("StaticFieldLeak") AsyncTask<Layer, Void, Layer> fetcher =
                            new AsyncTask<Layer, Void, Layer>() {
                        @Override
                        protected Layer doInBackground(Layer... layers) {
                            StaticFeatureServerFetch staticFeatureServerFetch =
                                    new StaticFeatureServerFetch(context);
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
                            OfflineLayersAdapter.this.getDownloadableLayers().remove(layer);
                            OfflineLayersAdapter.this.getOverlays().clear();
                            OfflineLayersAdapter.this.getSideloadedOverlays().clear();
                            notifyDataSetChanged();
                            CacheProvider.getInstance(context).refreshTileOverlays();
                        }
                    };
                   fetcher.execute(threadLayer);
                }
            }
        });

        view.setTag(layer.getName());

        return view;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.offline_layer_child, parent, false);
        }

        final CacheOverlay overlay = groupPosition < cacheOverlays.size()
                ? cacheOverlays.get(groupPosition) : sideloadedOverlays.get(groupPosition - cacheOverlays.size());
        final CacheOverlay childCache = overlay.getChildren().get(childPosition);

        ImageView imageView =  convertView.findViewById(R.id.cache_overlay_child_image);
        TextView tableName =  convertView.findViewById(R.id.cache_overlay_child_name);
        TextView info =  convertView.findViewById(R.id.cache_overlay_child_info);
        View checkBox =  convertView.findViewById(R.id.cache_overlay_child_checkbox);

        convertView.findViewById(R.id.divider).setVisibility(isLastChild ? View.VISIBLE : View.INVISIBLE);

        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean checked = ((Checkable) v).isChecked();

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
        ((Checkable)checkBox).setChecked(childCache.isEnabled());

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
