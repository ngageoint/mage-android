package mil.nga.giat.mage.map.preference;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.MAGE.OnCacheOverlayListener;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.map.CacheOverlay;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

public class FeatureOverlayPreferenceActivity extends ListActivity implements OnCacheOverlayListener {

    private MAGE mage;
    private OverlayAdapter overlayAdapter;
    private MenuItem refreshButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_feature_overlay);

        mage = (MAGE) getApplication();
        
        ListView listView = getListView();

        setListAdapter(overlayAdapter);
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
        mage.unregisterCacheOverlayListener(this);
    }

    @Override
    public void onCacheOverlay(List<CacheOverlay> cacheOverlays) {
        ListView listView = getListView();
        listView.clearChoices();
        
        cacheOverlays.add(new CacheOverlay("Features", new File("")));
        cacheOverlays.add(new CacheOverlay("Roads", new File("")));
        cacheOverlays.add(new CacheOverlay("Rivers", new File("")));
        
        OverlayAdapter overlayAdapter = new OverlayAdapter(this, cacheOverlays);
        setListAdapter(overlayAdapter);
//        
//        // Set what should be checked based on preferences.
//        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
//        Set<String> overlays = preferences.getStringSet("featureOverlays", Collections.<String> emptySet());
//        for (int i = 0; i < listView.getCount(); i++) {
//            CacheOverlay tileOverlay = (CacheOverlay) listView.getItemAtPosition(i);
//            if (overlays.contains(tileOverlay.getName())) {
//                listView.setItemChecked(i, true);
//            }
//        }
        
//        refreshButton.setEnabled(true);
        getListView().setEnabled(true);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        refreshButton = menu.findItem(R.id.overlay_map_refresh);
//        refreshButton.setEnabled(false);
//        
//        // This really should be done in the onResume, but I need to have my refreshButton
//        // before I register as the call back will set it to enabled
//        // the problem is that onResume gets called before this so my menu is 
//        // not yet setup and I will not have a handle on this button
        mage.registerCacheOverlayListener(this);
        
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
        overlays.add("Poop");
        overlays.add("Pee");
//        SparseBooleanArray checked = getListView().getCheckedItemPositions();
//        for (int i = 0; i < checked.size(); i++) {
//            if (checked.valueAt(i)) {
//                CacheOverlay overlay = (CacheOverlay) getListView().getItemAtPosition(checked.keyAt(i));
//                overlays.add(overlay.getName());
//            }
//        }

        return overlays;
    }

    public static class OverlayAdapter extends ArrayAdapter<CacheOverlay> {
        private List<CacheOverlay> overlays;

        public OverlayAdapter(Context context, List<CacheOverlay> overlays) {
            super(context, R.layout.feature_layer_list_item, R.id.checkedTextView, overlays);
            this.overlays = overlays;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            String name = overlays.get(position).getName();
            CheckedTextView checkedView = (CheckedTextView) view.findViewById(R.id.checkedTextView);
            checkedView.setText(name);

            return view;
        }

        @Override
        public CacheOverlay getItem(int index) {
            return overlays.get(index);
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