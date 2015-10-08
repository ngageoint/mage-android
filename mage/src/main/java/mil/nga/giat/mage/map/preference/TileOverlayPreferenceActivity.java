package mil.nga.giat.mage.map.preference;

import android.app.Activity;
import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.MAGE.OnCacheOverlayListener;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.map.cache.CacheOverlay;

public class TileOverlayPreferenceActivity extends ExpandableListActivity implements OnCacheOverlayListener {

    private MAGE mage;
    private ProgressBar progressBar;
    private MenuItem refreshButton;
    private OverlayAdapter overlayAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cache_overlay);

        mage = (MAGE) getApplication();
        progressBar = (ProgressBar) findViewById(R.id.overlay_progress_bar);

        getExpandableListView().setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mage.unregisterCacheOverlayListener(this);
    }

    @Override
    public void onCacheOverlay(List<CacheOverlay> cacheOverlays) {

        overlayAdapter = new OverlayAdapter(this, cacheOverlays);

        setListAdapter(overlayAdapter);
        refreshButton.setEnabled(true);
        getExpandableListView().setEnabled(true);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        refreshButton = menu.findItem(R.id.tile_overlay_refresh);
        refreshButton.setEnabled(false);

        // This really should be done in the onResume, but I need to have my refreshButton
        // before I register as the call back will set it to enabled
        // the problem is that onResume gets called before this so my menu is
        // not yet setup and I will not have a handle on this button
        mage.registerCacheOverlayListener(this);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tile_overlay_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.tile_overlay_refresh:
                item.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);
                getExpandableListView().setEnabled(false);

                ((MAGE) getApplication()).refreshTileOverlays();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putStringArrayListExtra(MapPreferencesActivity.OVERLAY_EXTENDED_DATA_KEY, getSelectedOverlays());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    /**
     * Get the selected cache overlays and child cache overlays
     *
     * @return
     */
    private ArrayList<String> getSelectedOverlays() {
        ArrayList<String> overlays = new ArrayList<String>();
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

    /**
     * Cache Overlay Expandable list adapter
     */
    public static class OverlayAdapter extends BaseExpandableListAdapter {

        /**
         * Context
         */
        private Context context;

        /**
         * List of cache overlays
         */
        private List<CacheOverlay> overlays;

        /**
         * Constructor
         *
         * @param context
         * @param overlays
         */
        public OverlayAdapter(Context context, List<CacheOverlay> overlays) {
            this.context = context;
            this.overlays = overlays;
        }

        /**
         * Get the overlays
         *
         * @return
         */
        public List<CacheOverlay> getOverlays() {
            return overlays;
        }

        @Override
        public int getGroupCount() {
            return overlays.size();
        }

        @Override
        public int getChildrenCount(int i) {
            return overlays.get(i).getChildren().size();
        }

        @Override
        public Object getGroup(int i) {
            return overlays.get(i);
        }

        @Override
        public Object getChild(int i, int j) {
            return overlays.get(i).getChildren().get(j);
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
        public View getGroupView(int i, boolean isExpanded, View view,
                                 ViewGroup viewGroup) {
            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                view = inflater.inflate(R.layout.cache_overlay_group, viewGroup, false);
            }

            ImageView imageView = (ImageView) view
                    .findViewById(R.id.cache_overlay_group_image);
            TextView geoPackageName = (TextView) view
                    .findViewById(R.id.cache_overlay_group_name);
            TextView childCount = (TextView) view
                    .findViewById(R.id.cache_overlay_group_count);
            CheckBox checkBox = (CheckBox) view
                    .findViewById(R.id.cache_overlay_group_checkbox);

            final CacheOverlay overlay = overlays.get(i);

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
            if(imageResource != null){
                imageView.setImageResource(imageResource);
            }else{
                imageView.setImageResource(-1);
            }
            geoPackageName.setText(overlay.getName());
            if (overlay.isSupportsChildren()) {
                childCount.setText("(" + getChildrenCount(i) + ")");
            }else{
                childCount.setText("");
            }
            checkBox.setChecked(overlay.isEnabled());

            return view;
        }

        @Override
        public View getChildView(int i, int j, boolean b, View view,
                                 ViewGroup viewGroup) {
            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                view = inflater.inflate(R.layout.cache_overlay_child, viewGroup, false);
            }

            final CacheOverlay overlay = overlays.get(i);
            final CacheOverlay childCache = overlay.getChildren().get(j);

            ImageView imageView = (ImageView) view
                    .findViewById(R.id.cache_overlay_child_image);
            TextView tableName = (TextView) view
                    .findViewById(R.id.cache_overlay_child_name);
            TextView info = (TextView) view
                    .findViewById(R.id.cache_overlay_child_info);
            CheckBox checkBox = (CheckBox) view
                    .findViewById(R.id.cache_overlay_child_checkbox);

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
            if(imageResource != null){
                imageView.setImageResource(imageResource);
            }else{
                imageView.setImageResource(-1);
            }

            return view;
        }

        @Override
        public boolean isChildSelectable(int i, int j) {
            return true;
        }

    }

}