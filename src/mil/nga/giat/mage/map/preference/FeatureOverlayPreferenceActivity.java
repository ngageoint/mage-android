package mil.nga.giat.mage.map.preference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeatureHelper;
import mil.nga.giat.mage.sdk.event.ILayerEventListener;
import mil.nga.giat.mage.sdk.event.IStaticFeatureEventListener;
import mil.nga.giat.mage.sdk.exceptions.LayerException;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;

public class FeatureOverlayPreferenceActivity extends ListActivity implements ILayerEventListener, IStaticFeatureEventListener {

    private OverlayAdapter overlayAdapter;
    private MenuItem refreshButton;
    private View contentView;
    private View noContentView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_feature_overlay);

        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        contentView = findViewById(R.id.content);
        noContentView = findViewById(R.id.no_content);
    }

    @Override
    public void onResume() {
        super.onResume();
        getListView().setEnabled(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        LayerHelper.getInstance(this).removeListener(this);
    }

    @Override
    public void onLayersCreated(Collection<Layer> layers) {
        ListView listView = getListView();
        listView.clearChoices();

        overlayAdapter = new OverlayAdapter(this, new ArrayList<Layer>(layers));
        setListAdapter(overlayAdapter);

        // Set what should be checked based on preferences.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> overlays = preferences.getStringSet(getResources().getString(R.string.mapFeatureOverlaysKey), Collections.<String> emptySet());
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
            ((TextView) noContentView.findViewById(R.id.title)).setText(getResources().getString(R.string.feature_overlay_no_content_text));
            noContentView.findViewById(R.id.text).setVisibility(View.VISIBLE);
            noContentView.findViewById(R.id.progressBar).setVisibility(View.GONE);
        }

        refreshButton.setEnabled(true);
        getListView().setEnabled(true);
    }

    @Override
    public void onStaticFeaturesCreated(Collection<Layer> layers) {
        for (Layer l : layers) {
            int i = overlayAdapter.getPosition(l);
            overlayAdapter.getItem(i).setLoaded(true);
        }
        
        overlayAdapter.notifyDataSetChanged();
    }

    @Override
    public void onError(Throwable error) {
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        refreshButton = menu.findItem(R.id.feature_overlay_refresh);
        refreshButton.setEnabled(false);

        // This really should be done in the onResume, but I need to have
        // the refreshButton
        // before I register as the call back will set it to enabled
        // the problem is that onResume gets called before this so my menu is
        // not yet setup and I will not have a handle on this button

        boolean loaded = StaticFeatureHelper.getInstance(this).haveLayersBeenFetchedOnce();
        if (loaded) {
            try {
                Collection<Layer> layers = LayerHelper.getInstance(this).readAllStaticLayers();
                onLayersCreated(layers);
            } catch (LayerException e) {
                e.printStackTrace();
            }
        }

        LayerHelper.getInstance(this).addListener(this);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.feature_overlay_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
        case R.id.feature_overlay_refresh:
            // item.setEnabled(false);
            // progressBar.setVisibility(View.VISIBLE);
            // getListView().setEnabled(false);
            //
            // ((MAGE) getApplication()).refreshTileOverlays();
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

    private ArrayList<String> getSelectedOverlays() {
        ArrayList<String> overlays = new ArrayList<String>();
        SparseBooleanArray checked = getListView().getCheckedItemPositions();
        for (int i = 0; i < checked.size(); i++) {
            if (checked.valueAt(i)) {
                Layer layer = (Layer) getListView().getItemAtPosition(checked.keyAt(i));
                overlays.add(layer.getId().toString());
            }
        }

        return overlays;
    }

    public static class OverlayAdapter extends ArrayAdapter<Layer> {
        private List<Layer> layers;

        public OverlayAdapter(Context context, List<Layer> overlays) {
            super(context, R.layout.feature_layer_list_item, R.id.checkedTextView, overlays);

            this.layers = overlays;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            Layer layer = getItem(position);
            String name = layer.getName();
            CheckedTextView checkedView = (CheckedTextView) view.findViewById(R.id.checkedTextView);
            checkedView.setText(name);
            
            View progressBar = view.findViewById(R.id.progressBar);
            progressBar.setVisibility(layer.isLoaded() ? View.GONE : View.VISIBLE);

            return view;
        }

        @Override
        public Layer getItem(int index) {
            return layers.get(index);
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