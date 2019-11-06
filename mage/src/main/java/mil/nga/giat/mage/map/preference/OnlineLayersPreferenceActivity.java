package mil.nga.giat.mage.map.preference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.Checkable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import dagger.android.support.DaggerFragment;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.map.cache.CacheOverlay;
import mil.nga.giat.mage.map.cache.CacheProvider;
import mil.nga.giat.mage.map.cache.URLCacheOverlay;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.fetch.ImageryServerFetch;


/**
 * This activity is the view component for online layers
 *
 */
public class OnlineLayersPreferenceActivity extends AppCompatActivity {

    /**
     * logger
     */
    private static final String LOG_NAME = OnlineLayersPreferenceActivity.class.getName();

    /**
     * Fragment showing the actual online layers URLs
     */
    private OnlineLayersListFragment onlineLayersFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_layers);

        onlineLayersFragment = (OnlineLayersListFragment) getSupportFragmentManager().findFragmentById(R.id.online_layers_fragment);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putStringArrayListExtra(MapPreferencesActivity.ONLINE_LAYERS_DATA_KEY, onlineLayersFragment.getSelectedOverlays());
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

    public static class OnlineLayersListFragment extends DaggerFragment implements CacheProvider.OnCacheOverlayListener {

        /**
         * This class is synchronized by only being accessed on the UI thread
         */
        private OnlineLayersAdapter adapter;

        private MenuItem refreshButton;
        private View contentView;
        private View noContentView;
        private RecyclerView recyclerView;
        private SwipeRefreshLayout swipeContainer;
        private Parcelable listState;


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_online_layers, container, false);

            contentView = view.findViewById(R.id.online_layers_content);
            noContentView = view.findViewById(R.id.online_layers_no_content);

            setHasOptionsMenu(true);

            swipeContainer = view.findViewById(R.id.swipeContainer);
            swipeContainer.setColorSchemeResources(R.color.md_blue_600, R.color.md_orange_A200);
            swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    manualRefresh();
                }
            });

            recyclerView = view.findViewById(R.id.recycler_view);

            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
            recyclerView.setLayoutManager(mLayoutManager);
            recyclerView.setItemAnimator(new DefaultItemAnimator());

            adapter = new OnlineLayersAdapter(getActivity());

            return view;
        }


        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.online_layers_menu, menu);

            refreshButton = menu.findItem(R.id.online_layers_refresh);
            refreshButton.setEnabled(false);

            CacheProvider.getInstance(getActivity()).registerCacheOverlayListener(this);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.online_layers_refresh:
                    manualRefresh();
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }

        /**vcvvvv
         * This is called when the user click the refresh button
         *
         */
        @UiThread
        private void manualRefresh() {
            refreshButton.setEnabled(false);
            noContentView.setVisibility(View.VISIBLE);
            contentView.setVisibility(View.GONE);
            ((TextView) noContentView.findViewById(R.id.online_layers_no_content_title)).setText(getResources().getString(R.string.online_layers_no_content_loading));
            noContentView.findViewById(R.id.online_layers_no_content_summary).setVisibility(View.GONE);
            noContentView.findViewById(R.id.online_layers_no_content_progressBar).setVisibility(View.VISIBLE);

            adapter.clear();
            adapter.notifyDataSetChanged();

            if (getActivity() != null) {
                final Context c = getActivity().getApplicationContext();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        ImageryServerFetch imageryServerFetch = new ImageryServerFetch(c);
                        try {
                            imageryServerFetch.fetch();
                            CacheProvider.getInstance(getContext()).refreshTileOverlays();
                        } catch (Exception e) {
                            Log.w(LOG_NAME, "Failed fetching imagery", e);
                        }
                    }
                };

                new Thread(runnable).start();
            } else {
                Log.e(LOG_NAME, "Activity is null");
            }
        }

        @Override
        public void onCacheOverlay(final List<CacheOverlay> cacheOverlays) {
            if(getActivity() == null) {
                Log.w(LOG_NAME, "Failed to handle new cache overly since activity was null");
                return;
            }

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Collection<Layer> layers = new ArrayList<>();

                    try {
                        layers = LayerHelper.getInstance(getActivity()).readByEvent(EventHelper.getInstance(getActivity()).getCurrentEvent(), "Imagery");
                    } catch (Exception e) {
                        Log.e(LOG_NAME, "Problem getting layers.", e);
                    }


                    adapter.clear();
                    adapter.notifyDataSetChanged();

                    List<Layer> secureLayers = new ArrayList<>();
                    List<Layer> insecureLayers = new ArrayList<>();

                    // Set what should be checked based on preferences.
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    Set<String> overlays = preferences.getStringSet(getResources().getString(R.string.onlineLayersKey), Collections.<String>emptySet());
                    for (Layer layer : layers) {
                        boolean enabled = overlays != null ? overlays.contains(layer.getName()) : false;

                        CacheOverlay overlay = CacheProvider.getInstance(getContext()).getOverlay(layer.getName());
                        if (overlay != null) {
                            overlay.setEnabled(enabled);
                        }

                        if (URLUtil.isHttpsUrl(layer.getUrl())) {
                            secureLayers.add(layer);
                        } else {
                            insecureLayers.add(layer);
                        }
                    }

                    Comparator<Layer> compare = new Comparator<Layer>() {
                        @Override
                        public int compare(Layer o1, Layer o2) {
                            return o1.getName().compareTo(o2.getName());

                        }
                    };

                    Collections.sort(secureLayers, compare);
                    Collections.sort(insecureLayers, compare);

                    adapter.addAllNonLayers(insecureLayers);
                    adapter.addAllSecureLayers(secureLayers);
                    adapter.notifyDataSetChanged();

                    if (!layers.isEmpty()) {
                        noContentView.setVisibility(View.GONE);
                        contentView.setVisibility(View.VISIBLE);
                    } else {
                        noContentView.setVisibility(View.VISIBLE);
                        contentView.setVisibility(View.GONE);
                        ((TextView) noContentView.findViewById(R.id.online_layers_no_content_title)).setText(getResources().getString(R.string.online_layers_no_content_text));
                        noContentView.findViewById(R.id.online_layers_no_content_summary).setVisibility(View.VISIBLE);
                        noContentView.findViewById(R.id.online_layers_no_content_progressBar).setVisibility(View.GONE);
                    }

                    refreshButton.setEnabled(true);
                    swipeContainer.setRefreshing(false);
                }
            });
        }

        public ArrayList<String> getSelectedOverlays() {
            ArrayList<String> overlays = new ArrayList<>();
            for (CacheOverlay overlay : CacheProvider.getInstance(getContext()).getCacheOverlays()) {
                if (overlay instanceof URLCacheOverlay) {
                    if (overlay.isEnabled()) {
                        overlays.add(overlay.getName());
                    }
                }
            }

            return overlays;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            CacheProvider.getInstance(getActivity()).unregisterCacheOverlayListener(this);
        }

        @Override
        public void onResume() {
            super.onResume();
            recyclerView.setAdapter(adapter);

            if (listState != null) {
                recyclerView.getLayoutManager().onRestoreInstanceState(listState);
            }
        }

        @Override
        public void onPause() {
            super.onPause();

            listState = recyclerView.getLayoutManager().onSaveInstanceState();
            recyclerView.setAdapter(null);
        }
    }

    /**
     *
     * <p></p>
     * <b>ALL public methods MUST be made on the UI thread to ensure concurrency.</b>
     */
    @UiThread
    public static class OnlineLayersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        private static final int ITEM_TYPE_HEADER = 1;
        private static final int ITEM_TYPE_LAYER = 2;

        private final Context context;
        private final List<Layer> secureLayers = new ArrayList<>();
        private final List<Layer> nonSecureLayers = new ArrayList<>();

        OnlineLayersAdapter(Context context) {
            this.context = context;
        }

        public void clear(){
            this.secureLayers.clear();
            this.nonSecureLayers.clear();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
            if (i == ITEM_TYPE_LAYER) {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.online_layers_list_item, parent, false);
                return new LayerViewHolder(itemView);
            } else if (i == ITEM_TYPE_HEADER) {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_list_section_header, parent, false);
                return new SectionViewHolder(itemView);
            } else {
                return null;
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int i) {
            if (holder instanceof LayerViewHolder) {
                bindLayerViewHolder((LayerViewHolder) holder, i);
            } else if (holder instanceof SectionViewHolder) {
                bindSectionViewHolder((SectionViewHolder) holder, i);
            }
        }

        private void bindLayerViewHolder(LayerViewHolder holder, int i) {
            View view = holder.itemView;
            Layer tmp = null;

            if (i <= secureLayers.size()) {
                tmp = secureLayers.get(i - 1);
            } else {
                tmp = nonSecureLayers.get(i - secureLayers.size() - 2);
            }

            final Layer layer = tmp;

            TextView title = view.findViewById(R.id.online_layers_title);
            title.setText(layer != null ? layer.getName() : "");

            TextView summary = view.findViewById(R.id.online_layers_summary);
            summary.setText(layer.getUrl());

            View progressBar = view.findViewById(R.id.online_layers_progressBar);
            progressBar.setVisibility(layer.isLoaded() ? View.GONE : View.VISIBLE);

            final View sw = view.findViewById(R.id.online_layers_toggle);

            if (URLUtil.isHttpUrl(layer.getUrl())) {
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(context)
                                .setTitle("Non HTTPS Layer")
                                .setMessage("We cannot load this layer on mobile because it cannot be accessed securely.")
                                .setPositiveButton("OK", null).show();
                    }
                });
                sw.setOnClickListener(null);
                sw.setEnabled(false);
                ((Checkable) sw).setChecked(false);
            } else {
                //TODO allow for selection by row??
                /*view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean checked = ((Checkable) sw).isChecked();
                        ((Checkable) sw).setChecked(!checked);
                    }
                });*/
                view.setOnClickListener(null);
                sw.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean isChecked = ((Checkable) v).isChecked();

                        //TODO persist this infor to the DB
                        CacheOverlay overlay = CacheProvider.getInstance(context).getOverlay(layer.getName());
                        if (overlay != null) {
                            overlay.setEnabled(isChecked);
                        }
                    }
                });
                sw.setEnabled(true);
                CacheOverlay overlay = CacheProvider.getInstance(context).getOverlay(layer.getName());
                if (overlay != null) {
                    ((Checkable) sw).setChecked(overlay.isEnabled());
                }
            }
        }

        private void bindSectionViewHolder(SectionViewHolder holder, int position) {
            holder.sectionName.setText(position == 0 ? "Secure Layers" : "Nonsecure Layers");
        }

        public void addAllSecureLayers(List<Layer> layers){
            this.secureLayers.addAll(layers);
        }

        public void addAllNonLayers(List<Layer> layers){
            this.nonSecureLayers.addAll(layers);
        }

        @Override
        public int getItemCount() {
            return secureLayers.size() + nonSecureLayers.size() + 2;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0 || position == secureLayers.size() + 1) {
                return ITEM_TYPE_HEADER;
            } else {
                return ITEM_TYPE_LAYER;
            }
        }

        private class LayerViewHolder extends RecyclerView.ViewHolder {
            LayerViewHolder(View view) {
                super(view);
            }
        }

        private class SectionViewHolder extends RecyclerView.ViewHolder {
            private TextView sectionName;

            private SectionViewHolder(View view) {
                super(view);

                sectionName = (TextView) view.findViewById(R.id.section_name);
            }
        }
    }
}
