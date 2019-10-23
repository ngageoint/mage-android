package mil.nga.giat.mage.map.preference;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.ListFragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import mil.nga.giat.mage.MageApplication;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeatureHelper;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.event.ILayerEventListener;
import mil.nga.giat.mage.sdk.event.IStaticFeatureEventListener;
import mil.nga.giat.mage.sdk.fetch.StaticFeatureServerFetch;

public class FeatureOverlayPreferenceActivity extends AppCompatActivity {

	private static final String LOG_NAME = FeatureOverlayPreferenceActivity.class.getName();

    private FeatureListFragment featureFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feature_overlay);

        featureFragment = (FeatureListFragment) getSupportFragmentManager().findFragmentById(R.id.feature_fragment);
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

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putStringArrayListExtra(MapPreferencesActivity.OVERLAY_EXTENDED_DATA_KEY, featureFragment.getSelectedOverlays());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    public static class FeatureListFragment extends ListFragment implements ILayerEventListener, IStaticFeatureEventListener {

        @Inject
        MageApplication application;

        private OverlayAdapter overlayAdapter;
        private MenuItem refreshButton;
        private View contentView;
        private View noContentView;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            AndroidSupportInjection.inject(this);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_feature_overlay, container, false);

            contentView = view.findViewById(R.id.content);
            noContentView = view.findViewById(R.id.no_content);

            setHasOptionsMenu(true);

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
            getListView().setEnabled(false);
        }

        @Override
        public void onPause() {
            super.onPause();
            LayerHelper.getInstance(getActivity()).removeListener(this);
            StaticFeatureHelper.getInstance(getActivity()).removeListener(this);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.feature_overlay_menu, menu);

            refreshButton = menu.findItem(R.id.feature_overlay_refresh);
            refreshButton.setEnabled(false);

            // This really should be done in the onResume, but I need to have
            // the refreshButton
            // before I register as the call back will set it to enabled
            // the problem is that onResume gets called before this so my menu is
            // not yet setup and I will not have a handle on this button

            onLayerCreated(null);

            LayerHelper.getInstance(getActivity()).addListener(this);
            StaticFeatureHelper.getInstance(getActivity()).addListener(this);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Handle presses on the action bar items
            switch (item.getItemId()) {
                case R.id.feature_overlay_refresh:
                    refreshOverlays();
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }


        @Override
        public void onLayerCreated(Layer layer) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Collection<Layer> layers = new ArrayList<>();

                    try {
                        layers = LayerHelper.getInstance(getActivity()).readByEvent(EventHelper.getInstance(getActivity()).getCurrentEvent(), "Feature");
                    } catch(Exception e) {
                        Log.e(LOG_NAME, "Problem getting layers.", e);
                    }
                    ListView listView = getListView();
                    listView.clearChoices();

                    overlayAdapter = new OverlayAdapter(getActivity(), new ArrayList<>(layers));
                    setListAdapter(overlayAdapter);

                    // Set what should be checked based on preferences.
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    Set<String> overlays = preferences.getStringSet(getResources().getString(R.string.staticFeatureLayersKey), Collections.<String> emptySet());
                    for (int i = 0; i < listView.getCount(); i++) {
                        Layer layer = (Layer) listView.getItemAtPosition(i);
                        if (overlays.contains(layer.getId().toString())) {
                            listView.setItemChecked(i, true);
                        }
                    }

                    if (!layers.isEmpty()) {
                        noContentView.setVisibility(View.GONE);
                        contentView.setVisibility(View.VISIBLE);
                    } else {
                        noContentView.setVisibility(View.VISIBLE);
                        contentView.setVisibility(View.GONE);
                        ((TextView) noContentView.findViewById(R.id.title)).setText(getResources().getString(R.string.feature_overlay_no_content_text));
                        noContentView.findViewById(R.id.summary).setVisibility(View.VISIBLE);
                        noContentView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                    }

                    refreshButton.setEnabled(true);
                    getListView().setEnabled(true);
                }
            });
        }

        @Override
        public void onLayerUpdated(Layer layer) {

        }

        @Override
        public void onStaticFeaturesCreated(final Layer layer) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int i = overlayAdapter.getPosition(layer);
                    Layer l = overlayAdapter.getItem(i);

                    if (l != null) {
                        l.setLoaded(layer.isLoaded());
                        overlayAdapter.notifyDataSetChanged();
                    } else {
                        Log.d(LOG_NAME, "Static layer " + layer.getName() + ":" + layer.getId() + " is not available, adapter size is: " + overlayAdapter.getCount());
                    }
                }
            });
        }

        @Override
        public void onError(Throwable error) {

        }

        public ArrayList<String> getSelectedOverlays() {
            ArrayList<String> overlays = new ArrayList<>();
            SparseBooleanArray checked = getListView().getCheckedItemPositions();
            for (int i = 0; i < checked.size(); i++) {
                if (checked.valueAt(i)) {
                    Layer layer = (Layer) getListView().getItemAtPosition(checked.keyAt(i));
                    overlays.add(layer.getId().toString());
                }
            }

            return overlays;
        }

        private void refreshOverlays() {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Refresh Feature Overlays")
                    .setMessage(R.string.feature_overlay_refresh)
                    .setPositiveButton("Refresh", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            refreshButton.setEnabled(false);
                            getListView().setEnabled(false);
                            noContentView.setVisibility(View.VISIBLE);
                            contentView.setVisibility(View.GONE);
                            ((TextView) noContentView.findViewById(R.id.title)).setText(getResources().getString(R.string.feature_overlay_no_content_loading));
                            noContentView.findViewById(R.id.summary).setVisibility(View.GONE);
                            noContentView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);

                            overlayAdapter.clear();
                            overlayAdapter.notifyDataSetChanged();
                            application.loadStaticFeatures(true, new StaticFeatureServerFetch.OnStaticLayersListener() {
                                @Override
                                public void onStaticLayersLoaded(Collection<Layer> layers) {
                                    onLayerCreated(null);
                                }
                            });
                        }
                    }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                }
            }).show();
        }
    }

    public static class OverlayAdapter extends ArrayAdapter<Layer> {
        private List<Layer> layers;

        public OverlayAdapter(Context context, List<Layer> overlays) {
            super(context, R.layout.feature_layer_list_item, R.id.title, overlays);

            this.layers = overlays;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            Layer layer = getItem(position);
            String name = layer.getName();
            TextView title = (TextView) view.findViewById(R.id.title);
            title.setText(name);

            TextView summary = (TextView) view.findViewById(R.id.summary);
            summary.setText(layer.getStaticFeatures().size() + " features");

            View progressBar = view.findViewById(R.id.progressBar);
            progressBar.setVisibility(layer.isLoaded() ? View.GONE : View.VISIBLE);

            return view;
        }

        @Override
        public int getPosition(Layer layer) {
            for (int i = 0; i < layers.size(); i++) {
                if (layer.equals(layers.get(i))) {
                    return i;
                }
            }

            return -1;
        }

        @Override
        public Layer getItem(int index) {
            Layer layer = null;
            
            try {
                layer = layers.get(index);
            } catch (ArrayIndexOutOfBoundsException e) {
				Log.e(LOG_NAME, "Why out of bounds?", e);
			}
            
            return layer;
        }

        @Override
        public long getItemId(int index) {
            return index;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}