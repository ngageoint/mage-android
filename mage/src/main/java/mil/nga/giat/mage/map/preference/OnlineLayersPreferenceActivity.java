package mil.nga.giat.mage.map.preference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.Checkable;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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

    public static class OnlineLayersListFragment extends ListFragment  implements CacheProvider.OnCacheOverlayListener {

        /**
         * This class is synchronized by only being accessed on the UI thread
         */
        private OnlineLayersAdapter secureOnlineLayersAdapter;
        private OnlineLayersAdapter insecureOnlineLayersAdapter;

        private MenuItem refreshButton;
        private View contentView;
        private View noContentView;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setHasOptionsMenu(true);
        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_online_layers, container, false);

            contentView = view.findViewById(R.id.online_layers_content);
            noContentView = view.findViewById(R.id.online_layers_no_content);

            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            ListView secureListView = getListView();
            secureListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

            secureOnlineLayersAdapter = new OnlineLayersAdapter(getActivity(), new ArrayList<Layer>());
            secureListView.setAdapter(secureOnlineLayersAdapter);

            ListView insecureListView = view.findViewById(R.id.insecure_layers_list);
            insecureListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

            insecureOnlineLayersAdapter = new OnlineLayersAdapter(getActivity(), new ArrayList<Layer>());
            insecureListView.setAdapter(insecureOnlineLayersAdapter);
        }

        @Override
        public void onResume() {
            super.onResume();
            getListView().setEnabled(false);
        }

        @Override
        public void onPause() {
            super.onPause();
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

        /**
         * This is called when the user click the refresh button
         *
         */
        @UiThread
        private void manualRefresh() {
            refreshButton.setEnabled(false);
            getListView().setEnabled(false);
            noContentView.setVisibility(View.VISIBLE);
            contentView.setVisibility(View.GONE);
            ((TextView) noContentView.findViewById(R.id.online_layers_no_content_title)).setText(getResources().getString(R.string.online_layers_no_content_loading));
            noContentView.findViewById(R.id.online_layers_no_content_summary).setVisibility(View.GONE);
            noContentView.findViewById(R.id.online_layers_no_content_progressBar).setVisibility(View.VISIBLE);

            secureOnlineLayersAdapter.clear();
            insecureOnlineLayersAdapter.clear();

            final Context c = getActivity().getApplicationContext();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    ImageryServerFetch imageryServerFetch = new ImageryServerFetch(c);
                    try {
                        imageryServerFetch.fetch();
                        CacheProvider.getInstance(getContext()).refreshTileOverlays();
                    } catch (Exception e) {
                        Log.w(LOG_NAME, "Failed fetching imagery",e);
                    }
                }
            };

            new Thread(runnable).start();
        }

        @Override
        public void onCacheOverlay(final List<CacheOverlay> cacheOverlays) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Collection<Layer> layers = new ArrayList<>();

                    try {
                        layers = LayerHelper.getInstance(getActivity()).readByEvent(EventHelper.getInstance(getActivity()).getCurrentEvent(), "Imagery");
                    } catch (Exception e) {
                        Log.e(LOG_NAME, "Problem getting layers.", e);
                    }

                    ListView listView = getListView();
                    listView.clearChoices();

                   secureOnlineLayersAdapter.clear();
                   insecureOnlineLayersAdapter.clear();

                   List<Layer> secureLayers = new ArrayList<>();
                   List<Layer> insecureLayers = new ArrayList<>();

                    // Set what should be checked based on preferences.
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    Set<String> overlays = preferences.getStringSet(getResources().getString(R.string.onlineLayersKey), Collections.<String>emptySet());
                    for (Layer layer : layers) {
                        boolean enabled = overlays.contains(layer.getName());

                        CacheOverlay overlay = CacheProvider.getInstance(getContext()).getOverlay(layer.getName());
                        if(overlay != null){
                            overlay.setEnabled(enabled);
                        }

                        if(URLUtil.isHttpsUrl(layer.getUrl())){
                            secureLayers.add(layer);
                        }else{
                            insecureLayers.add(layer);
                        }
                    }

                    secureOnlineLayersAdapter.addAll(secureLayers);
                    insecureOnlineLayersAdapter.addAll(insecureLayers);

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
                    getListView().setEnabled(true);
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
    }

    /**
     *
     * <p></p>
     * <b>ALL public methods MUST be made on the UI thread to ensure concurrency.</b>
     */
    @UiThread
    public static class OnlineLayersAdapter extends ArrayAdapter<Layer> {

        public OnlineLayersAdapter(Context context, List<Layer> overlays) {
            super(context, R.layout.online_layers_list_item, R.id.online_layers_title, overlays);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            final Layer layer = getItem(position);

            TextView title = view.findViewById(R.id.online_layers_title);
            title.setText(layer.getName());

            TextView summary = view.findViewById(R.id.online_layers_summary);
            summary.setText(layer.getUrl());

            View progressBar = view.findViewById(R.id.online_layers_progressBar);
            progressBar.setVisibility(layer.isLoaded() ? View.GONE : View.VISIBLE);

            View sw = view.findViewById(R.id.online_layers_toggle);

            if (URLUtil.isHttpUrl(layer.getUrl())) {
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(getContext())
                                .setTitle("Non HTTPS Layer")
                                .setMessage("We cannot load this layer on mobile because it cannot be accessed securely.")
                                .setPositiveButton("OK", null).show();
                    }
                });
                sw.setEnabled(false);
            } else {
                sw.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean isChecked = ((Checkable) v).isChecked();

                        CacheOverlay overlay = CacheProvider.getInstance(getContext()).getOverlay(layer.getName());
                        if (overlay != null) {
                            overlay.setEnabled(isChecked);
                        }
                    }
                });

                CacheOverlay overlay = CacheProvider.getInstance(getContext()).getOverlay(layer.getName());
                if (overlay != null) {
                    ((Checkable) sw).setChecked(overlay.isEnabled());
                }
            }

            return view;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }
}
