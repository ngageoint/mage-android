package mil.nga.giat.mage.map.preference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.MAGE.OnStaticLayerListener;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
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
import android.widget.ProgressBar;

public class FeatureOverlayPreferenceActivity extends ListActivity implements OnStaticLayerListener {

    private MAGE mage;
    private ProgressBar progressBar;
    private MenuItem refreshButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_feature_overlay);

        mage = (MAGE) getApplication();
        progressBar = (ProgressBar) findViewById(R.id.overlay_progress_bar);
        
        ListView listView = getListView();

        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        getListView().setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPause() {
        super.onPause();
        mage.unregisterStaticLayerListener(this);
    }

    @Override
    public void onStaticLayer(List<Layer> layers) {
        ListView listView = getListView();
        listView.clearChoices();
        
        OverlayAdapter overlayAdapter = new OverlayAdapter(this, layers);
        setListAdapter(overlayAdapter);
        
        // Set what should be checked based on preferences.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> overlays = preferences.getStringSet("featureOverlays", Collections.<String> emptySet());
        for (int i = 0; i < listView.getCount(); i++) {
            Layer layer = (Layer) listView.getItemAtPosition(i);
            if (overlays.contains(layer.getName())) {
                listView.setItemChecked(i, true);
            }
        }
        
        refreshButton.setEnabled(true);
        progressBar.setVisibility(View.GONE);
        getListView().setEnabled(true);
    }
    

    @Override
    public void onStaticLayerLoaded(Layer layer) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        refreshButton = menu.findItem(R.id.feature_overlay_refresh);
        refreshButton.setEnabled(false);
//        
//        // This really should be done in the onResume, but I need to have my refreshButton
//        // before I register as the call back will set it to enabled
//        // the problem is that onResume gets called before this so my menu is 
//        // not yet setup and I will not have a handle on this button
        mage.registerStaticLayerListener(this);
        
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
        case R.id.overlay_map_refresh:
//            item.setEnabled(false);
//            progressBar.setVisibility(View.VISIBLE);
//            getListView().setEnabled(false);
//
//            ((MAGE) getApplication()).refreshTileOverlays();
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
                overlays.add(layer.getName());
            }
        }

        return overlays;
    }

    public static class OverlayAdapter extends ArrayAdapter<Layer> {
        private List<Layer> layers;

        public OverlayAdapter(Context context, List<Layer> overlays) {
            super(context, R.layout.feature_layer_list_item, R.id.checkedTextView, overlays);
//            super(context, android.R.layout.simple_list_item_multiple_choice, overlays);

            this.layers = overlays;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            String name = layers.get(position).getName();
            CheckedTextView checkedView = (CheckedTextView) view.findViewById(R.id.checkedTextView);
//          CheckedTextView checkedView = (CheckedTextView) view;

            checkedView.setText(name);

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