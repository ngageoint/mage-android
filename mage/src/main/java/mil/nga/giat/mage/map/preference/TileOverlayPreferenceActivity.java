package mil.nga.giat.mage.map.preference;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.MAGE.OnCacheOverlayListener;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.map.CacheOverlay;

public class TileOverlayPreferenceActivity extends ListActivity implements OnCacheOverlayListener {

    private MAGE mage;
    private ProgressBar progressBar;
    private MenuItem refreshButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_cache_overlay);

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
        mage.unregisterCacheOverlayListener(this);
    }

    @Override
    public void onCacheOverlay(List<CacheOverlay> cacheOverlays) {
        ListView listView = getListView();
        listView.clearChoices();
        
        OverlayAdapter overlayAdapter = new OverlayAdapter(this, cacheOverlays);
        setListAdapter(overlayAdapter);
        
        // Set what should be checked based on preferences.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> overlays = preferences.getStringSet(getString(R.string.tileOverlaysKey), Collections.<String> emptySet());

        for (int i = 0; i < listView.getCount(); i++) {
            CacheOverlay tileOverlay = (CacheOverlay) listView.getItemAtPosition(i);
            if (overlays.contains(tileOverlay.getName())) {
                listView.setItemChecked(i, true);
            }
        }
        
        refreshButton.setEnabled(true);
        getListView().setEnabled(true);
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
            getListView().setEnabled(false);

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

    private ArrayList<String> getSelectedOverlays() {
        ArrayList<String> overlays = new ArrayList<String>();
        SparseBooleanArray checked = getListView().getCheckedItemPositions();
        for (int i = 0; i < checked.size(); i++) {
            if (checked.valueAt(i)) {
                CacheOverlay overlay = (CacheOverlay) getListView().getItemAtPosition(checked.keyAt(i));
                overlays.add(overlay.getName());
            }
        }

        return overlays;
    }

    public static class OverlayAdapter extends ArrayAdapter<CacheOverlay> {
        private List<CacheOverlay> overlays;

        public OverlayAdapter(Context context, List<CacheOverlay> overlays) {
            super(context, android.R.layout.simple_list_item_multiple_choice, overlays);
            this.overlays = overlays;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            String name = overlays.get(position).getName();
            CheckedTextView checkedView = (CheckedTextView) view;
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